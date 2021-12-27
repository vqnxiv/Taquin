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


public class BreadthFirst extends Search {


    public static class Builder extends Search.Builder<Builder> {

        private BooleanProperty checkNewStatesForGoal;
        
        public Builder(Search.Builder<?> toCopy) {
            super(toCopy);
            
            checkNewStatesForGoal = new SimpleBooleanProperty(this, "check new states for goal",true);
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
        setReady();
    }


    // ------

    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic));
    }

    @Override
    protected void step(){
        
        Grid newCurrent = searchSpace.getQueued().pollFirst();
        
        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);

        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkAlreadyExploredNeighbors);

        if(heuristic != Grid.Distance.NONE) {
            for(Grid g : toAdd) computeHeuristic(g);
            Collections.sort(toAdd);
        }

        if(checkNewStatesForGoal) 
            for (Grid g : toAdd) 
                if(searchSpace.isGoal(g)) 
                    searchSpace.setCurrent(g);

        searchSpace.getQueued().addAll(toAdd);
    }
    
}