package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.controller.BuilderController;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import javafx.beans.property.*;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;


public class IterativeDeepening extends Search {


    public static class Builder extends Search.Builder<Builder> {

        private BooleanProperty checkNewStatesForGoal;
        private IntegerProperty initialDepthLimit;
        private IntegerProperty limitIncrement;

        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
            
            checkNewStatesForGoal = new SimpleBooleanProperty(this, "check new states for goal", false);
            initialDepthLimit = new SimpleIntegerProperty(this, "initial depth limit", 1);
            limitIncrement = new SimpleIntegerProperty(this, "limit increment", 1);
        }
        
        @Override
        public boolean isHeuristicRequired() {
            return false;
        }

        @Override
        public EnumMap<BuilderController.TabPaneItem, List<Property<?>>> getBatchProperties() {

            var m = super.getBatchProperties();
            m.put(
                BuilderController.TabPaneItem.SEARCH_EXTRA, 
                List.of(checkNewStatesForGoal, initialDepthLimit, limitIncrement)
            );

            return m;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected IterativeDeepening build() {
            return new IterativeDeepening(this);
        }
    }


    // ------

    public static final String SEARCH_SHORT_NAME = "IDDFS";

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