package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import java.util.Collections;


public class BestFirst extends Search {


    public static class Builder extends Search.Builder<Builder> {

        private boolean useMerge = false;

        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
        }

        public Builder useMerge(boolean b) {
            useMerge = b;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public BestFirst build() {
            return new BestFirst(this);
        }
    }
    
    
    // ------

    private final boolean useMerge;

    
    // ------

    private BestFirst(Builder builder){
        super(builder);
        
        useMerge = builder.useMerge || (!currentSpace.getQueued().usesNaturalOrdering() && !currentSpace.getQueued().isSortable());
        setReady();
    }


    // ------
    
    public static boolean isHeuristicNeeded() { 
        return true; 
    }

    public static String getShortName() { 
        return "Best first"; 
    }

    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(currentSpace.getGoal(), heuristic));
    }

    @Override
    protected void step() {
        
        Grid newCurrent = currentSpace.getQueued().pollFirst();

        currentSpace.setCurrent(newCurrent);
        currentSpace.getExplored().add(newCurrent);

        var toAdd = currentSpace.getNewNeighbors(filterAlreadyExplored, linkAlreadyExploredNeighbors);

        for(Grid g : toAdd) computeHeuristic(g);

        if(!toAdd.isEmpty()) {
            if(currentSpace.getQueued().usesNaturalOrdering())
                currentSpace.getQueued().addAll(toAdd);
            else if(useMerge)
                currentSpace.getQueued().mergeWith(toAdd);
            else  {
                Collections.sort(toAdd);
                currentSpace.getQueued().addAll(toAdd);
                currentSpace.getQueued().sort();
            }
        }
    }
    
}
