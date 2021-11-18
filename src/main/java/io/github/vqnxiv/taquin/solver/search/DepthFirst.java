package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Collections;


public class DepthFirst extends Search {

    
    public static class Builder extends Search.Builder<Builder> {

        public BooleanProperty checkNewStatesForGoal;
        
        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
            
            checkNewStatesForGoal = new SimpleBooleanProperty(false);
        }
        
        @Override
        public Property<?>[] properties() {
            return new Property[]{ checkNewStatesForGoal };
        }
        
        @Override
        public boolean isHeuristicRequired() {
            return false;
        }
        
        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public DepthFirst build() {
            return new DepthFirst(this);
        }
    }
    

    // ------

    private final boolean checkNewStatesForGoal;


    // ------

    private DepthFirst(Builder builder) {
        super(builder);
        
        checkNewStatesForGoal = builder.checkNewStatesForGoal.get();
        setReady();
    }


    // ------
    
    public static String getShortName() { 
        return "DFS"; 
    }

    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic));
    }

    @Override
    protected void step(){
        Grid newCurrent = searchSpace.getQueued().pollLast();

        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);

        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkAlreadyExploredNeighbors);

        if(heuristic != Grid.Distance.NONE) {
            for(Grid g : toAdd) computeHeuristic(g);
            toAdd.sort(Collections.reverseOrder());
        }

        if(checkNewStatesForGoal) 
            for(Grid g : toAdd) 
                if(searchSpace.isGoal(g)) 
                    searchSpace.setCurrent(g);

        searchSpace.getQueued().addAll(toAdd);
    }
    
}
