package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * This class models the game's grids with an int 2d array.
 * 
 * @see SearchSpace
 * @see io.github.vqnxiv.taquin.solver.Search
 */
public class Grid implements Comparable<Grid> {
    
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
            (g -> g.zeroCol > 0),
            (x, y) -> (new int[]{x, --y})
        ),
        /**
         * Right based movement, i.e increasing by one a pair of coordinates column index.
         */
        RIGHT(
            (g -> g.zeroCol < g.self[0].length - 1),
            (x, y) -> (new int[]{x, ++y})
        ),
        /**
         * Upward based movement, i.e decreasing by one a of pair coordinates row index.
         */
        UP(
            (g -> g.zeroRow > 0),
            (x, y) -> (new int[]{--x, y})
        ),
        /**
         * Downward based movement, i.e increasing by one a pair of coordinates row index.
         */
        DOWN(
            (g -> g.zeroRow < g.self.length - 1),
            (x, y) -> (new int[]{++x, y})
        );

        /**
         * The function which determines whether the move is valid.
         */
        private final Function<Grid, Boolean> check;

        /**
         * Function which translates a pair of coordinates according 
         * to the move's definition.
         */
        private final BiFunction<Integer, Integer, int[]> move;

        /**
         * Enum constructor
         * 
         * @param c validation function
         */
        Direction(Function<Grid, Boolean> c, BiFunction<Integer, Integer, int[]> m) {
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
         * @param row The first element of the coordinates.
         * @param col The second element.
         * @return An int array of length 2 which contains the translated [row, col].
         */
        private int[] move(int row, int col) {
            return move.apply(row, col);
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
        EUCLIDEAN0(
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
     * placeholder hash value
     */
    private static final int HASH_VALUE = 31;

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
     * The row index of the cell with a value of zero (considered the blank tile)
     */
    private final int zeroRow;

    /**
     * The column index of the cell with a value of zero (considered the blank tile)
     */
    private final int zeroCol;

    
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
        
        int[] z = safeFindCoordinates(0, true).orElse(new int[]{-1, -1});
        zeroRow = z[0]; zeroCol = z[1];
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

        int[] newZero = d.move(from.zeroRow, from.zeroCol);
        zeroRow = newZero[0]; zeroCol = newZero[1];

        self[from.zeroRow][from.zeroCol] = self[zeroRow][zeroCol];
        self[zeroRow][zeroCol] = 0;
    }

    /**
     * Static factory method which creates a {@code Grid} if the given 2d int array is found to be valid
     * <p>
     * A valid array/{@code Grid} is considered as such:
     * <ul>
     * <li>only positive numbers</li>
     * <li>no duplicates</li>
     * <li>must contain one cell with a value of zero (0)</li>
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
            LOGGER.error("Duplicate cells: " + duplicate);
        }
        
        if(!empty.isEmpty()) {
            LOGGER.error("Empty cells: " + empty);
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
     * Finds the coordinates of a number in {@code self}.
     * 
     * The method offers no protection against the absence of {@code toFind} and returns
     * {@code null} if it wasn't found.
     * 
     * {@see safeFindCoordinates}
     * 
     * @param toFind the int to find in {@code self}
     * @return an array with a length of 2 containing the coordinates of {@code toFind}
     * if it was found; {@code null} otherwise
     */
    private int[] unsafeFindCoordinates(int toFind) {
        for (int row = 0; row < self.length; row++)
            for (int col = 0; col < self[0].length; col++)
                if (self[row][col] == toFind)
                    return new int[]{row, col};

        return null;
    }

    /**
     * Finds the coordinates of a number in {@code self}.
     * 
     * @param toFind the int to find in {@code self}
     * @param throwError whether an Exception should be thrown if {@code toFind} wasn't found
     * @return {@code Optional} of an array of int containing the coordinates of {@code toFind}
     * if it was found; empty {@code Optional} otherwise
     * @throws IllegalArgumentException if {@code toFind} wasn't found 
     * and {@code throwError} is {@code true}
     */
    private Optional<int[]> safeFindCoordinates(int toFind, boolean throwError) throws IllegalArgumentException {
        for (int row = 0; row < self.length; row++)
            for (int col = 0; col < self[0].length; col++)
                if (self[row][col] == toFind)
                    return Optional.of(new int[]{row, col});

        if(throwError) {
            throw new IllegalArgumentException(toFind + " not found");
        }
        else return Optional.empty();
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
            LOGGER.error("Different height: " + self.length + ", " + g.self.length);
            return false;
        }
        
        if(self[0].length != g.self[0].length) {
            LOGGER.error("Different width: " + self[0].length + ", " + g.self[0].length);
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
        
        var tmp = new HashSet<>(fromAll);
        
        fromAll.removeAll(toAll);
        toAll.removeAll(tmp);
        
        if(!fromAll.isEmpty()) {
            LOGGER.error("Missing numbers: " + fromAll);
            ret = false;
        }
        
        if(!toAll.isEmpty()) {
            LOGGER.error("Missing numbers: " + toAll);
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
     * The method called to compute the distance between this object and another {@link Grid}.
     * <p>
     * WARNING: no validation is done to ensure NPE and other errors won't happen.
     * 
     * @param g {@code Grid} target
     * @param d Which {@link Distance} to use
     * @return value of the distance
     */
    public int distanceTo(Grid g, Distance d){
        float i = d.calc(this, g);
        distanceMap.put(d, i);
        return (int) i;
    }

    /**
     * The manhattan distance between a grid and another is the sum of the
     * manhattan distances of all the misplaced tiles.
     * <p>
     * The manhattan distance of a misplaced tile is the number of move
     * the tile would require in order to be in its correct place.
     * In other words, {@code (target x - current x) + (target y - current y}.
     * <p>
     * f the parent grid posseses a value for the manhattan distance,
     * then we simply get said value which is decreased by the manhattan distance
     * of the tile (with its coordinates in the parent grid aka {@link #zeroRow}, {@link #zeroCol}) 
     * that was exchanged with the blank tile; then we add the manhattan distance 
     * of the same tile but with its current coordinates (the parent's {@link #zeroRow}, {@link #zeroCol})
     * <p>
     * No checking is done to ensure the distance for this grid and its parent
     * are computed against the same target grid.
     * 
     * @param g Target.
     * @return Manhattan distance between this grid and g.
     */
    private float manhattan(Grid g) {
        int[] tmp;
        
        if(parent.distanceMap.get(Distance.MANHATTAN) != null) {
            float i = parent.distanceMap.get(Distance.MANHATTAN);
            
            tmp = g.unsafeFindCoordinates(parent.self[zeroRow][zeroCol]);
            i -= (Math.abs(tmp[0] - zeroRow) + Math.abs(tmp[1] - zeroCol));
            
            tmp = g.unsafeFindCoordinates(self[parent.zeroRow][parent.zeroCol]);
            i += (Math.abs(tmp[0] - parent.zeroRow) + Math.abs(tmp[1] - parent.zeroCol));
            
            return i;
        }
        
        int retour = 0;
        
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                if(self[row][col] != g.self[row][col] && self[row][col] != 0) {
                    tmp = g.unsafeFindCoordinates(self[row][col]);
                    retour += (Math.abs(tmp[0] - row) + Math.abs(tmp[1] - col));
                }
            }
        }
        
        return retour;
    }

    /**
     * Hamming distance aka 'tiles out of place'. It is an integer value equal to the
     * number of tiles which do no match the tiles from the target grid (excluding the blank tile).
     * <p>
     * If the parent grid posseses a value for the hamming distance,
     * then we simply check if the tile that was moved into the blank tile
     * is now in its correct position compareed to the target grid {@code g}.
     * If it is, then the parent's value minus 1 is returned; otherwise, 
     * simply the parent's value.
     * <p>
     * No checking is done to ensure the distance for this grid and its parent
     * are computed against the same target grid.
     * 
     * @param g Target.
     * @return Hamming distance between this grid and g.
     */
    private float hamming(Grid g) {
        if(parent.distanceMap.get(Distance.HAMMING) != null) {
            float i = parent.distanceMap.get(Distance.HAMMING);
            return (self[parent.zeroRow][parent.zeroCol] == g.self[parent.zeroRow][parent.zeroCol]) ?
                i - 1 : i;
        }
        
        float retour = 0;
        
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                if(self[row][col] != g.self[row][col] && self[row][col] != 0) {
                    retour++;
                }
            }
        }

        return retour;
    }

    /**
     * Computes the sum of the euclidean distances between misplaced tiles 
     * (excluding the blank tile) and their respective goal positions, 
     * with the euclidean distance as defined in a R^2 plane with cartesian coordinates.
     * 
     * @param g Target.
     * @return Euclidean distance between this grid and g.
     */
    private float euclidean(Grid g) {
        float retour = 0;

        int[] tmp;
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                if(self[row][col] != g.self[row][col] && self[row][col] != 0) {
                    tmp = g.unsafeFindCoordinates(self[row][col]);
                    retour += Math.floor(
                        Math.sqrt(
                            Math.pow(Math.abs(tmp[0] - row), 2) 
                            + Math.pow(Math.abs(tmp[1] - col), 2)
                        )
                    );
                }
            }
        }

        return retour;
    }

    /**
     *
     * @param g Target.
     * @return float
     */
    // todo: linear conflicts
    private float linearManhattan(Grid g) {
        return 0;
    }

    
    /**
     * This method generates a {@link Set} of valid neighbor {@link Grid}s 
     * according to {@link Direction} validation ({@link Direction#check(Grid)}).
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
     * @param target
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
        
        /*
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(": ");
        for(var t : self)
            sb.append('\n').append(Arrays.toString(t));
        
        return sb.toString();
        */
        
        // return "{Grid " + key + " (" + ((parent != null) ? parent.key : -2) + "): " + depth + " " + Arrays.deepToString(self) + "}";
        return "{Grid " + key + " (" + heuristicValue + "): " + depth + " " + Arrays.deepToString(self) + "}";
        //return "[Grid] " + key + " (" + ((parent != null) ? parent.key : -2) + "):";
    }

}
