package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.util.Utils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;


public class Grid implements Comparable<Grid> {
    

    private enum Direction {
        LEFT    ((g -> g.zeroCol > 0)), 
        RIGHT   ((g -> g.zeroCol < g.self[0].length - 1)), 
        UP      ((g -> g.zeroRow > 0)),
        DOWN    ((g -> g.zeroRow < g.self.length - 1));
        
        private final Function<Grid, Boolean> check;
        
        Direction(Function<Grid, Boolean> c) {
            check = c;
        }

        private boolean check(Grid g) {
            return check.apply(g);
        }
        
        @Override
        public String toString() {
            return Utils.constantToReadable(this.name());
        }
    }

    public enum Distance {
        NONE                ((x, y) -> 0),
        MANHATTAN           ((x, y) -> x.manhattan(y)),
        HAMMING             ((x, y) -> x.hamming(y)),
        EUCLIDEAN           ((x, y) -> x.euclidean(y)),
        LINEAR_MANHATTAN    ((x, y) -> x.linearManhattan(y));
        
        private final BiFunction<Grid, Grid, Integer> function;
        
        Distance(BiFunction<Grid, Grid, Integer> func) {
            function = func;
        }
        
        private int calc(Grid g1, Grid g2) {
            return function.apply(g1, g2);
        }
        
        @Override
        public String toString() {
            return Utils.constantToReadable(this.name());
        }
    }

    public enum EqualPolicy {
        NONE            ((x, y) -> 0),
        RANDOM          ((x, y) -> (ThreadLocalRandom.current().nextBoolean()) ? 1 : -1),
        NEWER_FIRST     ((x, y) -> (x.key < y.key) ? -1 : 1),
        OLDER_FIRST     ((x, y) -> (x.key < y.key) ? 1 : -1),
        HIGHER_FIRST    ((x, y) -> (x.depth <= y.depth) ? -1 : 1),
        DEEPER_FIRST    ((x, y) -> (x.depth <= y.depth) ? 1 : - 1);

        private final BiFunction<Grid, Grid, Integer> function;

        EqualPolicy(BiFunction<Grid, Grid, Integer> func) {
            function = func;
        }

        private int calc(Grid g1, Grid g2) {
            return function.apply(g1, g2);
        }
        
        @Override
        public String toString() {
            return Utils.constantToReadable(this.name());
        }
    } 

    
    // ------

    private static final int HASH_VALUE = 31;

    private final int[][] self;
    private int key = -2;
    
    private final int depth;

    private final Direction parentDirection;
    private final Grid parent;

    private Grid[] hasGenerated;
    private Grid[] existingNeighbors;
    
    private int heuristicValue;
    private final EqualPolicy equalPolicy;
    
    private int zeroRow;
    private int zeroCol;
    
    
    // ------

    // constructor called from SearchSpace/GridViewer creation
    public Grid(int[][] content, EqualPolicy ep) {
        self = Arrays.stream(content).map(int[]::clone).toArray(t -> content.clone());

        parent = null;
        parentDirection = null;
        depth = 0;
        equalPolicy = ep;
        
        int[] z = safeFindCoordinates(0, true).orElse(new int[]{-1, -1});
        zeroRow = z[0]; zeroCol = z[1];
    }

    // constructor called from neighbor generation
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


    // ------
    
    private int[] unsafeFindCoordinates(int toFind) {
        for (int row = 0; row < self.length; row++)
            for (int col = 0; col < self[0].length; col++)
                if (self[row][col] == toFind)
                    return new int[]{row, col};

        throw new IllegalArgumentException(toFind + " not found");
    }
    
    private Optional<int[]> safeFindCoordinates(int toFind, boolean throwError) {
        for (int row = 0; row < self.length; row++)
            for (int col = 0; col < self[0].length; col++)
                if (self[row][col] == toFind)
                    return Optional.of(new int[]{row, col});

        if(throwError) throw new IllegalArgumentException(toFind + " not found");
        else return Optional.empty();
    }
    
    
    public boolean hasSameAlphabet(Grid g) {
        if(self.length != g.self.length) {
            System.out.println("different height");
            return false;
        }
        else if(self[0].length != g.self[0].length) {
            System.out.println("different width");
            return false;
        }
        
        for(int[] ints : self) {
            for(int i : ints) {
                if(g.safeFindCoordinates(i, false).isEmpty()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    
    // ------

    public int[][] getSelf() { 
        return self; 
        // return Arrays.stream(self).map(int[]::clone).toArray($ -> self.clone());
    }

    public int getKey() { 
        return key; 
    }

    public int getHeuristicValue() { 
        return heuristicValue; 
    }

    public int getDepth() { 
        return depth; 
    }

    public Grid getParent() { 
        return parent; 
    }
    
    public Grid[] getChildren() { 
        return hasGenerated; 
    }

    public Grid[] getPreExistingNeighbors() { 
        return existingNeighbors; 
    }


    // ------
    
    void setKey(int k) { 
        key = k; 
    }
    
    public void setHeuristicValue(int v) { 
        heuristicValue = v; 
    }
    
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

    public void resetNeighbors() {
        hasGenerated = new Grid[]{};
    }
    

    // ------

    // todo: linear conflicts
    public int distanceTo(Grid g, Distance d){
        return d.calc(this, g);
    }
    
    /* WARNING:
     * these use unsafeFindCoordinates()
     */
    private int manhattan(Grid g) {
        int retour = 0;
        
        int[] tmp;
        for(int row = 0; row < self.length; row++)
            for(int col = 0; col < self[0].length; col++)
                if(self[row][col] != g.self[row][col] && self[row][col] != 0){
                    tmp = g.unsafeFindCoordinates(self[row][col]);
                    retour += (Math.abs(tmp[0] - row) + Math.abs(tmp[1] - col));
                }
        
        return retour;
    }
    
    private int hamming(Grid g) {
        int retour = 0;
        
        for(int row = 0; row < self.length; row++)
            for(int col = 0; col < self[0].length; col++)
                if(self[row][col] != g.self[row][col] && self[row][col] != 0)
                    retour++;

        return retour;
    }
    
    private int euclidean(Grid g) {
        int retour = 0;

        int[] tmp;
        for(int row = 0; row < self.length; row++)
            for(int col = 0; col < self[0].length; col++)
                if(self[row][col] != g.self[row][col] && self[row][col] != 0){
                    tmp = g.unsafeFindCoordinates(self[row][col]);
                    retour += (int) Math.floor(Math.sqrt(Math.pow(Math.abs(tmp[0] - row), 2) + Math.pow(Math.abs(tmp[1] - col), 2)));
                }

        return retour;
    }
    
    private int linearManhattan(Grid g) {
        return 0;
    }
    
    // hashset for the 'randomness' instead of always left, right, up, down
    HashSet<Grid> generateNeighbors() {

        var retour = new HashSet<Grid>();
        
        for (Direction d : Direction.values()) {
            if (d != parentDirection && d.check(this)) {
                retour.add(new Grid(this, d));
            }
        }
        
        return retour;
    }


    // ------
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Grid g) {
            return Arrays.deepEquals(self, g.self);
        }
        
        return false;
    }
    
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
