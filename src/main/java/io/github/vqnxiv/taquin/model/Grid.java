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
        LEFT    ((g -> g.zeroCol > 0)), 
        RIGHT   ((g -> g.zeroCol < g.self[0].length - 1)), 
        UP      ((g -> g.zeroRow > 0)),
        DOWN    ((g -> g.zeroRow < g.self.length - 1));

        /**
         * The function which determines whether the move is valid.
         */
        private final Function<Grid, Boolean> check;

        /**
         * Enum constructor
         * 
         * @param c validation function
         */
        Direction(Function<Grid, Boolean> c) {
            check = c;
        }

        /**
         * The method which is called when proceeding to validation
         * 
         * @param g the {@code Grid} to check
         * @return {@code true} if the move is valid, {@code false} otherwise
         */
        private boolean check(Grid g) {
            return check.apply(g);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.constantToReadable(this.name());
        }
    }

    /**
     * Enum which is used to compute the distances between two {@code Grid}s.
     * <p>
     * The grids are assumed to be compatible, i.e {@code g1} and {@code g2} such that {@code}
     * {@code g1.checkCompatibility(g2)} returns {@code true}.
     */
    // todo: move all the calc funcs in this enum
    public enum Distance {
        NONE                ((x, y) -> 0),
        MANHATTAN           ((x, y) -> x.manhattan(y)),
        HAMMING             ((x, y) -> x.hamming(y)),
        EUCLIDEAN           ((x, y) -> x.euclidean(y)),
        LINEAR_MANHATTAN    ((x, y) -> x.linearManhattan(y));

        /**
         * Function which is used to compute the distance
         */
        private final BiFunction<Grid, Grid, Integer> function;

        /**
         * Enum contstuctor
         * 
         * @param func the function called by {@code calc}
         */
        Distance(BiFunction<Grid, Grid, Integer> func) {
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
        private int calc(Grid g1, Grid g2) {
            return function.apply(g1, g2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.constantToReadable(this.name());
        }
    }

    /**
     * Enum used to decide between two {@code Grid}s which have the same {@code heuristicValue}.
     */
    public enum EqualPolicy {
        NONE            ((x, y) -> 0),
        RANDOM          ((x, y) -> (ThreadLocalRandom.current().nextBoolean()) ? 1 : -1),
        NEWER_FIRST     ((x, y) -> (x.key < y.key) ? -1 : 1),
        OLDER_FIRST     ((x, y) -> (x.key < y.key) ? 1 : -1),
        HIGHER_FIRST    ((x, y) -> (x.depth <= y.depth) ? -1 : 1),
        DEEPER_FIRST    ((x, y) -> (x.depth <= y.depth) ? 1 : - 1);

        /**
         * The function which is called by {@code calc}
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
         * with the same heuristic bvalue.
         * 
         * @param g1 {@code Grid}
         * @param g2 {@code Grid}
         * @return {@code 1} if {@code g1} is determined to be greather than {@code g2}; 
         * {@code -1} otherwise
         */
        private int calc(Grid g1, Grid g2) {
            return function.apply(g1, g2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.constantToReadable(this.name());
        }
    } 

    
    // ------

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * placeholder hash value
     */
    private static final int HASH_VALUE = 31;

    /**
     * The 2d array which represents the grid.
     * <p>
     * {@code 0} is used as the 'blank' value for the blank tile which is moved
     * when a {@code Search} is run.
     * <p>
     * Numbers must be positive and unique for the grid to be valid.
     */
    private final int[][] self;

    /**
     * The key is the order in which the {@code Grid}s were generated and validated within 
     * a {@code SpaceSearch}.
     * <p>
     * {@code -2} is an illegal placeholder value until the {@code Grid} gets validated
     * by the {@code SpaceSearch} it was generated from)
     * <p>
     * {@code -1} is the value reserved for pre-generated grids such as the start and the
     * end of a {@code SpaceSearch}
     * <p>
     * {@code 0} is the value for the first {@code Grid} generated and validated within a 
     * {@code SpaceSearch}
     */
    private int key = -2;

    /**
     * The depth is the number of steps between a {@code SpaceSearch}'s start {@code Grid} 
     * and this object.
     * <p>
     * i.e how many times you can call {@code g2 = g1.getParent().getParent().getParent()}
     * until {@code g2.equals(start)} returns {@code true} for that {@code SpaceSearch}
     */
    private final int depth;

    /**
     * The direction in which you can generate {@code parent}
     * <p>
     * It is the opposite of the direction used when {@code generateNeighbors} was called
     * on {@code parent} (e.g {@code LEFT} -> {@code RIGHT}).
     */
    private final Direction parentDirection;

    /**
     * The {@code Grid} this object was generated from ({@code generateNeighbors})
     */
    private final Grid parent;

    /**
     * The grids generated from calling {@code generateNeighbors} on this object
     * which were valided by the {@code SpaceSearch} this object belongs to
     */
    private Grid[] hasGenerated;

    /**
     * The grids generated from calling {@code generateNeighbors} on this object
     * but were not valided by the {@code SpaceSearch} this object belongs to
     */
    private Grid[] existingNeighbors;

    /**
     * The heuristic value for this object, as calculated by a {@code Search} {@code Distance}
     */
    private int heuristicValue;

    /**
     * Equal policy value used when calling {@code compareTo} on this object and
     * specifying as argument another {@code Grid} with the same {@code heuristicValue}
     */
    private final EqualPolicy equalPolicy;

    /**
     * The row index of the cell with a value of zero (considered the blank tile)
     */
    private int zeroRow;

    /**
     * The column index of the cell with a value of zero (considered the blank tile)
     */
    private int zeroCol;

    
    /**
     * Constructor which is called from the factory method {@code of}.
     * 
     * @param content the array which {@code self} will become a deep copy of
     * @param ep the value for {@code equalPolicy}
     */
    private Grid(int[][] content, EqualPolicy ep) {
        self = Arrays.stream(content).map(int[]::clone).toArray(t -> content.clone());

        parent = null;
        parentDirection = null;
        depth = 0;
        equalPolicy = ep;
        
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
        equalPolicy = from.equalPolicy;

        parentDirection = switch(d) {
            case RIGHT -> Direction.LEFT;
            case LEFT -> Direction.RIGHT;
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
        };
        
        zeroRow = from.zeroRow; zeroCol = from.zeroCol;

        self[zeroRow][zeroCol] = switch (d){
            case LEFT -> self[zeroRow][--zeroCol];
            case RIGHT -> self[zeroRow][++zeroCol];
            case UP -> self[--zeroRow][zeroCol];
            case DOWN -> self[++zeroRow][zeroCol];
        };
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
     * @param ep {@code EqualPolicy} of the {@code Grid} to be created
     * @return {@code Optional} of the created {@code Grid} if {@code array} was valid;
     * empty {@code Optional} otherwise
     */
    public static Optional<Grid> of(int[][] array, EqualPolicy ep) {
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
            return Optional.of(new Grid(array, ep));
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
        var g = new Grid(t, EqualPolicy.RANDOM);
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
     * Getter for this object's {@code heuristicValue}
     *
     * @return {@code heuristicValue}
     */
    public int getHeuristicValue() { 
        return heuristicValue; 
    }

    /**
     * Getter for this object's {@code depth}
     *
     * @return {@code depth}
     */
    public int getDepth() { 
        return depth; 
    }

    /**
     * Getter for this object's parent {@code Grid}
     *
     * @return {@code Grid} from which this object was generated
     */
    public Grid getParent() { 
        return parent; 
    }

    /**
     * Getter for this object's children {@code Grid}
     *
     * @return array of {@code Grid} which were created from calling {@code generateNeighbors}
     * on this object
     */
    public Grid[] getChildren() { 
        return hasGenerated; 
    }

    /**
     * Getter for this object's other neighbors {@code Grid}
     *
     * @return array of {@code Grid} which would be valid results from calling {@code generateNeighbors}
     * on this object, but were later removed before being added to the {@code SpaceSearch} containing 
     * this object
     */
    public Grid[] getPreExistingNeighbors() { 
        return existingNeighbors; 
    }


    /**
     * Sets a new value for this object's {@code key}
     * 
     * @param k new key value
     */
    void setKey(int k) { 
        key = k; 
    }

    /**
     * Sets a new value for this object's {@code heuristicValue}
     * 
     * @param v new heuristic value
     */
    public void setHeuristicValue(int v) { 
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
            if(hasGenerated == null) hasGenerated = new Grid[0];
            hasGenerated = Arrays.copyOf(hasGenerated, hasGenerated.length+1);
            hasGenerated[hasGenerated.length-1] = g;
        }
        else{
            if(existingNeighbors == null) existingNeighbors = new Grid[0];
            existingNeighbors = Arrays.copyOf(existingNeighbors, existingNeighbors.length+1);
            existingNeighbors[existingNeighbors.length-1] = g;
        }
    }

    /**
     * Method which resets this object's neighbors.
     */
    public void resetNeighbors() {
        hasGenerated = new Grid[]{};
        existingNeighbors = new Grid[]{};
    }
    

    /**
     * The method called to compute the distance between this object and another {@code Grid}
     * <p>
     * WARNING: no validation is done to ensure NPE and other errors won't happen.
     * 
     * @param g {@code Grid} target
     * @param d Which {@code Distance} to use
     * @return {@code int} value of the distance
     */
    public int distanceTo(Grid g, Distance d){
        return d.calc(this, g);
    }

    /**
     * 
     * @param g target
     * @return int
     */
    private int manhattan(Grid g) {
        int retour = 0;
        
        int[] tmp;
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
     *
     * @param g target
     * @return int
     */
    private int hamming(Grid g) {
        int retour = 0;
        
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
     *
     * @param g target
     * @return int
     */
    private int euclidean(Grid g) {
        int retour = 0;

        int[] tmp;
        for(int row = 0; row < self.length; row++) {
            for(int col = 0; col < self[0].length; col++) {
                if(self[row][col] != g.self[row][col] && self[row][col] != 0) {
                    tmp = g.unsafeFindCoordinates(self[row][col]);
                    retour += (int) Math.floor(Math.sqrt(Math.pow(Math.abs(tmp[0] - row), 2) + Math.pow(Math.abs(tmp[1] - col), 2)));
                }
            }
        }

        return retour;
    }

    /**
     *
     * @param g target
     * @return int
     */
    // todo: linear conflicts
    private int linearManhattan(Grid g) {
        return 0;
    }

    
    /**
     * This method generates a {@code Set} of valid neighbor {@code Grid}s 
     * according to {@code Direction} validation
     * <p>
     * a {@code HashSet} is used to simulate randomness instead of always having
     * the neighbors ordered as from {@code Direction#values}
     * 
     * @return {@code} Set of valid neighbors
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
        int result = 1;        
        int tmp = 1;
        
        for(var t : self) {
            for(var i : t) {
                tmp = HASH_VALUE * tmp + i;
            }
            result = HASH_VALUE * result + tmp;
            tmp = 1;
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares two {@code Grid}s with their {@code heuristicValue}. 
     * <p>
     * Calls this object's {@code equalPolicy} if the two grids have the same 
     * {@code heuristic value}
     * 
     * @param target the {@code Grid} this object should be compared to
     */
    @Override
    public int compareTo(Grid target){
        if(heuristicValue == target.getHeuristicValue()) {
            if(this.equals(target)) {
                return  0;
            }
            
            return equalPolicy.calc(this, target);
        } 
        
        return Integer.compare(heuristicValue, target.heuristicValue);
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
        
        return "{Grid " + key + " (" + ((parent != null) ? parent.key : -2) + "): " + depth + " " + Arrays.deepToString(self) + "}";
        //return "[Grid] " + key + " (" + ((parent != null) ? parent.key : -2) + "):";
    }

}
