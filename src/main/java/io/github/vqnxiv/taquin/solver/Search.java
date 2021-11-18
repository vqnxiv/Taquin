package io.github.vqnxiv.taquin.solver;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.openjdk.jol.info.GraphLayout;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;


public abstract class Search {

    
    public abstract static class Builder<B extends Builder<B>> {
        
        private SearchSpace space;
        private final EnumMap<Limit, Long> limits;

        public ObjectProperty<Grid.Distance> heuristic;
        public BooleanProperty filterExplored;
        public BooleanProperty filterQueued;
        public BooleanProperty linkExplored;
        public BooleanProperty checkForQueuedEnd;
        public IntegerProperty throttle;
        public StringProperty name;
        
        // used when converting from a search type to another
        public Builder(Builder<?> toCopy) {
            
            if(toCopy == null) {
                limits = new EnumMap<>(Limit.class);
                for (var l : Limit.values()) limits.put(l, 0L);
                
                heuristic = new SimpleObjectProperty<>(Grid.Distance.NONE);
                filterExplored = new SimpleBooleanProperty(true);
                filterQueued = new SimpleBooleanProperty(true);
                linkExplored = new SimpleBooleanProperty(false);
                checkForQueuedEnd = new SimpleBooleanProperty(false);
                throttle = new SimpleIntegerProperty(0);
                name = new SimpleStringProperty("");
            }
            else {
                limits = toCopy.limits;
                heuristic = toCopy.heuristic;
                filterExplored = toCopy.filterExplored;
                filterQueued = toCopy.filterQueued;
                linkExplored = toCopy.linkExplored;
                checkForQueuedEnd = toCopy.checkForQueuedEnd;
                throttle = toCopy.throttle;
                name = toCopy.name;
            }
        }
        
        public B searchSpace(SearchSpace s) {
            space = s;
            return self();
        }
        
        public B limit(Limit l, long n) {
            limits.put(l, n);
            return self();
        }
        
        public abstract Property<?>[] properties();
        
        public abstract boolean isHeuristicRequired();
        
        protected abstract B self();
        
        public abstract Search build();
    }
    
    
    // ------

    public enum Limit {
        MAX_TIME        { public String toString() { return "maximum time allowed"; } },
        MAX_MEMORY      { public String toString() { return "maximum memory allowed"; } },
        MAX_DEPTH       { public String toString() { return "maximum depth allowed"; } },
        MAX_EXPLORED    { public String toString() { return "maximum explored states allowed"; } },
        MAX_GENERATED   { public String toString() { return "maximum generated states allowed"; } }
    }

    public enum State {
        NOT_READY                   { public String toString() { return "not ready"; } },
        READY                       { public String toString() { return "ready"; } },
        RUNNING                     { public String toString() { return "running"; } },
        PAUSED                      { public String toString() { return "paused"; } },
        ENDED_SUCCESS               { public String toString() { return "successfully ended"; } },
        ENDED_FAILURE_USER_FORCED   { public String toString() { return "forcefully ended"; } },
        ENDED_FAILURE_EMPTY_SPACE   { public String toString() { return "empty space search"; } },
        ENDED_FAILURE_LIMIT         { public String toString() { return "reached limit"; } },
    }


    // ------
    
    private final long id;
    private final String name;
    
    private State currentState = State.NOT_READY;
    
    protected final SearchSpace searchSpace;
    protected final Grid.Distance heuristic;
    private final EnumMap<Search.Limit, Long> limitsMap;
    
    protected final boolean filterExplored;
    protected final boolean filterQueued;
    protected final boolean linkAlreadyExploredNeighbors;
    
    boolean userForceStop = false;
    
    private long startTime;
    private long elapsedTime = 0;
    
    private final int throttle;
    private final boolean checkIfEndWasQueued;
    private boolean monitorMemory = false;
    

    // ------
    
