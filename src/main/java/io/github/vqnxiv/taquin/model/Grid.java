package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;


/**
 * This class models the game's grids with an 2d int array.
 * 
 * @see SearchSpace
 * @see io.github.vqnxiv.taquin.solver.Search
 */
public class Grid implements Comparable<Grid> {

    /**
     * {@link Record} to store a tile's coordinates.
     */
    private record Coordinates(int row, int column) {}
    
    /**
     * Enum for a blank tile move's possible directions.
     * <p>
     * As the grid is 2d, there is at most only four possible directions: 
     * LEFT, RIGHT, UP, DOWN.
     * <p>
     * A move is considered valid if moving the blank tiles does not result in 
     * attempting to place it out of the bounds of the 2d array
     */
    private enum Direction {
        /**
         * Left based movement, i.e decreasing by one a pair of coordinates column index.
         */
        LEFT(
            (g -> g.zero.column() > 0),
            (c -> new Coordinates(c.row(), c.column() - 1))
        ),
        /**
         * Right based movement, i.e increasing by one a pair of coordinates column index.
         */
        RIGHT(
            (g -> g.zero.column() < g.self[0].length - 1),
            (c -> new Coordinates(c.row(), c.column() + 1))
        ),
        /**
         * Upward based movement, i.e decreasing by one a of pair coordinates row index.
         */
        UP(
            (g -> g.zero.row() > 0),
            (c -> new Coordinates(c.row() - 1, c.column()))
        ),
        /**
         * Downward based movement, i.e increasing by one a pair of coordinates row index.
         */
        DOWN(
            (g -> g.zero.row() < g.self.length - 1),
            (c -> new Coordinates(c.row() + 1, c.column()))
        );

        /**
         * The function which determines whether the move is valid.
         */
        private final Function<Grid, Boolean> check;

        /**
         * Function which translates a pair of coordinates according 
         * to the move's definition.
         */
        private final UnaryOperator<Coordinates> move;

        /**
         * Enum constructor
         * 
         * @param c validation function.
         * @param m The operator for {@link #move}.
         */
        Direction(Function<Grid, Boolean> c, UnaryOperator<Coordinates> m) {
            check = c;
            move = m;
        }

        /**
         * The method which is called when proceeding to validation.
         * 
         * @param g the {@code Grid} to check
         * @return {@code true} if the move is valid, {@code false} otherwise
         */
        private boolean check(Grid g) {
            return check.apply(g);
        }

        /**
         * The method which is called when translating coordinates.
         *
         * @param c The coordinates to translate.
         * @return A new {@link Coordinates} instance with the move.
         */
        private Coordinates move(Coordinates c) {
            return move.apply(c);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }

    /**
     * Enum which is used to compute the distances between two {@code Grid}s.
     * <p>
     * The grids are assumed to be compatible, i.e {@code g1} and {@code g2} such that {@code}
     * {@code g1.checkCompatibility(g2)} returns {@code true}.
     */
    public enum Distance {
        /**
         * No computation.
         */
        NONE(
            (x, y) -> 0f
        ),
        /**
         * Manhattan distance. {@link #manhattan(Grid)}
         */
        MANHATTAN(
            (x, y) -> x.manhattan(y)
        ),
        /**
         * Hamming distance. {@link #hamming(Grid)}
         */
        HAMMING(
            (x, y) -> x.hamming(y)
        ),
        /**
         * Euclidean distance. {@link #euclidean(Grid)}
         */
        EUCLIDEAN(
            (x, y) -> x.euclidean(y)
        ),
        /**
         * Linear conflits and manhattan distance. {@link #linearManhattan(Grid)}
         */
        LINEAR_MANHATTAN(
            (x, y) -> x.linearManhattan(y)
        );

        /**
         * Function which is used to compute the distance
         */
        private final BiFunction<Grid, Grid, Float> function;

        /**
         * Enum contstuctor
         * 
         * @param func the function called by {@code calc}
         */
        Distance(BiFunction<Grid, Grid, Float> func) {
            function = func;
        }

