package io.github.vqnxiv.taquin.solver.search;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.structure.Sortable;
import io.github.vqnxiv.taquin.model.structure.Sorted;
import io.github.vqnxiv.taquin.solver.Search;


/**
 * This class represents a search using the A* algorithm, 
 * which is an informed search and thus requires an heuristic.
 */
public class Astar extends Search {
    
    /**
     * Builder.
     */
    public static class Builder extends Search.Builder<Builder> {
        
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

        /**
         * Whether this search requires an heuristic.
         * 
         * @return {@code true}.
         */
        @Override
        public boolean isHeuristicRequired() {
            return true;
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
         * @return A new instance of {@link Astar}.
         */
        @Override
        protected Astar build() {
            return new Astar(this);
        }
    }


    /**
     * This search's shortname, which will be displayed on the GUI.
     */
    public static final String SEARCH_SHORT_NAME = "A*";


    /**
     * Constructor.
     * 
     * @param builder {@link Builder}.
     */
    private Astar(Builder builder) {
        super(builder);
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
     * as per the A* algorithm.
     * <p>
     * The value is defined as {@link #heuristic} {@code +} the depth of the {@link Grid}.
     * 
     * @param g The {@link Grid} to compute the heuristic value for.
     */
    @Override
    protected void computeHeuristic(Grid g) {
        g.setHeuristicValue(g.distanceTo(searchSpace.getGoal(), heuristic) + g.getDepth());
    }

    /**
     * Represents a step from the A* algorithm.
     * <p>
     * Explores the most promising state as per its heuristic value,
     * generates its neighbors and, for each neighbor that hasn't
     * been queued or explored yet, compute their heuristic values
     * then adds them to the queue.
     */
    @Override
    protected void step() {

        Grid newCurrent = searchSpace.getQueued().dsPollFirst();
        log("Exploring new current: " + newCurrent.getKey());
        
        searchSpace.setCurrent(newCurrent);
        searchSpace.getExplored().add(newCurrent);
        
        log("Generating neighbors");
        var toAdd = searchSpace.getNewNeighbors(filterExplored, filterQueued, linkExistingNeighbors);

        log("Computing heuristics");
        for(Grid g : toAdd) computeHeuristic(g);
        
        log("Queuing " + toAdd.size() + " generated neighbors");
        if(!toAdd.isEmpty()) {
            if(searchSpace.getQueued() instanceof Sorted<Grid>) {
                searchSpace.getQueued().addAll(toAdd);
            }
            else if(searchSpace.getQueued() instanceof Sortable<Grid> s) {
                toAdd.sort(heuristicComparator);
                s.addAll(toAdd);
                s.sort(heuristicComparator);
            }
        }
    }
  
}