    private final StringProperty currentKey     = new SimpleStringProperty(this, "currentKey", "");
    private final StringProperty currentDepth   = new SimpleStringProperty(this, "currentDepth", "");
    private final StringProperty searchState    = new SimpleStringProperty(this, "searchState", "");
    private final StringProperty time           = new SimpleStringProperty(this, "time", "");
    private final StringProperty explored       = new SimpleStringProperty(this, "explored", "");
    private final StringProperty exploredMem    = new SimpleStringProperty(this, "exploredMem", "0b");
    private final StringProperty queued         = new SimpleStringProperty(this, "queued", "");
    private final StringProperty queuedMem      = new SimpleStringProperty(this, "queuedMem", "0b");

    private final ReadOnlyStringProperty[] properties = {
        searchState, time, currentKey, currentDepth, explored, exploredMem, queued, queuedMem
    };
    
    public final ReadOnlyStringProperty currentStateProperty() {
        return searchState;
    }
    
    
    // ------
    
    protected Search(Builder<?> builder) {
        searchSpace = builder.space;
        heuristic = builder.heuristic.get();
        
        builder.limits.entrySet().removeIf(e -> e.getValue() == 0);
        limitsMap = builder.limits;
        
        filterExplored = builder.filterExplored.get();
        filterQueued = builder.filterQueued.get();
        linkAlreadyExploredNeighbors = builder.linkExplored.get();

        checkIfEndWasQueued = builder.checkForQueuedEnd.get();
        throttle = builder.throttle.get();

        id = System.currentTimeMillis();
        var s = builder.name.get();
        name = (s.equals("")) ? Long.toString(id) : s;
        
        // needed?
        if(heuristic != Grid.Distance.NONE)
            searchSpace.getStart().setHeuristicValue(searchSpace.getStart().distanceTo(searchSpace.getGoal(), heuristic));
    }

    
    // ------

    protected State getState() { 
        return currentState; 
    }

    protected void setReady() { 
        currentState = State.READY; 
    }
    
    public String getName() {
        return name;
    }
    
    public ReadOnlyStringProperty[] getProperties() {
        return properties;
    }
    
    
    // ------
    
    private boolean checkConditions(){

        if(currentState == State.PAUSED) {
            return false;
        }

        if(searchSpace.isCurrentGoal()){
            currentState = State.ENDED_SUCCESS;
            return false;
        }

        if(searchSpace.getQueued().isEmpty()){
            currentState = State.ENDED_FAILURE_EMPTY_SPACE;
            return false;
        }

        if(userForceStop){
            currentState = State.ENDED_FAILURE_USER_FORCED;
            return false;
        }

        for(var l : limitsMap.entrySet()){

            long currentVal = switch (l.getKey()) {
                case MAX_TIME -> (System.currentTimeMillis() - startTime) + elapsedTime;
                case MAX_MEMORY -> GraphLayout.parseInstance(searchSpace.getExplored()).totalSize() 
                    + GraphLayout.parseInstance(searchSpace.getQueued()).totalSize();
                case MAX_DEPTH -> searchSpace.getCurrent().getDepth();
                case MAX_EXPLORED -> searchSpace.getExplored().size();
                case MAX_GENERATED -> searchSpace.getExplored().size() + searchSpace.getQueued().size();
                default -> -1;
            };

            if(currentVal >= l.getValue()){
                currentState = State.ENDED_FAILURE_LIMIT;
                return false;
            }
        }

        return true;
    }


    // ------

    protected void pause() {
        if(currentState != State.RUNNING) {
            return;
        }

        this.currentState = State.PAUSED;
    }

    protected void stop() {
        if(currentState != State.RUNNING && currentState != State.PAUSED)
            return;
        
        userForceStop = true;
        // find a way to update here
        // if(currentState == State.PAUSED) pause();
    }
    
    
    public SearchTask<State> newSearchTask(int n) {
        return new SearchTask<>(n);
    }
    

    public class SearchTask<S> extends Task<S> {

