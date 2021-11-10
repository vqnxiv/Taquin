package io.github.vqnxiv.taquin.solver;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;
import io.github.vqnxiv.taquin.solver.search.Astar;
import io.github.vqnxiv.taquin.util.Utils;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;


public abstract class Search {

    
    public abstract static class Builder<B extends Builder<B>> {
        
        private SearchSpace space;
        private Grid.Distance heuristic = Grid.Distance.NONE;
        private EnumMap<Limit, Long> limits;
        private boolean filterExplored = true;
        private boolean linkExplored = false;
        private boolean checkForQueuedEnd = false;
        private int throttle = 0;
        private String name = "";
        
        
        // used when converting from a search type to another
        public Builder(Builder toCopy) {
            if(toCopy != null) {
                space = toCopy.space;
                heuristic = toCopy.heuristic;
                limits = toCopy.limits;
                filterExplored = toCopy.filterExplored;
                linkExplored = toCopy.linkExplored;
                name = toCopy.name;
            }
            else {
                limits = new EnumMap<>(Limit.class);
                for (var l : Limit.values())
                    limits.put(l, 0L);
            }
        }
        
        
        public B searchSpace(SearchSpace s) {
            space = s;
            return self();
        }

        public B heuristic(Grid.Distance d) {
            heuristic = d;
            return self();
        }
        
        public B limit(Limit l, long n) {
            limits.put(l, n);
            return self();
        }
        
        public B filterExplored(boolean b) {
            filterExplored = b;
            return self();
        }
        
        public B linkExploredNeighbors(boolean b) {
            linkExplored = b;
            return self();
        }
        
        public B checkForQueuedEnd(boolean b) {
            checkForQueuedEnd = b;
            return self();
        }
        
        public B throttle(int n) {
            throttle = n;
            return self();
        }
        
        public B name(String s) {
            name = s;
            return self();
        }
        
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
    
    private State currentState;
    
    protected final SearchSpace currentSpace;
    protected final Grid.Distance heuristic;
    private final EnumMap<Search.Limit, Long> limitsMap;
    
    protected final boolean filterAlreadyExplored;
    protected final boolean linkAlreadyExploredNeighbors;
    
    boolean userForceStop = false;
    
    private long startTime;
    private long elapsedTime = 0;
    
    private int throttle;
    private boolean checkIfEndWasQueued;
    
    
    // ------
    
    protected Search(Builder<?> builder) {
        currentSpace = builder.space;
        heuristic = builder.heuristic;
        
        builder.limits.entrySet().removeIf(e -> e.getValue() == 0);
        limitsMap = builder.limits;
        
        filterAlreadyExplored = builder.filterExplored;
        linkAlreadyExploredNeighbors = builder.linkExplored;
        
        checkIfEndWasQueued = builder.checkForQueuedEnd;
        throttle = builder.throttle;

        if(heuristic != Grid.Distance.NONE)
            currentSpace.getStart().setHeuristicValue(currentSpace.getStart().distanceTo(currentSpace.getGoal(), heuristic));
        
        id = System.currentTimeMillis();
        name = (builder.name.equals("")) ? Long.toString(id) : builder.name; 
    }

    
    // ------

    protected State getState() { 
        return currentState; 
    }

    protected void setReady() { 
        currentState = State.READY; 
    }
    

    // ------
    
