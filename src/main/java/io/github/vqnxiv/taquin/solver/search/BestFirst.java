package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.structure.Sortable;
import io.github.vqnxiv.taquin.model.structure.Sorted;
import io.github.vqnxiv.taquin.solver.Search;

import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.EnumMap;
import java.util.List;


public class BestFirst extends Search {


    public static class Builder extends Search.Builder<Builder> {

        // private final BooleanProperty useMerge = 
        //    new SimpleBooleanProperty(this, "use merge", false);
        
        /**
         * Base no args constructor.
         * <p>
         * Sets the value of {@link Search.Builder#heuristic} to {@link Grid.Distance#LINEAR_MANHATTAN}.
         */
        public Builder() {
            super();

            heuristic.set(Grid.Distance.LINEAR_MANHATTAN);
        }

        /**
         * Copy constructor. Used when converting from a subclass to another.
         * <p>
         * Sets the value of {@link Search.Builder#heuristic} to {@link Grid.Distance#LINEAR_MANHATTAN}.
         *
         * @param toCopy The builder to copy.
         */
        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
            
            if(heuristic.get() == Grid.Distance.NONE) {
                heuristic.set(Grid.Distance.LINEAR_MANHATTAN);
            }
        }
        
        @Override
        public boolean isHeuristicRequired() {
            return true;
        }

        @Override
        public EnumMap<Category, List<Property<?>>> getBatchProperties() {

            // var m = super.getBatchProperties();
            // m.put(
            //     IBuilder.Category.SEARCH_EXTRA, 
            //     List.of(useMerge)
            // );

            // return m;
            
            return super.getBatchProperties();
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected BestFirst build() {
            return new BestFirst(this);
        }
    }
    
    
    // ------
    
    public static final String SEARCH_SHORT_NAME = "GBFS";

    // private boolean useMerge;

    
    // ------

    private BestFirst(Builder builder) {
        super(builder);
        
        // useMerge = builder.useMerge.get();
    }


    // ------

    @Override
    protected void setSpaceDependentParameters() {
        // useMerge = useMerge || (!searchSpace.getQueued().usesNaturalOrdering() && !searchSpace.getQueued().isSortable());

    }
    
    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic));
    }

    @Override
    protected void step() {

        // Grid newCurrent = searchSpace.getQueued().pollFirst();
        Grid newCurrent = searchSpace.getQueued().dsPollFirst();
        log("Exploring new current: " + newCurrent.getKey());

        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);

        log("Generating neighbors");
        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkExistingNeighbors);

        log("Computing heuristics");
        for(Grid g : toAdd) computeHeuristic(g);

        log("Queuing {}" + toAdd.size() + " generated neighbors");
        if(!toAdd.isEmpty()) {
            if(searchSpace.getQueued() instanceof Sorted<Grid>) {
                searchSpace.getQueued().addAll(toAdd);
            }
            // else if(useMerge) {
            //     searchSpace.getQueued().mergeWith(toAdd);
            // }
            else if(searchSpace.getQueued() instanceof Sortable<Grid> s) {
                toAdd.sort(heuristicComparator);
                // searchSpace.getQueued().addAll(toAdd);
                // searchSpace.getQueued().sort(heuristicComparator);
                s.addAll(toAdd);
                s.sort(heuristicComparator);
            }
        }
    }
    
}