        private final AtomicReference<String> currentKeyUpdate    = new AtomicReference<>();
        private final AtomicReference<String> currentDepthUpdate  = new AtomicReference<>();
        private final AtomicReference<String> searchStateUpdate   = new AtomicReference<>();
        private final AtomicReference<String> timeUpdate          = new AtomicReference<>();
        private final AtomicReference<String> exploredUpdate      = new AtomicReference<>();
        private final AtomicReference<String> exploredMemUpdate   = new AtomicReference<>();
        private final AtomicReference<String> queuedUpdate        = new AtomicReference<>();
        private final AtomicReference<String> queuedMemUpdate     = new AtomicReference<>();
        
        
        private void updateAll() {
            if(currentKeyUpdate.getAndSet(
                Integer.toString(searchSpace.getCurrent().getKey())
            ) == null) {
                Platform.runLater(() -> {
                    final String s = currentKeyUpdate.getAndSet(null);
                    currentKey.set(s);
                });
            }
            if(currentDepthUpdate.getAndSet(
                Integer.toString(searchSpace.getCurrent().getDepth())
            ) == null) {
                Platform.runLater(() -> {
                    final String s = currentDepthUpdate.getAndSet(null);
                    currentDepth.set(s);
                });
            }
            if(searchStateUpdate.getAndSet(
                currentState.toString()
            ) == null) {
                Platform.runLater(() -> {
                    final String s = searchStateUpdate.getAndSet(null);
                    searchState.set(s);
                });
            }
            // throwup but it avoids showing double time when it ends
            if(timeUpdate.getAndSet(
                Long.toString(
                    (currentState == Search.State.RUNNING)
                    ? (System.currentTimeMillis() - startTime) + elapsedTime
                    : (elapsedTime)
                )
            ) == null) {
                Platform.runLater(() -> {
                    final String s = timeUpdate.getAndSet(null) + "ms";
                    time.set(s);
                });
            }
            if(exploredUpdate.getAndSet(
                Integer.toString(searchSpace.getExplored().size())
            ) == null) {
                Platform.runLater(() -> {
                    final String s = exploredUpdate.getAndSet(null);
                    explored.set(s);
                });
            }
            if(queuedUpdate.getAndSet(
                Integer.toString(searchSpace.getQueued().size())
            ) == null) {
                Platform.runLater(() -> {
                    final String s = queuedUpdate.getAndSet(null);
                    queued.set(s);
                });
            }
            if(monitorMemory) {
                if(exploredMemUpdate.getAndSet(
                    Long.toString(GraphLayout.parseInstance(searchSpace.getExplored()).totalSize() / 1048576L)
                ) == null) {
                    Platform.runLater(() -> {
                        final String s = exploredMemUpdate.getAndSet(null) + "mb";
                        exploredMem.set(s);
                    });
                }
                if(queuedMemUpdate.getAndSet(
                    Long.toString(GraphLayout.parseInstance(searchSpace.getQueued()).totalSize() / 1048576L)
                ) == null) {
                    Platform.runLater(() -> {
                        final String s = queuedMemUpdate.getAndSet(null) + "mb";
                        queuedMem.set(s);
                    });
                }
            }
        }
        

        private final int iterations;
        
        SearchTask(int n) {
            iterations = n;
        }
        
        @Override
        protected S call() throws Exception {

            if(currentState == Search.State.READY || currentState == Search.State.PAUSED) {

                currentState = Search.State.RUNNING;
                startTime = System.currentTimeMillis();
                
                for(int i = 0; i < ((iterations > 0) ? iterations : iterations + 1); i += (iterations > 0) ? 1 : 0) {
                    if(checkConditions()) {
                        step();
                        updateAll();
                        
                        if(throttle > 0) {
                            elapsedTime += System.currentTimeMillis() - startTime;
                            Thread.sleep(throttle);
                            startTime = System.currentTimeMillis();
                        }
                    }
                    else break;
                }
                
                elapsedTime += System.currentTimeMillis() - startTime;
                if(checkConditions()) pause();
                monitorMemory = true;
                updateAll();
                
                if(checkIfEndWasQueued && currentState == Search.State.ENDED_FAILURE_LIMIT) {
                    int n;
                    if((n = searchSpace.getQueued().indexOf(searchSpace.getGoal())) > -1) {
                        System.out.println("end queued at " + n);
                    }
                    else {
                        System.out.println("end not queued");
                    }
                }
            }

            return (S) currentState;
        }
    }
    

    // ------

    public static String getShortName() { 
        return "Abstract search"; 
    }
    
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
        return "[" + id + "] " + name + ": " + currentState;
    }
}
