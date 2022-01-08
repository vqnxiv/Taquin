package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.*;

import java.util.EnumMap;
import java.util.List;


public class IterativeDeepening extends Search {


    public static class Builder extends Search.Builder<Builder> {

        private final BooleanProperty checkNewStatesForGoal = 
            new SimpleBooleanProperty(this, "check new states for goal", false);
        
        private final IntegerProperty initialDepthLimit = 
            new SimpleIntegerProperty(this, "initial depth limit", 1);
        
        private final IntegerProperty limitIncrement =
            new SimpleIntegerProperty(this, "limit increment", 1);


        /**
         * Base no args constructor.
         */
        public Builder() {
            super();
        }

        /**
         * Copy constructor. Used when converting from a subclass to another.
         *
         * @param toCopy The builder to copy.
         */
        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
        }
        
        @Override
        public boolean isHeuristicRequired() {
            return false;
        }

        @Override
        public EnumMap<Category, List<Property<?>>> getBatchProperties() {
            var m = super.getBatchProperties();
            m.put(
                IBuilder.Category.SEARCH_EXTRA, 
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
    }


    // ------

    @Override
    protected void setSpaceDependentParameters() { }
    
    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic));
    }

    @Override
    protected void step(){
        
        Grid newCurrent = searchSpace.getQueued().pollLast();
        log("Exploring new current: " + newCurrent.getKey());
        
        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);
        
        if(searchSpace.getCurrent().getDepth() < currentDepthLimit) {
            log("Generating neighbors");
            var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkExistingNeighbors);
            
            if(heuristic != Grid.Distance.NONE) {
                log("Computing heuristics");
                for(Grid g : toAdd) computeHeuristic(g);
                toAdd.sort(reverseHeuristicComparator);
            }

            log("Checking for goal");
            if(checkNewStatesForGoal)
                for(Grid g : toAdd)
                    if(searchSpace.isGoal(g))
                        searchSpace.setCurrent(g);

            log("Queuing " + toAdd.size() + " generated neighbors");
            searchSpace.getQueued().addAll(toAdd);
        }
        
        // considering there's (n*m)! / 2 accessible configurations maybe just cap at Integer.MAX_VALUE
        // should be enough to still be higher than the upper bounds and way higher than the lower bounds
        // (which IDDFS is supposed to reach)
        if(searchSpace.getQueued().isEmpty() && currentDepthLimit < Integer.MAX_VALUE) {
            currentDepthLimit += limitIncrement;
            log("Increasing depth limit: " + currentDepthLimit);
            searchSpace.getExplored().clear();
            searchSpace.getStart().resetNeighbors();
            searchSpace.getQueued().add(searchSpace.getStart());
        }
    }

}