        /**
         * The method which is called when calculating the distance.
         * <p>
         * WARNING: no validation is done when attempting to compute the distance.
         * As such, Exceptions may be thrown (NullPointerExceptions).
         * 
         * @param g1 a {@code Grid} such that {@code g1.checkCompatibility(g2)} returns {@code true}
         * @param g2 a {@code Grid} such that {@code g2.checkCompatibility(g1)} returns {@code true}
         * @return the distance as an int
         */
        private float calc(Grid g1, Grid g2) {
            return function.apply(g1, g2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }

    /**
     * Enum used to decide between two {@link Grid}s which have the same {@link #heuristicValue}.
     */
    public enum EqualPolicy {
        /**
         * Neutral comparison.
         */
        NONE            ((x, y) -> 0),
        /**
         * At random.
         */
        RANDOM          ((x, y) -> (ThreadLocalRandom.current().nextBoolean()) ? 1 : -1),
        /**
         * The more recent {@link Grid} as determined by {@link #key}.
         */
        NEWER_FIRST     ((x, y) -> (x.key < y.key) ? -1 : 1),
        /**
         * The older {@link Grid} as determined by {@link #key}.
         */
        OLDER_FIRST     ((x, y) -> (x.key < y.key) ? 1 : -1),
        /**
         * The higher {@link Grid} as determined by {@link #depth}.
         */
        HIGHER_FIRST    ((x, y) -> (x.depth <= y.depth) ? -1 : 1),
        /**
         * The deeper {@link Grid} as determined by {@link #depth}.
         */
        DEEPER_FIRST    ((x, y) -> (x.depth <= y.depth) ? 1 : - 1);

        /**
         * The function which is called by {@link #calc}
         */
        private final BiFunction<Grid, Grid, Integer> function;

        /**
         * Enum constructor
         * 
         * @param func the function
         */
        EqualPolicy(BiFunction<Grid, Grid, Integer> func) {
            function = func;
        }

        /**
         * The method which should be called when comparing two grids 
         * with the same heuristic value.
         * 
         * @param g1 {@link Grid}
         * @param g2 {@link Grid}
         * @return {@code 1} if {@code g1} is determined to be greater than {@code g2}; 
         * {@code -1} otherwise
         */
        public int calc(Grid g1, Grid g2) {
            return function.apply(g1, g2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }
    

    /**
     * Root logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(Grid.class);

    /**
     * The 2d array which represents the grid.
     * <p>
     * {@code 0} is used as the 'blank' value for the blank tile which is moved
     * when a {@link io.github.vqnxiv.taquin.solver.Search} is run.
     * <p>
     * Numbers must be positive and unique for the grid to be valid.
     */
    private final int[][] self;

    /**
     * The key is the order in which the {@code Grid}s were generated and validated within 
     * a {@link SearchSpace}.
     * <p>
     * {@code -2} is an illegal placeholder value until the {@link Grid} gets validated
     * by the {@link SearchSpace} it was generated from).
     * <p>
     * {@code -1} is the value reserved for pre-generated grids such as the start and the
     * end of a {@link SearchSpace}.
     * <p>
     * {@code 0} is the value for the first {@code Grid} generated and validated within a 
     * {@code SpaceSearch}
     */
    private int key = -2;

    /**
     * The depth is the number of steps between a {@link SearchSpace}'s start {@link Grid} 
     * and this object.
     * <p>
     * i.e how many times you can call {@code g2 = g1.getParent().getParent().getParent()}
     * until {@code g2.equals(start)} returns {@code true} for that {@link SearchSpace}.
     */
    private final int depth;

    /**
     * The direction in which you can generate {@link #parent}.
     * <p>
     * It is the opposite of the direction used when {@link #generateNeighbors()} was called
     * on {@link #parent} (e.g {@link Direction#LEFT} -> {@link Direction#RIGHT}).
     */
    private final Direction parentDirection;

    /**
     * The {@link Grid} this object was generated from ({@link #generateNeighbors()}).
     */
    private final Grid parent;

    /**
     * The grids generated from calling {@link #generateNeighbors()} on this object
     * which were valided by the {@link SearchSpace} this object belongs to.
     */
    private List<Grid> hasGenerated;

    /**
     * The grids generated from calling {@link SearchSpace} on this object
     * but were not valided by the {@link SearchSpace} this object belongs to
     */
    private List<Grid> existingNeighbors;

    /**
     * The heuristic value for this object, as calculated by a {@link io.github.vqnxiv.taquin.solver.Search}'s
     * {@link Distance}.
     */
    private float heuristicValue = Float.MAX_VALUE;

    /**
     * Distance values for this object. Used for faster distance calculation for children grids.
     */
    private final EnumMap<Distance, Float> distanceMap;

    /**
     * The coordinates of the cell with a value of zero (considered the blank tile).
     */
    private final Coordinates zero;

    
    /**
     * Constructor which is called from the factory method {@code of}.
     * 
     * @param content the array which {@code self} will become a deep copy of
     */
    private Grid(int[][] content) {
        self = Arrays.stream(content).map(int[]::clone).toArray(t -> content.clone());

        // so we don't have to deal with NPE?
        parent = this;
        parentDirection = null;
        depth = 0;
        
        distanceMap = new EnumMap<>(Distance.class);
        
        zero = findCoordinates(0).orElseThrow();
    }

    /**
     * Constructor which is called from neigbor generation.
     * <p>
     * The parent grid is the grid on which calling {@code generateNeighbors} 
     * would call this constructor.
     * 
     * @param from the parent {@code Grid} 
     * @param d the {@code Direction} which determined this move from {@code from} to be valid
     */
    private Grid(Grid from, Direction d) {
        self = Arrays.stream(from.self).map(int[]::clone).toArray(t -> from.self.clone());

        parent = from;
        depth = from.depth+1;

        parentDirection = switch(d) {
            case RIGHT -> Direction.LEFT;
            case LEFT -> Direction.RIGHT;
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
        };
        
        distanceMap = new EnumMap<>(Distance.class);

        zero = d.move(from.zero);
        
        self[from.zero.row()][from.zero.column()] = self[zero.row()][zero.column()];
        self[zero.row()][zero.column()] = 0;
    }

    /**
     * Static factory method which creates a {@code Grid} if the given 2d int array is found to be valid
     * <p>
     * A valid array/{@code Grid} is considered as such:
     * <ul>
     * <li>only positive numbers</li>
     * <li>no duplicates</li>
     * <li>must contain one cell with a value of zero (0)</li>
     * <li>all the rows must have the same length</li>
     * <li>all the columns must have the same length</li>
     * </ul>
     * 
     * @param array the content of the {@code Grid} to be created
     * @return {@code Optional} of the created {@code Grid} if {@code array} was valid;
     * empty {@code Optional} otherwise
     */
    public static Optional<Grid> of(int[][] array) {
        record Pair(int row, int col) {
            @Override
            public String toString() {
                return "(" + row + ", " + col + ")";
            }
        }

        boolean hasAZero = false;

        var values = new HashSet<Integer>();

        var duplicate = new HashSet<Pair>();
        var empty = new HashSet<Pair>();

        for(int row = 0; row < array.length; row++) {
            if(array[row].length != array[0].length)  {
                LOGGER.error(
                    "Column with non valid length: {} (expected {})",
                    array[row].length, array[0].length
                    );
                break;
            }
            
            for (int col = 0; col < array[0].length; col++) {
                if(array[row][col] == 0) {
                    hasAZero = true;
                }
                if(array[row][col] < 0) {
                    empty.add(new Pair(row, col));
                }
                if(!values.add(array[row][col])) {
                    duplicate.add(new Pair(row, col));
                }
            }
        }

        if(!hasAZero) {
            LOGGER.error("No cell with a value of zero");
        }

        if(!duplicate.isEmpty()) {
            LOGGER.error("Duplicate cells: {}", duplicate);
        }
        
        if(!empty.isEmpty()) {
            LOGGER.error("Empty cells: {}", empty);
        }

        if(hasAZero && duplicate.isEmpty() && empty.isEmpty()) {
            return Optional.of(new Grid(array));
        }
        else {
            return Optional.empty();
        }
    }

    /**
     * Static factory method which returns an invalid {@code Grid} where all the fields in {@code self}
     * are set to {@code -1}
     * 
     * @param width the witdth of the grid
     * @param height the height of the grid
     * @return invalid {@code Grid} of dimensions {@code width} x {@code height}
     */
    public static Grid invalidOfSize(int width, int height) {

        int[][] t = new int[height][width];

        for(int i = 0; i < t.length; i++) {
            for(int j = 0; j < t[0].length; j++) {
                t[i][j] = -1;
            }
        }

        t[0][0] = 0;
        var g = new Grid(t);
        g.self[0][0] = -1;

        return g;
    }
    

    /**
     * Finds the coordinates of a number in {@link #self}.
     * 
     * @param toFind the number to find.
     * @return {@link Optional} of {@link Coordinates} of the number if it was found;
     * {@link Optional#empty()} otherwise.
     */
    private Optional<Coordinates> findCoordinates(int toFind) {
        for (int row = 0; row < self.length; row++) {
            for (int col = 0; col < self[0].length; col++) {
                if(self[row][col] == toFind) {
                    return Optional.of(new Coordinates(row, col));
                }
            }
        }
        
        return Optional.empty();
    }

    /**
     * Checks compatibility between this object and another {@code Grid}.
     * <p>
     * Compatibility is defined as such, for all valid grids g1 and g2:
     * <ul>
     * <li>g1 and g2 must have the same dimensions</li>
     * <li>g1 and g2 must share the same alphabet
     * (i.e all the ints which can be found in g1 are in g2 and the other way around)</li>
     * </ul>
     * @param g the {@code Grid} to check this object against
     * @return {@code true} if both grids are compatible; false otherwise
     */
    public boolean checkCompatibility(Grid g) {
        if(g == null) {
            LOGGER.error("Grid is null");
            return false;
        }
        
        if(self.length != g.self.length) {
            LOGGER.error("Different height: {}, {}", self.length, g.self.length);
            return false;
        }
        
        if(self[0].length != g.self[0].length) {
            LOGGER.error("Different width: {}, {}", self[0].length, g.self[0].length);
            return false;
        }
        
        boolean ret = true;
        
        Set<Integer> fromAll = new HashSet<>();
        Set<Integer> toAll = new HashSet<>();

        for(int[] ints : self) {
            for(int col = 0; col < self[0].length; col++) {
                fromAll.add(ints[col]);
                toAll.add(ints[col]);
            }
        }
        
        if(fromAll.contains(-1) || toAll.contains(-1)) {
            LOGGER.error("Invalid cells");
            return false;
        }
        
        var tmp = new HashSet<>(fromAll);
        
        fromAll.removeAll(toAll);
        toAll.removeAll(tmp);
        
        if(!fromAll.isEmpty()) {
            LOGGER.error("Missing numbers: {}", fromAll);
            ret = false;
        }
        
        if(!toAll.isEmpty()) {
            LOGGER.error("Missing numbers: {}", toAll);
            ret = false;
        }
        
        return ret;
    }


    /**
     * Getter for this object's 2d array {@code self}
     * 
     * @return a deep copy of {@code self}
     */
    public int[][] getCopyOfSelf() { 
        return Arrays.stream(self).map(int[]::clone).toArray(t -> self.clone());
    }

    /**
     * Getter for this object's {@code key}
     * 
     * @return {@code key}
     */
    public int getKey() { 
        return key; 
    }

    /**
     * Getter for this object's {@link #heuristicValue}
     *
     * @return {@link #heuristicValue}
     */
    public float getHeuristicValue() { 
        return heuristicValue; 
    }

    /**
     * Getter for a specific heuristic value.
     * 
     * @param d Distance for which to get the value.
     * @return The value for this distance.
     */
    public float getHeuristicValue(Distance d) {
        return distanceMap.get(d);
    }

    /**
     * Getter for this object's {@link #depth}.
     *
     * @return {@link #depth}
     */
    public int getDepth() { 
        return depth; 
    }

    /**
     * Getter for this object's parent {@link Grid}.
     *
     * @return {@link Grid} from which this object was generated
     */
    public Grid getParent() { 
        return parent; 
    }

    /**
     * Getter for this object's children {@link Grid}.
     *
     * @return Optional of a list of {@link Grid} which were created from calling {@link #generateNeighbors()}
     * on this object
     */
    public Optional<List<Grid>> getChildren() { 
        return Optional.ofNullable(hasGenerated); 
    }

    /**
     * Getter for this object's other neighbors {@link Grid}.
     *
     * @return Optional of a list of {@link Grid}} which would be valid results from calling {@link #generateNeighbors()}
     * on this object, but were later removed before being added to the {@link SearchSpace} containing 
     * this object
     */
    public Optional<List<Grid>> getPreExistingNeighbors() { 
        return Optional.ofNullable(existingNeighbors); 
    }


    /**
     * Sets a new value for this object's {@link #key}.
     * 
     * @param k new key value
     */
    void setKey(int k) { 
        key = k; 
    }

    /**
     * Sets a new value for this object's {@link #heuristicValue}.
     * 
     * @param v new heuristic value
     */
    public void setHeuristicValue(float v) { 
        heuristicValue = v; 
    }
    
    /**
     * Adds a neighbor to this object.
     * 
     * @param g {@code Grid} new neighbor to be added
     * @param children {@code boolean} which should be {@code true} if {@code g}
     * was in the return value from calling {@code generateNeighbors} on this object;
     * and {@code false} otherwise                               
     */
    void addNeighbor(Grid g, boolean children) {
        if(children){
            if(hasGenerated == null) {
                hasGenerated = new ArrayList<>();
            }
            hasGenerated.add(g);
        }
        else{
            if(existingNeighbors == null) {
                existingNeighbors = new ArrayList<>();
            }
            existingNeighbors.add(g);
        }
    }

    /**
     * Method which resets this object's neighbors.
     */
    public void resetNeighbors() {
        if(hasGenerated != null) {
            hasGenerated.clear();
        }
        if(existingNeighbors != null) {
            existingNeighbors.clear();
        }
    }

    /**
     * This method generates a {@link Set} of valid neighbor {@link Grid}s according to 
     * {@link Direction} validation ({@link Direction#check(Grid)}), except the parent grid.
     * <p>
     * Automatically filtering out the parent grid is done to ensure a search does not 'loop over
     * the same two states' forever. While this is likely not an issue in informed searches, this
     * can easily lead to two grids enqueueing each other over and over in the case of a non 
     * informed search.
     * <p>
     * For example, a DFS search where calling this method on this grid's parent would return a set
     * where this grid would be the last element and calling it on this grid would return a set 
     * where the last element is this grid's parent.
     * <p>
     * a {@link HashSet} is used to simulate randomness instead of always having
     * the neighbors ordered as from {@link Direction#values()}.
     *
     * @return {@link Set} of valid neighbors
     */
    Set<Grid> generateNeighbors() {

        var retour = new HashSet<Grid>();

        for (Direction d : Direction.values()) {
            if (d != parentDirection && d.check(this)) {
                retour.add(new Grid(this, d));
            }
        }

        return retour;
    }
    

    /**
     * The method called to compute the distance between this object and another {@link Grid}.
     * <p>
     * WARNING: no validation is done to ensure NPE and other exceptions won't happen.
     * 
     * @param g {@code Grid} target
     * @param d Which {@link Distance} to use
     * @return value of the distance
     */
    public float distanceTo(Grid g, Distance d){
        float i = d.calc(this, g);
        distanceMap.put(d, i);
        return i;
    }
    
    /*
      A brief note on all the lazy distance calculations from the parent grid:
      the tile that was moved is now (in this grid) where the blank tile was
      in the parent grid.
      I.e its coordinates in this grid are (parent.zero.row(), parent.zero.column()),
      and its coordinates in the parent grid are (this.zero.row(), this.zero.column()).
      
      We also do not need to check whether it is the blank tile, as it can not
      be the blank tile.
    */

    /**
     * The manhattan distance between a grid and another is the sum of the
     * manhattan distances of all the misplaced tiles.
     * <p>
     * The manhattan distance of a misplaced tile is the number of move
     * the tile would require in order to be in its correct place.
         * In other words, {@code (target x - current x) + (target y - current y}.
     * <p>
     * If the parent grid posseses a value for the manhattan distance,
     * then we simply get said value which is decreased by the manhattan distance
     * of the tile (with its coordinates in the parent grid (i.e {@link #zero}) that was 
     * exchanged with the blank tile; then we add the manhattan distance of the same tile 
     * but with its current coordinates (the parent's {@link #zero}), unless it is 
     * now correctly placed, in which case the decreased parent's distance is 
     * directly returned.
     * <p>
     * No checking is done to ensure the distance for this grid and its parent
     * are computed against the same target grid.
     * 
     * @param g Target.
     * @return Manhattan distance between this grid and g.
     */
    private float manhattan(Grid g) {
        float ret = 0f;
        Coordinates tmp;
        
        if(parent.distanceMap.get(Distance.MANHATTAN) != null) {
            ret = parent.distanceMap.get(Distance.MANHATTAN);
            
            // removes the manhattan distance of the moved tile
            tmp = g.findCoordinates(parent.self[zero.row()][zero.column()]).orElseThrow();
            ret -= (Math.abs(tmp.row() - zero.row()) + Math.abs(tmp.column() - zero.column()));
            
            // if it is still not correctly placed
            if(self[parent.zero.row()][parent.zero.column()] != g.self[parent.zero.row()][parent.zero.column()]) {
                // computes its new distance
                tmp = g.findCoordinates(self[parent.zero.row()][parent.zero.column()]).orElseThrow();
                ret += (Math.abs(tmp.row() - parent.zero.row()) + Math.abs(tmp.column() - parent.zero.column()));
            }
            return ret;
        }
        
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                if(self[row][col] != g.self[row][col] && self[row][col] != 0) {
                    tmp = g.findCoordinates(self[row][col]).orElseThrow();
                    ret += (Math.abs(tmp.row() - row) + Math.abs(tmp.column() - col));
                }
            }
        }
        
        return ret;
    }

    /**
     * Hamming distance aka 'tiles out of place'. It is an integer value equal to the
     * number of tiles which do no match the tiles from the target grid (excluding the blank tile).
     * <p>
     * If the parent grid posseses a value for the hamming distance,
     * then we simply check if the tile that was moved into the blank tile
     * is now in its correct position compared to the target grid {@code g}.
     * This leaves 3 possible cases:
     * <ul>
     *     <li>it was moved <u>into</u> its correct place -> parent's value minus 1</li>
     *     <li>it was moved <u>out of</u> its correct place -> parent's value plus 1</li>
     *     <li>neither -> paren'ts value</li>
     * </ul>
     * <p>
     * No checking is done to ensure the distance for this grid and its parent
     * are computed against the same target grid.
     * 
     * @param g Target.
     * @return Hamming distance between this grid and g.
     */
    private float hamming(Grid g) {
        float ret = 0f;
        
        if(parent.distanceMap.get(Distance.HAMMING) != null) {
            ret = parent.distanceMap.get(Distance.HAMMING);
            
            // if the tile that was moved into the blank space is now correctly placed
            if(self[parent.zero.row()][parent.zero.column()] == g.self[parent.zero.row()][parent.zero.column()]) {
                return ret - 1;
            }
            // if it was correctly placed but moved out of it
            else if(parent.self[zero.row()][zero.column()] == g.self[zero.row()][zero.column()]) {
                return ret + 1;
            }
            // if it wasn't correctly placed and still isn't
            else {
                return ret;
            }
        }
        
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                if(self[row][col] != g.self[row][col] && self[row][col] != 0) {
                    ret++;
                }
            }
        }

