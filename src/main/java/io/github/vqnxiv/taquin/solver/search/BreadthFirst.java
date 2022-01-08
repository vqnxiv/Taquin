package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.EnumMap;
import java.util.List;


public class BreadthFirst extends Search {


    public static class Builder extends Search.Builder<Builder> {

        private final BooleanProperty checkNewStatesForGoal = 
            new SimpleBooleanProperty(this, "check new states for goal", true);

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
                List.of(checkNewStatesForGoal)
            );

            return m;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected BreadthFirst build() {
            return new BreadthFirst(this);
        }
    }
    
    
    // ------

    public static final String SEARCH_SHORT_NAME = "BFS";

    private final boolean checkNewStatesForGoal;


    // ------
    
    private BreadthFirst(Builder builder) {
        super(builder);
        
        checkNewStatesForGoal = builder.checkNewStatesForGoal.get();
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
        
        Grid newCurrent = searchSpace.getQueued().pollFirst();
        log("Exploring new current: " + newCurrent.getKey());
        
        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);

        log("Generating neighbors");
        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkExistingNeighbors);

        if(heuristic != Grid.Distance.NONE) {
            log("Computing heuristics");
            for(Grid g : toAdd) computeHeuristic(g);
            toAdd.sort(heuristicComparator);
        }

        log("Checking for goal");
        if(checkNewStatesForGoal) 
            for (Grid g : toAdd) 
                if(searchSpace.isGoal(g)) 
                    searchSpace.setCurrent(g);

        log("Queuing " + toAdd.size() + " generated neighbors");
        searchSpace.getQueued().addAll(toAdd);
    }
    
}