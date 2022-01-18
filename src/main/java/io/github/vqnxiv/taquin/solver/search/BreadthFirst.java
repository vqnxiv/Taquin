package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.solver.Search;
import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.EnumMap;
import java.util.List;


/**
 * This class represents a search using the Breadth First Search algorithm, 
 * which does not require an heuristic.
 */
public class BreadthFirst extends Search {
    
    /**
     * Builder. 
     */
    public static class Builder extends Search.Builder<Builder> {

        /**
         * Whether to check if the goal state is among newly generated states
         * before adding them to the queue.
         */
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

        /**
         * Whether this search requires an heuristic.
         *
         * @return {@code false}.
         */
        @Override
        public boolean isHeuristicRequired() {
            return false;
        }

        /**
         * Returns the base search batch properties and {@link #checkNewStatesForGoal}.
         * 
         * @return {@link Search.Builder#getBatchProperties()} and {@link #checkNewStatesForGoal}.
         */
        @Override
        public EnumMap<Category, List<Property<?>>> getBatchProperties() {

            var m = super.getBatchProperties();
            m.put(
                IBuilder.Category.SEARCH_EXTRA, 
                List.of(checkNewStatesForGoal)
            );

            return m;
        }

        /**
         * Method used to chain setters calls.
         *
         * @return This instance of {@link Builder}.
         */
        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Build method.
         *
         * @return A new instance of {@link BreadthFirst}.
         */
        @Override
        protected BreadthFirst build() {
            return new BreadthFirst(this);
        }
    }


    /**
     * This search's shortname, which will be displayed on the GUI.
     */
    public static final String SEARCH_SHORT_NAME = "BFS";

    /**
     * Whether to check if new states contain the goal state before queuing them. 
     */
    private final boolean checkNewStatesForGoal;


    /**
     * Constructor.
     *
     * @param builder {@link Builder}.
     */
    private BreadthFirst(Builder builder) {
        super(builder);
        
        checkNewStatesForGoal = builder.checkNewStatesForGoal.get();
    }


    /**
     * {@inheritDoc}
     * <p>
     * This method is empty as there is no extra parameters to set.
     */
    @Override
    protected void setSpaceDependentParameters() {
        // no additional parameters to set
    }

    /**
     * Computes the heuristic value for the given {@link Grid}, 
     * as per the Greedy Best First algorithm.
     * <p>
     * The value is defined as the result {@link #heuristic} for the given {@link Grid}.
     *
     * @param g The {@link Grid} to compute the heuristic value for.
     */
    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic));
    }

    /**
     * Represents a step from the Breadth First algorithm.
     * <p>
     * Explores the first state in the queue, generates its neighbors
     * and add them at the end of the queue.
     * <p>
     * Potentially filters out already explored or queued states, 
     * computes new states' heuristic value, and checks whether
     * the goal state is among them.
     */
    @Override
    protected void step(){

        Grid newCurrent = searchSpace.getQueued().dsPollFirst();
        log("Exploring new current: " + newCurrent.getKey());
        
        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);

        log("Generating neighbors");
        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkExistingNeighbors);

        if(heuristic != Grid.Distance.NONE) {
            log("Computing heuristics");
            for(Grid g : toAdd) {
                computeHeuristic(g);
            }
            toAdd.sort(heuristicComparator);
        }

        log("Checking for goal");
        if(checkNewStatesForGoal) {
            for(Grid g : toAdd) {
                if(searchSpace.isGoal(g)) {
                    searchSpace.setCurrent(g);
                }
            }
        }
        
        log("Queuing " + toAdd.size() + " generated neighbors");
        searchSpace.getQueued().addAll(toAdd);
    }
    
}