        return ret;
    }

    /**
     * Computes the sum of the euclidean distances between misplaced tiles 
     * (excluding the blank tile) and their respective goal positions, 
     * with the euclidean distance as defined in a R^2 plane with cartesian coordinates.
     * <p>
     * Here again we can calculate the grid's distance to the target by removing
     * the moved tile's distance and adding its new distance if it is incorrectly
     * placed, much like it is done in {@link #manhattan(Grid)}.
     * 
     * @param g Target.
     * @return Euclidean distance between this grid and g.
     */
    private float euclidean(Grid g) {
        float ret = 0f;
        Coordinates tmp;
        
        if(parent.distanceMap.get(Distance.EUCLIDEAN) != null) {
            ret = parent.distanceMap.get(Distance.EUCLIDEAN);

            // removes the euclidean distance of the moved tile
            tmp = g.findCoordinates(parent.self[zero.row()][zero.column()]).orElseThrow();
            ret -= (Math.abs(tmp.row() - zero.row()) + Math.abs(tmp.column() - zero.column()));

            // if it is still not correctly placed
            if(self[parent.zero.row()][parent.zero.column()] != g.self[parent.zero.row()][parent.zero.column()]) {
                // computes its new distance
                tmp = g.findCoordinates(self[parent.zero.row()][parent.zero.column()]).orElseThrow();
                ret += Math.floor(
                    Math.sqrt(
                        Math.pow(Math.abs(tmp.row() - parent.zero.row()), 2) +
                        Math.pow(Math.abs(tmp.column() - parent.zero.column()), 2)
                    )
                );
            }
            return ret;
        }
        
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                if(self[row][col] != g.self[row][col] && self[row][col] != 0) {
                    tmp = g.findCoordinates(self[row][col]).orElseThrow();
                    ret += Math.floor(
                        Math.sqrt(
                            Math.pow(Math.abs(tmp.row() - row), 2) +
                            Math.pow(Math.abs(tmp.column() - col), 2)
                        )
                    );
                }
            }
        }

