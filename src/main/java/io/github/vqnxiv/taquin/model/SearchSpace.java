package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.*;


/**
 * This class models a search space, defined by
 * <ul>
 *     <li>A starting point: {@link #startGrid}</li>
 *     <li>An end point: {@link #goalGrid}</li>
 *     <li>A set of explored points: {@link # explored}</li>
 *     <li>A set of queued points: {@link # queued}</li>
 * </ul>
 * <p>
 * This class is a simple model class and does nothing on its own. A {@link io.github.vqnxiv.taquin.solver.Search}
 * object should be injected an instance of {@link SearchSpace} to perform an actual search.
 * 
 * @see Grid
 * @see io.github.vqnxiv.taquin.solver.Search
 */
public class SearchSpace {

    /**
     * Builder class.
     */
    public static class Builder implements IBuilder {

        /**
         * {@link DataStructure.Builder} for {@link SearchSpace#explored}.
         */
        private DataStructure.Builder explored;

        /**
         * {@link DataStructure.Builder} for {@link SearchSpace#queued}
         */
        private DataStructure.Builder queued;
        
        /**
         * {@link ObjectProperty} for {@link SearchSpace#startGrid}.
         */
        private final ObjectProperty<Grid> start;

        /**
         * {@link ObjectProperty} for {@link SearchSpace#goalGrid}.
         */
        private final ObjectProperty<Grid> end;


        /**
         * Constructor.
         */
        public Builder() {
            start = new SimpleObjectProperty<>(this, "start", Grid.invalidOfSize(3, 3));
            end = new SimpleObjectProperty<>(this, "end", Grid.invalidOfSize(3, 3));
        }
        
        /**
         * Setter for {@link #explored}.
         * @param e The new value for {@link #explored}.
         * @return this object.
         */
        public Builder explored(DataStructure.Builder e) {
            explored = e;
            return this;
        }

        /**
         * Setter for {@link #queued}.
         * @param q The new value for {@link #queued}.
         * @return this object.
         */
        public Builder queued(DataStructure.Builder q) {
            queued = q;
            return this;
        }
        
        /**
         * Build method.
         * 
         * @return a {@link SearchSpace} with the contents of {@link #explored}, {@link #queued},
         * {@link #start} and {@link #end}.
         */
        public SearchSpace build() {
            if(start.get() == null || end.get() == null || explored == null || queued == null) {
                throw new NullPointerException();
            }
            
            return new SearchSpace(start.getValue(), end.getValue(), explored, queued);
        }

        /**
         * {@link IBuilder} method.
         * 
         * @return {@link Map} which contains {@link #start} and {@link #end}.
         */
        @Override
        public Map<String, Property<?>> getNamedProperties() {
            return Map.of(
                start.getName(), start,
                end.getName(), end
            );
        }

        /**
         * {@link IBuilder} method.
         *
         * @return Empty {@link Map}.
         */
        @Override
        public EnumMap<Category, List<Property<?>>> getBatchProperties() {
            return new EnumMap<>(Category.class);
        }
    }

    /**
     * The starting point from which a {@link io.github.vqnxiv.taquin.solver.Search} can be performed.
     */
    private final Grid startGrid;

    /**
     * The goal point of a {@link io.github.vqnxiv.taquin.solver.Search}.
     */
    private final Grid goalGrid;

    /**
     * The current point of a {@link io.github.vqnxiv.taquin.solver.Search}.
     */
    private Grid currentGrid;

    /**
     * {@link DataStructure} which contains explored grids.
     */
    private final DataStructure<Grid> explored;

    /**
     * {@link DataStructure} which contains queued grids.
     */
    private final DataStructure<Grid> queued;

    /**
     * The counter for the states' key.
     */
    private int currentKeyCounter = 0;

    /**
     * Property which contains {@link #currentGrid}.
     */
    private final ObjectProperty<Grid> currentGridProperty;


    /**
     * Constructor.
     * 
     * @param start The start grid.
     * @param end The goal grid.
     * @ param explored The {@link DataStructure} to use to store the explored grids.
     * @ param queued The {@link DataStructure} data structure to use to store the queued grids.
     */
    @SuppressWarnings("unchecked")
    private SearchSpace(Grid start, Grid end, DataStructure.Builder exploredBuilder, DataStructure.Builder queuedBuilder) {

        startGrid = start;
        goalGrid = end;

        startGrid.setKey(0);
        goalGrid.setKey(-1);

        explored = (DataStructure<Grid>) exploredBuilder.build();
        queued = (DataStructure<Grid>) queuedBuilder.build();
        
        this.queued.add(startGrid);
        currentGrid = startGrid;

        currentGridProperty = new SimpleObjectProperty<>(currentGrid);
    }


    /**
     * Getter for {@link #startGrid}.
     * 
     * @return {@link #startGrid}.
     */
    public Grid getStart() { 
        return startGrid; 
    }

    /**
     * Getter for {@link #currentGrid}.
     *
     * @return {@link #currentGrid}.
     */
    public Grid getCurrent() { 
        return currentGrid; 
    }

    /**
     * Getter for {@link #goalGrid}.
     *
     * @return {@link #goalGrid}.
     */
    public Grid getGoal() { 
        return goalGrid; 
    }