    private boolean checkConditions(){

        if(currentState == State.PAUSED) {
            return false;
        }

        if(currentSpace.isCurrentGoal()){
            currentState = State.ENDED_SUCCESS;
            return false;
        }

        if(currentSpace.getQueued().isEmpty()){
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
                //case MAX_MEMORY -> (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L - startMemory);
                case MAX_DEPTH -> currentSpace.getCurrent().getDepth();
                case MAX_EXPLORED -> currentSpace.getExplored().size();
                case MAX_GENERATED -> currentSpace.getExplored().size() + currentSpace.getQueued().size();
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
        if(currentState != State.RUNNING && this.currentState != State.PAUSED)
            return;
        
        userForceStop = true;
        // find a way to update here
        // if(currentState == State.PAUSED) pause();
    }
    
    
    public SearchTask<State> newSearchTask(int n) {
        return new SearchTask<State>(n);
    }
    
    // Properties:
    // * currentKey
    // * currentGrid
    // * currentDepth
    // * explored.size()
    // * queued.size()
    // * elapsedTime
    // * state

    public class SearchTask<S> extends Task<S> {

        // all as <String> since they're only used for the GUI
        // so we can directly bind them to the Labels
        // also searchState because stateProperty() is final in Task
        private final AtomicReference<String> currentKeyUpdate    = new AtomicReference<>();
        private final AtomicReference<String> currentDepthUpdate  = new AtomicReference<>();
        private final AtomicReference<String> searchStateUpdate   = new AtomicReference<>();
        private final AtomicReference<String> timeUpdate          = new AtomicReference<>();
        private final AtomicReference<String> exploredUpdate      = new AtomicReference<>();
        private final AtomicReference<String> queuedUpdate        = new AtomicReference<>();
        
        private final StringProperty currentKey     = new SimpleStringProperty(this, "currentKey", "");
        private final StringProperty currentDepth   = new SimpleStringProperty(this, "currentDepth", "");
        private final StringProperty searchState    = new SimpleStringProperty(this, "searchState", "");
        private final StringProperty time           = new SimpleStringProperty(this, "time", "");
        private final StringProperty explored       = new SimpleStringProperty(this, "explored", "");
        private final StringProperty queued         = new SimpleStringProperty(this, "queued", "");
        
        public final ReadOnlyStringProperty currentKeyProperty()    { return currentKey; }
        public final ReadOnlyStringProperty currentDepthProperty()  { return currentDepth; }
        public final ReadOnlyStringProperty searchStateProperty()   { return searchState; }
        public final ReadOnlyStringProperty timeProperty()          { return time; }
        public final ReadOnlyStringProperty exploredProperty()      { return explored; }
        public final ReadOnlyStringProperty queuedProperty()        { return queued; }
        
        
        private void updateAll(String... strings) {
            if(currentKeyUpdate.getAndSet(strings[0]) == null) {
                Platform.runLater(() -> {
                    final String s = currentKeyUpdate.getAndSet(null);
                    currentKey.set(s);
                });
            }
            if(currentDepthUpdate.getAndSet(strings[1]) == null) {
                Platform.runLater(() -> {
                    final String s = currentDepthUpdate.getAndSet(null);
                    currentDepth.set(s);
                });
            }
            if(searchStateUpdate.getAndSet(strings[2]) == null) {
                Platform.runLater(() -> {
                    final String s = searchStateUpdate.getAndSet(null);
                    searchState.set(s);
                });
            }
            if(timeUpdate.getAndSet(strings[3]) == null) {
                Platform.runLater(() -> {
                    final String s = timeUpdate.getAndSet(null);
                    SearchTask.this.time.set(s);
                });
            }
            if(exploredUpdate.getAndSet(strings[4]) == null) {
                Platform.runLater(() -> {
                    final String s = exploredUpdate.getAndSet(null);
                    explored.set(s);
                });
            }
            if(queuedUpdate.getAndSet(strings[5]) == null) {
                Platform.runLater(() -> {
                    final String s = queuedUpdate.getAndSet(null);
                    queued.set(s);
                });
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
                
                if(iterations > 0) {
                    for(int i = 0; i < iterations; i++) {
                        if(checkConditions()) {
                            step();
                            updateAll(getAllStrings());
                        } 
                        else break;
                    }
                }
                else {
                    while(checkConditions()) {
                        step();
                        updateAll(getAllStrings());
                    }
                }

                elapsedTime += System.currentTimeMillis() - startTime;

                if(checkConditions()) pause();

                try {
                    Thread.sleep(200);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                updateAll(getAllStrings());
            }
            
            
            return (S) currentState;
        }
    }
    
    private String[] getAllStrings() {
        return new String[]{
            Integer.toString(currentSpace.getCurrent().getKey()),
            Integer.toString(currentSpace.getCurrent().getDepth()),
            currentState.toString(),
            Long.toString((System.currentTimeMillis() - startTime) + elapsedTime),
            Integer.toString(currentSpace.getExplored().size()),
            Integer.toString(currentSpace.getQueued().size())    
        };
    }


    // ------

    public static boolean isHeuristicNeeded() { 
        return false; 
    }

    public static String getShortName() { 
        return "search"; 
    }
    
    protected abstract void computeHeuristic(Grid g);

    protected abstract void step();

    
    // ------
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Search search = (Search) o;

        if (id != search.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "[" + id + "] " + name; 
        // return Utils.getStringMethodReturn(this.getClass(), "getShortName")
        //         + " search type with " + this.heuristic + " heuristic function: " + this.currentState;
    }
}