        return ret;
    }
    
    /**
     * Private {@link Record} used to count linear conflicts in {@link #linearManhattan(Grid)} 
     * and {@link #totalConflicts(Conflict[][], Coordinates[][], boolean)}.
     * <p>
     * As conflicts go by pair of two ({@code a is in conflict with b 
     * <=> b is in conflict with a}), any reference to {@link #conflictsWith}
     * in a method can cause a {@link StackOverflowError}. As such, 
     * {@link Conflict#equals(Object)}, {@link Conflict#hashCode()}, 
     * {@link Conflict#compareTo(Conflict)}, {@link Conflict#toString()} and any others
     * should only manipulate {@link Conflict} instances by their {@link #coords} field.
     */
    private record Conflict(Coordinates coords, List<Conflict> conflictsWith)
        implements Comparable<Conflict> {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("(").append(Conflict.this.coords.row())
                .append(", ").append(Conflict.this.coords.column()).append(") [");

            for(var c : Conflict.this.conflictsWith) {
                sb.append("(").append(c.coords.row())
                    .append(", ").append(c.coords.column()).append("), ");
            }

            sb.append("]");

            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Conflict conflict = (Conflict) o;

            return coords.equals(conflict.coords);
        }

        @Override
        public int hashCode() {
            return coords.hashCode();
        }

        @Override
        public int compareTo(Conflict o) {
            return Integer.compare(Conflict.this.conflictsWith.size(), o.conflictsWith.size());
        }
    }
    
    /**
     * Linear conflicts heuristic. If two tiles which share their goal row/column also
     * happen to be in that same row/column and are inverted (i.e for the tiles to get
     * to their respective goal positions, one would have to 'pass over' the other),
     * then there is a linear conflict between these two tiles. 
     * <p>
     * The total number of conflicts is multiplied by 2 because one of the two tiles in
     * conflict has to leave the row/column and enter it again, which adds two moves.
     * <p>
     * This heuristic is added on top of a more generalistic heuristic, i.e the Manhattan distance.
     * <p>
     * For more information, see <u>Hansson, Mayer, Yung</u>
     * <i>Criticizing Solutions to Relaxed Models Yields Powerful Admissible Heuristics</i> (1992).
     * <p>
     * For lazy calculation of the linear conflicts, it can be noted that moving along a row
     * does not change any row based conflicts; in fact it only impacts the conflicts
     * in the previous column of the blank tile (which gained a normal tile) or the new one 
     * (which lost a normal tile). The same goes for moving along a column and the rows of the 
     * blank tile. So we would only need to check for the conflicts in these rows/columns, and 
     * add it to the conflicts of the other rows/columns.
     * <p>
     * Unfortunately, due to how complex calculating the conflicts is compared to the other heuristics,
     * it is not so easily reversible and that meansit would be required to keep an extra field 
     * (2d array or 2 arrays) which contains the number of conflicts of each row and column.
     * 
     * @param g Target.
     * @return {@code 2 *} the number of linear conflicts {@code +} {@link #manhattan(Grid)}.
     */
    /*
     Idk maybe its worth it /shrug. either way i left out a commented draft of a lazy calc
    */
    private float linearManhattan(Grid g) {

        /*
         by calling distanceTo instead of the enum or the private method,
         we also implicitly store the value in the enumMap so it will allow
         for lazy calc for this grid's children grids.
         
         Also this is called for for lazy calc of the conflicts if the parent
         grid has a linear conflicts heuristic value.
        */
        float ret = distanceTo(g, Distance.MANHATTAN);

        // coordinates in g.self of every tile from self
        Coordinates[][] goalCoords = new Coordinates[self.length][self[0].length];
        
        Conflict[][] rowConflicts = new Conflict[self.length][self[0].length];
        // colConflicts is transposed so it's easier to iterate over it
        Conflict[][] colConflicts = new Conflict[self[0].length][self.length];
        
        /*
        if(parent.distanceMap.get(Distance.LINEAR_MANHATTAN) != null) {
            //ret = parent.distanceMap.get(Distance.LINEAR_MANHATTAN);
            
            // should not be null as distanceTo(g, MANHATTAN) is the first call in this method
            // and we only get in this block if this method was called on the parent grid.
            float tmp2 = parent.distanceMap.get(Distance.LINEAR_MANHATTAN) - 
                parent.distanceMap.get(Distance.MANHATTAN);
            
            // if we moved from a row to another
            if(parentDirection == Direction.UP || parentDirection == Direction.DOWN) {
                for(int row = 0; row < self.length; row++) {
                    for(int col = 0; col < self[0].length; col++) {
                        // we only check columns
                        if(col == zero.column() || col == parent.zero.column()) {
                            var t = g.unsafeFindCoordinates(self[row][col]);
                            goalCoords[row][col] = new Coordinates(t[0], t[1]);
                        }
                    }
                }
                // and so here we would count potential conflicts, remove nulls and then call totalConflicts().
                // shouldn't have to worry about nulls and index out of bounds as everything is handled
                // by conflicts and coordinates objects + it uses for(var x : y) loops.
            }
            else {
                // and here do the same but we only check if (row = zero.row() || row = parent.zero.row())
            }
            
            return ret;
        }
        */
        
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                var t = g.findCoordinates(self[row][col]).orElseThrow();
                goalCoords[row][col] = new Coordinates(t.row(), t.column());
            }
        }
        
        // counts potential conflicts
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                // as always, ignore the blank tile
                if(self[row][col] != 0) {
                    // row based conflicts
                    if(goalCoords[row][col].row() == row) {
                        rowConflicts[row][col] = new Conflict(new Coordinates(row, col), new ArrayList<>());
                    }
                    // column based conflicts
                    if(goalCoords[row][col].column() == col) {
                        colConflicts[col][row] = new Conflict(new Coordinates(row, col), new ArrayList<>());
                    }
                }
            }
        }

        // removes all the null elements (i.e tiles which had no conflicts)
        rowConflicts = Arrays.stream(rowConflicts)
            .map(
                t -> Arrays.stream(t)
                    .filter(Objects::nonNull)
                    .toArray(Conflict[]::new)
            )
            .toArray(Conflict[][]::new);

        colConflicts = Arrays.stream(colConflicts)
            .map(
                t -> Arrays.stream(t)
                    .filter(Objects::nonNull)
                    .toArray(Conflict[]::new)
            )
            .toArray(Conflict[][]::new);
        
        /*
         idiot check: ret = this.MANHATTAN(g)
         so doing ret += totalConflicts() twice and then return 2 * ret
         returns 2 * MANHATTAN as well
        */
        ret += 2 * totalConflicts(rowConflicts, goalCoords, true);
        ret += 2 * totalConflicts(colConflicts, goalCoords, false);
        
        return ret;
    }


    /**
     * Helper method for {@link #linearManhattan(Grid)} which counts the total
     * of actual conflicts (either row or column based, not both) in a given
     * {@link Conflict} 2d array against a given goal array of {@link Coordinates}.
     * 
     * @param conflicts The conflicts to count.
     * @param goal The goal to check against for conflicts.
     * @param isRowConflict Whether {@code conflicts} should be treated as row based conflicts.
     * @return Total number of conflicts in {@code conflicts}.
     */
    private float totalConflicts(Conflict[][] conflicts, Coordinates[][] goal, boolean isRowConflict) {
        float totalConflicts = 0;
        LinkedList<Conflict> currentConflicts = new LinkedList<>();

        // for each row or col
        for(var tab : conflicts) {
            
            for(int el1 = 0; el1 < tab.length; el1++) {
                var clf1 = tab[el1];
                var coords1 = goal[clf1.coords().row()][clf1.coords().column()];
                // if it's a row based conflict, we get the column to check against other columns
                // and the other way around
                int goalPos1 = (isRowConflict) ? coords1.column() : coords1.row();

                // we check all the elements to the right
                for(int el2 = el1; el2 < tab.length; el2++) {
                    var clf2 = tab[el2];
                    var coords2 = goal[clf2.coords().row()][clf2.coords().column()];
                    int goalPos2 = (isRowConflict) ? coords2.column() : coords2.row();

                    // if an element on the right has a lower goal position, then there is a conflict
                    if(goalPos1 > goalPos2) {
                        tab[el1].conflictsWith().add(tab[el2]);
                        tab[el2].conflictsWith().add(tab[el1]);
                    }
                }
            }
            
            currentConflicts.clear();
            currentConflicts.addAll(Arrays.stream(tab).toList());
            // sorted from least conflicts to most conflicts
            Collections.sort(currentConflicts);
            
            currentConflicts.removeIf(c -> c.conflictsWith().isEmpty());
            
            // while there's still conflicts in the list
            while(!currentConflicts.isEmpty()) {

                // remove the most conflicting conflict and removes it from other conflicts
                var c = currentConflicts.pollLast();
                for(var c2 : currentConflicts) {
                    c2.conflictsWith().remove(c);

                }

                // + 1 total conflict
                totalConflicts += 1;

                // remove conflicts that no longer conflict
                currentConflicts.removeIf(c3 -> c3.conflictsWith().isEmpty());
            }
        }
        
        return totalConflicts;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Grid g) {
            return Arrays.deepEquals(self, g.self);
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(self);
    }

    /**
     * Compares two grid by their {@link #self} field.
     * 
     * @param target The grid this object should be compared to.
     * @return {@link Utils#intArrayDeepCompare(Object[], Object[])}.
     */
    @Override
    public int compareTo(Grid target) {
        return Utils.intArrayDeepCompare(self, target.self);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        return "{Grid " + key + " (" + heuristicValue + "): " + depth + " " + Arrays.deepToString(self) + "}";
    }

}
