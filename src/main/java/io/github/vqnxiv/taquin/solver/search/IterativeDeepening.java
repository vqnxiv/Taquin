package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import javafx.beans.property.*;
import java.util.Collections;


public class IterativeDeepening extends Search {


    public static class Builder extends Search.Builder<Builder> {

        public BooleanProperty checkNewStatesForGoal;
        public IntegerProperty initialDepthLimit;
        public IntegerProperty limitIncrement;

        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
            
            checkNewStatesForGoal = new SimpleBooleanProperty(this, "Check new states for goal", false);
            initialDepthLimit = new SimpleIntegerProperty(this, "Initial depth limit", 1);
            limitIncrement = new SimpleIntegerProperty(this, "Limit increment", 1);
        }
        
        @Override
        public Property<?>[] properties() {
            return new Property[]{ checkNewStatesForGoal, initialDepthLimit, limitIncrement };
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
        public IterativeDeepening build() {
            return new IterativeDeepening(this);
        }
    }


    // ------

    private final boolean checkNewStatesForGoal;
    private final int initialDepthLimit;
    private final int limitIncrement;

    private int currentDepthLimit;
    
    
    // ------

    public IterativeDeepening(Builder builder) {
        super(builder);

        checkNewStatesForGoal = builder.checkNewStatesForGoal.get();
        initialDepthLimit = builder.initialDepthLimit.get();
        limitIncrement = builder.limitIncrement.get();
        
        currentDepthLimit = initialDepthLimit;
        setReady();
    }


    // ------

    public static String getShortName() {
        return "IDDFS";
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
        
        if(searchSpace.getCurrent().getDepth() < currentDepthLimit) {
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
        
        // considering there's (n*m)! / 2 accessible configurations maybe just cap at Integer.MAX_VALUE
        // should be enough to still be higher than the upper bounds and way higher than the lower bounds
        // (which IDDFS is supposed to reach)
        if(searchSpace.getQueued().isEmpty() && currentDepthLimit < Integer.MAX_VALUE) {
            currentDepthLimit += limitIncrement;
            searchSpace.getExplored().clear();
            searchSpace.getStart().resetNeighbors();
            searchSpace.getQueued().add(searchSpace.getStart());
        }
    }

}