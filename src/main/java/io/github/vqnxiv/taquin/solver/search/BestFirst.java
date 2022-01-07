package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.controller.BuilderController;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;


public class BestFirst extends Search {


    public static class Builder extends Search.Builder<Builder> {

        private final BooleanProperty useMerge;

        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);

            useMerge = new SimpleBooleanProperty(this, "use merge", false);
            
            if(heuristic.get() == Grid.Distance.NONE)
                heuristic.set(Grid.Distance.MANHATTAN);
        }
        
        @Override
        public boolean isHeuristicRequired() {
            return true;
        }

        @Override
        public EnumMap<BuilderController.TabPaneItem, List<Property<?>>> getBatchProperties() {

            var m = super.getBatchProperties();
            m.put(
                BuilderController.TabPaneItem.SEARCH_EXTRA, 
                List.of(useMerge)
            );

            return m;
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

    private boolean useMerge;

    
    // ------

    private BestFirst(Builder builder) {
        super(builder);
        
        useMerge = builder.useMerge.get();
        setReady();
    }


    // ------

    @Override
    protected void setProperties() {
        useMerge = useMerge || (!searchSpace.getQueued().usesNaturalOrdering() && !searchSpace.getQueued().isSortable());

    }
    
    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic));
    }

    @Override
    protected void step() {
        
        Grid newCurrent = searchSpace.getQueued().pollFirst();
        log("Exploring new current: " + newCurrent.getKey());

        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);

        log("Generating neighbors");
        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkAlreadyExploredNeighbors);

        log("Computing heuristics");
        for(Grid g : toAdd) computeHeuristic(g);

        log("Queuing " + toAdd.size() + " generated neighbors");
        if(!toAdd.isEmpty()) {
            if(searchSpace.getQueued().usesNaturalOrdering()) {
                searchSpace.getQueued().addAll(toAdd);
            }
            else if(useMerge) {
                searchSpace.getQueued().mergeWith(toAdd);
            }
            else {
                toAdd.sort(heuristicComparator);
                searchSpace.getQueued().addAll(toAdd);
                searchSpace.getQueued().sort(heuristicComparator);
            }
        }
    }
    
}
