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
        g.setHeuristicValue(g.distanceTo(currentSpace.getGoal(), heuristic));
    }

    @Override
    protected void step(){
        
        Grid newCurrent = currentSpace.getQueued().pollLast();
        currentSpace.setCurrent(newCurrent);
        currentSpace.getExplored().add(newCurrent);

        if(currentSpace.getCurrent().getDepth() < currentDepthLimit) {
            var toAdd = currentSpace.getNewNeighbors(filterExplored, filterQueued, linkAlreadyExploredNeighbors);
            
            if(heuristic != Grid.Distance.NONE) {
                for(Grid g : toAdd) computeHeuristic(g);
                toAdd.sort(Collections.reverseOrder());
            }

            if(checkNewStatesForGoal)
                for(Grid g : toAdd)
                    if(currentSpace.isGoal(g))
                        currentSpace.setCurrent(g);

            currentSpace.getQueued().addAll(toAdd);
        }
        
        // todo: add MAX_DEPTH_LIMIT to avoid infinite looping
        if(currentSpace.getQueued().isEmpty()) {
            currentDepthLimit += limitIncrement;
            currentSpace.getExplored().clear();
            currentSpace.getStart().resetNeighbors();
            currentSpace.getQueued().add(currentSpace.getStart());
        }
    }

}