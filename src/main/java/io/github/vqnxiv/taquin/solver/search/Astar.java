package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Collections;


public class Astar extends Search {


    public static class Builder extends Search.Builder<Builder> {
        
        public BooleanProperty useMerge;
        
        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
            
            useMerge = new SimpleBooleanProperty(this, "Use merge", false);
            
            if(heuristic.get() == Grid.Distance.NONE)
                heuristic.set(Grid.Distance.MANHATTAN);
        }

        @Override
        public Property<?>[] properties() {
            return new Property[]{ useMerge };
        }
        
        @Override
        public boolean isHeuristicRequired() {
            return true;
        }
        
        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public Astar build() {
            return new Astar(this);
        }
    }
    

    // ------

    private final boolean useMerge;

    
    // ------

    private Astar(Builder builder) {
        super(builder);

        useMerge = builder.useMerge.get() || (!currentSpace.getQueued().usesNaturalOrdering() && !currentSpace.getQueued().isSortable());
        setReady();
    }


    // ------
    
    public static String getShortName() { 
        return "A*"; 
    }

    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(currentSpace.getGoal(), heuristic) + g.getDepth());
    }

    @Override
    protected void step() {

        Grid newCurrent = currentSpace.getQueued().pollFirst();

        currentSpace.setCurrent(newCurrent);
        currentSpace.getExplored().add(newCurrent);
        
        var toAdd = currentSpace.getNewNeighbors(filterExplored, filterQueued, linkAlreadyExploredNeighbors);

        for(Grid g : toAdd) computeHeuristic(g);
        
        if(!toAdd.isEmpty()) {
            if(currentSpace.getQueued().usesNaturalOrdering()) 
                currentSpace.getQueued().addAll(toAdd);
            else if(useMerge) {
                currentSpace.getQueued().mergeWith(toAdd);
            }
            else {
                Collections.sort(toAdd);
                currentSpace.getQueued().addAll(toAdd);
                currentSpace.getQueued().sort();
            }
        }
    }
  
}