    /**
     * Getter for {@link #startGrid}, {@link #currentGrid}, {@link #goalGrid}.
     * 
     * @return Grid array of {@link #startGrid}, {@link #currentGrid}, {@link #goalGrid}.
     */
    public Grid[] getGrids(){ 
        return new Grid[] { startGrid, currentGrid, goalGrid }; 
    }

    /**
     * Getter for {@link #currentGridProperty}.
     * 
     * @return {@link #currentGridProperty}.
     */
    public ReadOnlyObjectProperty<Grid> currentGridProperty() {
        return currentGridProperty;
    }

    /**
     * Getter for {@link #explored}.
     *
     * @return {@link #explored}.
     */
    public DataStructure<Grid> getExplored() {
        return explored;
    }

    /**
     * Getter for {@link #queued}.
     *
     * @return {@link #queued}.
     */
    public DataStructure<Grid> getQueued() {
        return queued;
    }
    
    /**
     * Setter for {@link #currentGrid}. Also updates {@link #currentGridProperty}.
     * 
     * @param g The {@link Grid} to set for {@link #currentGrid}.
     */
    public void setCurrent(Grid g) { 
        currentGrid = g;
        currentGridProperty.setValue(g);
    }

    /**
     * Checks whether {@link #currentGrid} is equal to {@link #goalGrid}.
     * 
     * @return {@code true} if {@link #currentGrid} is equal to {@link #goalGrid}; {@code false} otherwise.
     */
    public boolean isCurrentGoal() { 
        return currentGrid.equals(goalGrid); 
    }

    /**
     * Checks whether a {@link Grid} is 'goal', i.e {@code g.equals(goalGrid)} returns {@code true}.
     * 
     * @param g The grid to check.
     * @return {@code true} if {@code g} is equal to {@link #goalGrid}; {@code false} otherwise.
     */
    public boolean isGoal(Grid g) { 
        return g.equals(goalGrid); 
    }

    /**
     * Gets the 'path' from {@link #startGrid} to {@link #currentGrid}.
     * 
     * @return A {@link List} of the {@link Grid}s from {@link #startGrid} (inclusive) 
     * to {@link #currentGrid} (inclusive).
     */
    public List<Grid> pathFromStart() {
        var l = new ArrayList<Grid>();
        var g = currentGrid;
        
        while(!g.equals(startGrid)) {
            l.add(g);
            g = g.getParent();
        }

        // start
        l.add(g);
        
        l.sort(Comparator.comparingInt(Grid::getDepth));
        
        return l;
    }

    /**
     * Gets the neighbors of {@link #currentGrid}, except for the parent grid {@link #currentGrid}
     * was generated from. (see {@link Grid#generateNeighbors()} for details)
     * <p>
     * Do note that {@code linkExisting} is potentially a more costly operation than simply filtering out
     * already existing neighbors, as the {@link Grid} which is used to check whether the neighbor
     * is already in {@link #explored} or {@link #queued} is an incomplete {@link Grid} 
     * and not the actual existing neighbor. So for some {@link Collection} we have to re-iterate over
     * {@link #explored} or {@link #queued} to fetch the actual {@link Grid} reference.
     * <p>
     * On the brighter side i might actually make this decent someday, i forgot how crap it was
     * 
     * @param filterExplored whether to filter out the neighbors that already are in {@link #explored}.
     * @param filterQueued whether to filter out the neighbors that already are in {@link #queued}.
     * @param linkExisting whether to link {@link #currentGrid} to its existing neighbors. 
     *                     ({@link #linkExisting(Grid)})
     * @param <T> A {@link Collection} which extends both {@link Queue} and {@link List}, 
     *           so likely a {@link LinkedList}.
     * @return A {@link Collection} of the new neighbors.
     */
    @SuppressWarnings("unchecked")
    public <T extends Queue<Grid> & List<Grid>> T getNewNeighbors(
        boolean filterExplored, boolean filterQueued, boolean linkExisting
    ) {

        var possibleNewStates = currentGrid.generateNeighbors();
        var retour = new LinkedList<Grid>();
        
        for(Grid g : possibleNewStates){
            
            if((filterExplored && explored.contains(g)) || (filterQueued && queued.contains(g))) {
                if(linkExisting) {
                    linkExisting(g);
                }
            }
            else {
                g.setKey(currentKeyCounter);
                currentKeyCounter++;
                currentGrid.addNeighbor(g, true);
                retour.add(g);
            }
        }

        return (T) retour;
    }

    /**
     * Method which links a {@link Grid} to its neighbors that exist in {@link #explored} or {@link #queued},
     * and calls {@link Grid#addNeighbor(Grid, boolean)} with the found neighbors.
     * <p>
     * As it iterates through both {@link #explored} and {@link #queued}, this can be
     * a time consuming operations and as such should not be done if speed is at matter.
     * 
     * @param g The {@link Grid} to link.
     */
    private void linkExisting(Grid g) {
        boolean wasFound = false;

        
        for(Grid g2 : explored) {
            if(g.equals(g2)) {
                g = g2;
                wasFound = true;
            }
        }

        if(!wasFound) {
            for(Grid g2 : queued) {
                if(g.equals(g2)) {
                    g = g2;
                    wasFound = true;
                }
            }
        }
        
        
        if(wasFound) currentGrid.addNeighbor(g, false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        return "Start:\n" + startGrid.toString() + 
                "\nCurrent:\n" + currentGrid.toString() + 
                "\nSolved:\n" + goalGrid.toString();
    }

}