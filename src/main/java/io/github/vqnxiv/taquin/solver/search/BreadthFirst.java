package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import java.util.Collections;


public class BreadthFirst extends Search {


    public static class Builder extends Search.Builder<Builder> {

        private boolean checkNewStatesForGoal = false;

        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
        }

        public Builder checkNewStatesForGoal(boolean b) {
            checkNewStatesForGoal = b;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public BreadthFirst build() {
            return new BreadthFirst(this);
        }
    }
    
    
    // ------

    private final boolean checkNewStatesForGoal;


    // ------
    
    private BreadthFirst(Builder builder){
        super(builder);
        
        checkNewStatesForGoal = builder.checkNewStatesForGoal;
        setReady();
    }


    // ------

    public static boolean isHeuristicNeeded() { 
        return false; 
    }

    public static String getShortName() {
        return "BFS"; 
    }

    @Override
    protected void computeHeuristic(Grid g){
        g.setHeuristicValue(g.distanceTo(currentSpace.getGoal(), heuristic));
    }

    @Override
    protected void step(){
        
        Grid newCurrent = currentSpace.getQueued().pollFirst();
        
        currentSpace.setCurrent(newCurrent);
        currentSpace.getExplored().add(newCurrent);

        var toAdd = currentSpace.getNewNeighbors(filterAlreadyExplored, linkAlreadyExploredNeighbors);

        if(heuristic != Grid.Distance.NONE){
            for(Grid g : toAdd) computeHeuristic(g);
            Collections.sort(toAdd);
        }

        if(checkNewStatesForGoal) 
            for (Grid g : toAdd) 
                if(currentSpace.isGoal(g)) 
                    currentSpace.setCurrent(g);

        currentSpace.getQueued().addAll(toAdd);
    }
    
}