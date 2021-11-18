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

        useMerge = builder.useMerge.get() || (!searchSpace.getQueued().usesNaturalOrdering() && !searchSpace.getQueued().isSortable());
        setReady();
    }


    // ------
    
    public static String getShortName() { 
        return "A*"; 
    }

    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic) + g.getDepth());
    }

    @Override
    protected void step() {

        Grid newCurrent = searchSpace.getQueued().pollFirst();

        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);
        
        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkAlreadyExploredNeighbors);

        for(Grid g : toAdd) computeHeuristic(g);
        
        if(!toAdd.isEmpty()) {
            if(searchSpace.getQueued().usesNaturalOrdering()) 
                searchSpace.getQueued().addAll(toAdd);
            else if(useMerge) {
                searchSpace.getQueued().mergeWith(toAdd);
            }
            else {
                Collections.sort(toAdd);
                searchSpace.getQueued().addAll(toAdd);
                searchSpace.getQueued().sort();
            }
        }
    }
  
}
