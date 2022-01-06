package io.github.vqnxiv.taquin.solver;


import io.github.vqnxiv.taquin.Taquin;
import io.github.vqnxiv.taquin.controller.BuilderController;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;

import io.github.vqnxiv.taquin.util.IBuilder;
import io.github.vqnxiv.taquin.util.Utils;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.openjdk.jol.info.GraphLayout;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;


public abstract class Search {

    
    public abstract static class Builder<B extends Builder<B>> implements IBuilder {
        
        // todo integerProperties for limits? then put back into a map when creating
        private final EnumMap<SearchLimit, Long> limits;

        protected final ObjectProperty<Grid.Distance> heuristic;
        protected final ObjectProperty<Grid.EqualPolicy> equalPolicy;
        protected final BooleanProperty filterExplored;
        protected final BooleanProperty filterQueued;
        protected final BooleanProperty linkExplored;
        protected final BooleanProperty checkForQueuedEnd;
        protected final StringProperty name;
        protected final BooleanProperty monitorMemory;
        
        // used when converting from a search type to another
        protected Builder(Builder<?> toCopy) {
            
            if(toCopy == null) {
                limits = new EnumMap<>(SearchLimit.class);
                for (var l : SearchLimit.values()) limits.put(l, 0L);

                equalPolicy = new SimpleObjectProperty<>(this, "equal policy", Grid.EqualPolicy.NONE);
                heuristic = new SimpleObjectProperty<>(this, "heuristic", Grid.Distance.NONE);
                filterExplored = new SimpleBooleanProperty(this, "filter explored", true);
                filterQueued = new SimpleBooleanProperty(this, "filter queued", true);
                linkExplored = new SimpleBooleanProperty(this, "link already explored", false);
                checkForQueuedEnd = new SimpleBooleanProperty(this, "check if goal queued", false);
                name = new SimpleStringProperty(this, "name", "");
                monitorMemory = new SimpleBooleanProperty(this, "monitor memory", false);
            }
            else {
                limits = toCopy.limits;
                heuristic = toCopy.heuristic;
                equalPolicy = toCopy.equalPolicy;
                filterExplored = toCopy.filterExplored;
                filterQueued = toCopy.filterQueued;
                linkExplored = toCopy.linkExplored;
                checkForQueuedEnd = toCopy.checkForQueuedEnd;
                name = toCopy.name;
                monitorMemory = toCopy.monitorMemory;
            }
        }
        
        
        public B limit(SearchLimit l, long n) {
            limits.put(l, n);
            return self();
        }

        public Grid.Distance getHeuristic() {
            return heuristic.get();
        }

        @Override
        public Map<String, Property<?>> getNamedProperties() {
            return Map.of(
                name.getName(), name,
                heuristic.getName(), heuristic
            );
        }

        @Override
        public EnumMap<BuilderController.TabPaneItem, List<Property<?>>> getBatchProperties() {
            return new EnumMap<>(Map.of(
                BuilderController.TabPaneItem.SEARCH_MAIN, List.of(filterExplored, filterQueued, linkExplored, equalPolicy),
                BuilderController.TabPaneItem.LIMITS, List.of(checkForQueuedEnd),
                BuilderController.TabPaneItem.MISCELLANEOUS, List.of(monitorMemory)
            ));
        }
        
        public abstract boolean isHeuristicRequired();
        
        protected abstract B self();
        
        protected abstract Search build();
    }
    
    public class SearchTask<S> extends Task<S> {
        
        private final EnumMap<SearchProperty, AtomicReference<String>> atomicReferences;

        {
            atomicReferences = new EnumMap<SearchProperty, AtomicReference<String>>(SearchProperty.class);
            
            for(var k : properties.keySet()) {
                atomicReferences.put(k, new AtomicReference<>());
            }
        }
        
        private void updateAll() {
            for(var e : atomicReferences.entrySet()) {
                var k = e.getKey(); 
                var v = e.getValue();
                if(v.getAndSet(k.calcToString(Search.this)) == null) {
                    Platform.runLater(() -> {
                        final String s = v.getAndSet(null);
                        properties.get(k).set(s);
                    });
                }
            }
        }
        
        private void throttle() {
            if(throttle <= 0) {
                return;
            }
            
            elapsedTime += System.nanoTime() - startTime;
            try {
                Thread.sleep(throttle);
            } catch(InterruptedException e) {
                // e.printStackTrace();
                Search.this.stop();
            }
            startTime = System.nanoTime();
        }
        
        
        private final int iterations;
        private final int throttle;

        
        private SearchTask(int iter, int thr) {
            iterations = iter;
            throttle = thr;
        }

