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

        private BooleanProperty useMerge;

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

    private final boolean useMerge;

    
    // ------

    private BestFirst(Builder builder) {
        super(builder);
        
        useMerge = builder.useMerge.get() || (!searchSpace.getQueued().usesNaturalOrdering() && !searchSpace.getQueued().isSortable());
        setReady();
    }


    // ------

    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic));
    }

    @Override
    protected void step() {
        
        Grid newCurrent = searchSpace.getQueued().pollFirst();

        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);

        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkAlreadyExploredNeighbors);

        for(Grid g : toAdd) computeHeuristic(g);

        if(!toAdd.isEmpty()) {
            if(searchSpace.getQueued().usesNaturalOrdering()) {
                searchSpace.getQueued().addAll(toAdd);
            }
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
