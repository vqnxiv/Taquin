package io.github.vqnxiv.taquin.solver;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;
import io.github.vqnxiv.taquin.solver.search.Astar;
import io.github.vqnxiv.taquin.util.Utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.EnumMap;


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
                case MAX_TIME -> System.currentTimeMillis() - startTime;
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
    
    public Task<State> getSteps(int n) {
        return new Steps<>(n);
    }
    
    public Task<State> getRun() {
        return new Run<>();
    }
    
    public class Steps<State> extends Task<State> {
        
        private int iterations;
        
        protected Steps(int n) {
            
            iterations = n;
        }

        @Override
        protected State call() {

            if(currentState == Search.State.READY || currentState == Search.State.PAUSED) {

                currentState = Search.State.RUNNING;
                startTime = System.currentTimeMillis();

                for(int i = 0; i < iterations; i++)
                    if(checkConditions()) {
                        step();
                        updateMessage(currentState());
                    }
                    else break;

                elapsedTime += System.currentTimeMillis() - startTime;
            }

            // if false -> we reached the end of the search
            if(checkConditions()) pause();

            try {
                Thread.sleep(200);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            updateMessage(currentState());

            return (State) currentState;
        }
       
    }

    public class Run<State> extends Task<State> {
        
        protected Run() {
        }

        @Override
        protected State call() {

            if(currentState == Search.State.READY || currentState == Search.State.PAUSED) {

                currentState = Search.State.RUNNING;
                startTime = System.currentTimeMillis();

                while(checkConditions()) {

                    // todo: weird case of BFS with LinkedHashSet on the test grid at around ~2013 (constant) h = NONE
                    // todo: where it doesn't remove the new current from queued and loop over ^ state
                    // it also happens with other heuristics, it just blocks at another state
                    // hash conflict?

                    step();
                    updateMessage(currentState());
                }

                elapsedTime += System.currentTimeMillis() - startTime;

                // sleep to make sure the last updateMessage() goes through without being throttled
                // todo: change to a platfrom.runlater()
                try {
                    Thread.sleep(200);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                updateMessage(currentState());
            }

            // we don't need to currentState = PAUSED / .pause() here 
            // as it's either ENDED_* or PAUSED if we got out of the loop

            return (State) currentState;
        }

    }
    
    private String currentState() {
        var sb = new StringBuilder();
        
        // '\t' doesnt work /shrug
        // todo: progressLabel -> text area progress
        sb.append("State: ").append(currentState).append("  ");
        sb.append("Explored: ").append(currentSpace.getExplored().size()).append("  ");
        sb.append("Queued: ").append(currentSpace.getQueued().size()).append("  ");
        sb.append("Current: ").append(currentSpace.getCurrent().getKey()).append("  ");
        sb.append("Depth: ").append(currentSpace.getCurrent().getDepth()).append("  ");
        
        return sb.toString();
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