        @Override
        protected S call() throws Exception {
            
            if(currentSearchState == SearchState.READY || currentSearchState == SearchState.PAUSED) {

                currentSearchState = SearchState.RUNNING;
                startTime = System.nanoTime();
                log("Starting search");
                
                for(int i = 0; i < ((iterations > 0) ? iterations : iterations + 1); i += (iterations > 0) ? 1 : 0) {
                    if(checkConditions()) {
                        step();
                        updateAll();
                        throttle();
                    }
                    else break;
                }

                elapsedTime += System.nanoTime() - startTime;
                if(checkConditions()) {
                    pause();
                    log("Search paused");
                }
                else {
                    log(Search.this.getState().toString());
                }
                //monitorMemory = true;
                updateAll();

                
                if(checkIfEndWasQueued && currentSearchState == SearchState.ENDED_FAILURE_LIMIT) {
                    int n;
                    if((n = searchSpace.getQueued().indexOf(searchSpace.getGoal())) > -1) {
                        log("End queued at index " + n);
                    }
                    else {
                        log("End not queued");
                    }
                }
                
            }

            return (S) currentSearchState;
        }
    }

    public enum SearchLimit {
        
        /*
        (s -> SProperty.ELAPSED_TIME.calc(s) >= s.limitsMap.get(Limit.MAXIMUM_TIME)),
        (s -> SProperty.ELAPSED_TIME.calc(s) >= s.limitsMap.get(this)),
        => illegal self reference
        so we have to use an extra method for the check
        */
        
        MAXIMUM_TIME
            (SearchProperty.ELAPSED_TIME::calc),
        MAXIMUM_MEMORY
            (s -> (Long) SearchProperty.EXPLORED_MEMORY.calc(s) + (Long) SearchProperty.QUEUED_MEMORY.calc(s)),
        MAXIMUM_DEPTH
            (SearchProperty.CURRENT_DEPTH::calc),
        MAXIMUM_EXPLORED_STATES
            (SearchProperty.EXPLORED_SIZE::calc),
        MAXIMUM_GENERATED_STATES
            (s -> (Long) SearchProperty.EXPLORED_SIZE.calc(s) + (Long) SearchProperty.QUEUED_SIZE.calc(s));
        
        private final Function<Search, Long> function;
        
        SearchLimit(Function<Search, Long> func) {
            function = func;
        }

        private boolean check(Search s) {
            return function.apply(s) >= s.limitsMap.get(this);
        }

        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }
    
    public enum SearchState {
        NOT_READY,
        READY,
        RUNNING,
        PAUSED,
        ENDED_SUCCESS { 
            @Override 
            public String toString() { 
                return "successfully ended"; 
            } 
        },
        ENDED_FAILURE_USER_FORCED { 
            @Override 
            public String toString() { 
                return "forcefully ended"; 
            } 
        },
        ENDED_FAILURE_EMPTY_SPACE { 
            @Override 
            public String toString() { 
                return "empty space search";
            } 
        },
        ENDED_FAILURE_LIMIT { 
            @Override 
            public String toString() { 
                return "reached limit"; 
            } 
        };
        
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }
    
    public enum SearchProperty {
        CURRENT_STATE
            (x -> x.currentSearchState),
        ELAPSED_TIME
            (x -> x.getElapsedTime() / 1_000_000),
        CURRENT_KEY
            (x -> x.searchSpace.getCurrent().getKey()),
        CURRENT_DEPTH
            (x -> x.searchSpace.getCurrent().getDepth()),
        EXPLORED_SIZE
            (x -> x.searchSpace.getExplored().size()),
        EXPLORED_MEMORY
            (x -> GraphLayout.parseInstance(x.searchSpace.getExplored()).totalSize() / 1048576L),
        QUEUED_SIZE
            (x -> x.searchSpace.getQueued().size()),
        QUEUED_MEMORY
            (x -> GraphLayout.parseInstance(x.searchSpace.getQueued()).totalSize() / 1048576L);
        
        private final Function<Search, ?> function;
        
        SearchProperty(Function<Search, ?> func) {
            function = func;
        }
        
        public <T> T calc(Search s) {
            return (T) function.apply(s);
        }
        
        public String calcToString(Search s) {
            return function.apply(s).toString();
        }

        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }
    
    public static class SearchLogMarker implements Marker {

        @Override
        public Marker addParents(Marker... markers) {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Marker[] getParents() {
            return new Marker[0];
        }

        @Override
        public boolean hasParents() {
            return false;
        }

        @Override
        public boolean isInstanceOf(Marker m) {
            return false;
        }

        @Override
        public boolean isInstanceOf(String name) {
            return false;
        }

        @Override
        public boolean remove(Marker marker) {
            return false;
        }

        @Override
        public Marker setParents(Marker... markers) {
            return null;
        }
    }

    
    // ------

    /**
     * Search logger.
     */
    private static final Logger LOGGER = LogManager.getLogger("searchLogger");
    
    private static int SEARCH_ID_COUNT;
    
    private final int id;
    private final String name;
    
    private SearchState currentSearchState = SearchState.NOT_READY;
    
    protected SearchSpace searchSpace;
    protected final Grid.Distance heuristic;
    protected final Grid.EqualPolicy equalPolicy;
    private final EnumMap<SearchLimit, Long> limitsMap;
    
