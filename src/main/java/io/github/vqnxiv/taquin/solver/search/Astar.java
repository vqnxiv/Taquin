package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.controller.BuilderController;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.*;


public class Astar extends Search {


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
        protected Astar build() {
            return new Astar(this);
        }
    }
    

    // ------

    public static final String SEARCH_SHORT_NAME = "A*";
    
    private boolean useMerge;

    
    // ------

    private Astar(Builder builder) {
        super(builder);

        builder.useMerge.get();
            
        setReady();
    }


    // ------
    
    @Override
    protected void setProperties() {
        useMerge = useMerge || (!searchSpace.getQueued().usesNaturalOrdering() && !searchSpace.getQueued().isSortable());

    }

    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue((float) g.distanceTo(searchSpace.getGoal(), heuristic) + g.getDepth());
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
        
        log("Queuing {}" + toAdd.size() + " generated neighbors");
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