    protected final Comparator<Grid> heuristicComparator = new Comparator<Grid>() {
        @Override
        public int compare(Grid g1, Grid g2) {
            if(g1.getHeuristicValue() == g2.getHeuristicValue()) {
                if(g1.equals(g2)) {
                    return  0;
                }

                return equalPolicy.calc(g1, g2);
            }

            return Float.compare(g1.getHeuristicValue(), g2.getHeuristicValue());
        }
    };
    
    protected final Comparator<Grid> reverseHeuristicComparator = 
        (g1, g2) -> - heuristicComparator.compare(g1, g2);
    
    protected final boolean filterExplored;
    protected final boolean filterQueued;
    protected final boolean linkAlreadyExploredNeighbors;
    
    // todo: int time + move start/elapsed calc to task?
    // todo: use instant
    private long startTime;
    private long elapsedTime = 0;
    

    private boolean log = true;
   
    private final boolean checkIfEndWasQueued;
    
    private final EnumMap<SearchProperty, StringProperty> properties;

    
    // ------
    
    protected Search(Builder<?> builder) {
        heuristic = builder.heuristic.get();
        equalPolicy = builder.equalPolicy.get();
        
        builder.limits.entrySet().removeIf(e -> e.getValue() == 0);
        limitsMap = builder.limits;
        
        filterExplored = builder.filterExplored.get();
        filterQueued = builder.filterQueued.get();
        linkAlreadyExploredNeighbors = builder.linkExplored.get();

        checkIfEndWasQueued = builder.checkForQueuedEnd.get();
        
        id = SEARCH_ID_COUNT;
        SEARCH_ID_COUNT++;
        name = (builder.name.get().equals("")) ? Integer.toString(id) : builder.name.get();
        
        properties = createProperties(builder.monitorMemory.get());
    }

    private EnumMap<SearchProperty, StringProperty> createProperties(boolean monitorMemory) {

        var s = EnumSet.allOf(SearchProperty.class);
        
        if(!monitorMemory) {
            s.remove(SearchProperty.EXPLORED_MEMORY);
            s.remove(SearchProperty.QUEUED_MEMORY);
        }
        
        var propsMap = new EnumMap<SearchProperty, StringProperty>(SearchProperty.class);

        for(var s2 : s) {
            propsMap.put(
                s2,
                new SimpleStringProperty(this, s2.toString(), "")
            );
        }

        return propsMap;
    };
    
    
    // ------
    
    public void setSearchSpace(SearchSpace space) {
        searchSpace = space;
        setProperties();
    }

    public SearchSpace getSearchSpace() {
        return searchSpace;
    }
    
    private long getElapsedTime() {
        return (currentSearchState == SearchState.RUNNING)
            ? (System.nanoTime() - startTime) + elapsedTime
            : elapsedTime;
    }
    
    protected SearchState getState() { 
        return currentSearchState; 
    }

    protected void setReady() { 
        currentSearchState = SearchState.READY; 
    }
    
    protected void log(String message) {
        if(!log) {
            return;
        }
        
        LOGGER.info(
            new MarkerManager.Log4jMarker(Integer.toString(id)), 
            Long.toString(getElapsedTime()) + '\t' +'\t' + message
        );
    }
    
    public String getName() {
        return name;
    }
    
    public long getID() {
        return id;
    }
    
    public Comparator<Grid> getHeuristicComparator() {
        return heuristicComparator;
    }
    
    public EnumMap<SearchProperty, StringProperty> getProperties() {
        return properties;
    }

    public final ReadOnlyStringProperty getCurrentStateProperty() {
        return properties.get(SearchProperty.CURRENT_STATE);
    }
    
    // ------
    
    private boolean checkConditions(){
        log("Checking conditions");

        if(currentSearchState == SearchState.PAUSED || currentSearchState == SearchState.ENDED_FAILURE_USER_FORCED) {
            return false;
        }

        if(searchSpace.isCurrentGoal()){
            currentSearchState = SearchState.ENDED_SUCCESS;
            return false;
        }

        if(searchSpace.getQueued().isEmpty()){
            currentSearchState = SearchState.ENDED_FAILURE_EMPTY_SPACE;
            return false;
        }
        
        for(var l : limitsMap.keySet()) {
            if(l.check(this)){
                currentSearchState = SearchState.ENDED_FAILURE_LIMIT;
                return false;
            }
        }
    
        return true;
    }


    // ------

    protected void pause() {
        if(currentSearchState != SearchState.RUNNING) {
            return;
        }

        this.currentSearchState = SearchState.PAUSED;
    }

    protected void stop() {
        if(currentSearchState != SearchState.RUNNING && currentSearchState != SearchState.PAUSED) {
            return;
        }
        
        this.currentSearchState = SearchState.ENDED_FAILURE_USER_FORCED;
    }
    
    
    public Optional<SearchTask<SearchState>> newSearchTask(int n, int o) {
        if(searchSpace == null) {
            return Optional.empty();
        }
        return Optional.of(new SearchTask<>(n, o));
    }
    

    // ------
    
    protected abstract void setProperties();
    
    protected abstract void computeHeuristic(Grid g);

    protected abstract void step();
    
    
    // ------
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof Search s) {
            return id == s.id;
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "[" + id + "] " + name + ": " + currentSearchState;
    }
}
