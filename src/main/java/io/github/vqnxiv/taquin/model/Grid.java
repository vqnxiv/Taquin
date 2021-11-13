package io.github.vqnxiv.taquin.model;


import java.util.*;


public class Grid implements Comparable<Grid> {
    
    
    private enum Direction {
        LEFT    { public String toString() { return "Left"; } }, 
        RIGHT   { public String toString() { return "Right"; } }, 
        UP      { public String toString() { return "Up"; } }, 
        DOWN    { public String toString() { return "Down"; } }
    }

    public enum Distance {
        NONE                { public String toString() { return "none"; } },
        MANHATTAN           { public String toString() { return "manhattan"; } },
        HAMMING             { public String toString() { return "hamming"; } },
        EUCLIDEAN           { public String toString() { return "euclidean"; } },
        LINEAR_MANHATTAN    { public String toString() { return "linear conflicts"; } }
    }

    // todo: refactor as a comparator?
    public enum EqualPolicy {
        NONE            { public String toString() { return "None"; } },
        RANDOM          { public String toString() { return "Random"; } },
        NEWER_FIRST     { public String toString() { return "Newer"; } },
        OLDER_FIRST     { public String toString() { return "Older"; } },
        DEEPER_FIRST    { public String toString() { return "Deeper"; } },
        HIGHER_FIRST    { public String toString() { return "Higher"; } }
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
        self = Arrays.stream(content).map(int[]::clone).toArray($ -> content.clone());

        parent = null;
        parentDirection = null;
        depth = 0;
        equalPolicy = ep;
        
        int[] z = findCoordinates(0, true);
        zeroRow = z[0]; zeroCol = z[1];
    }

    // constructor called from neighbor generation
    private Grid(Grid from, Direction d) {
        self = Arrays.stream(from.self).map(int[]::clone).toArray($ -> from.self.clone());

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
            case RIGHT -> self[zeroRow][zeroCol] = self[zeroRow][++zeroCol];
            case UP -> self[zeroRow][zeroCol] = self[--zeroRow][zeroCol];
            case DOWN -> self[zeroRow][zeroCol] = self[++zeroRow][zeroCol];
        };
        self[zeroRow][zeroCol] = 0;
    }


    // ------

    private int[] findCoordinates(int toFind, boolean throwError) {
        for (int row = 0; row < self.length; row++)
            for (int col = 0; col < self[0].length; col++)
                if (self[row][col] == toFind) {
                    return new int[]{row, col};
                }

        if(throwError) throw new IllegalArgumentException(toFind + " not found");
        else return null;
    }
    
    public boolean hasSameAlphabet(Grid g) {
        if(self.length != g.self.length) {
            return false;
        }
        else if(self[0].length != g.self[0].length) {
            return false;
        }
        
        for(int[] ints : self) {
            for(int i : ints) {
                if(g.findCoordinates(i, false) == null) {
                    return false;
                }
            }
        }
        
        return true;
    }

    
    // ------

    public int[][] getSelf() { 
        //return self; 
        return Arrays.stream(self).map(int[]::clone).toArray($ -> self.clone());
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
        
        return switch(d) {
            case MANHATTAN -> manhattan(g);
            case HAMMING -> hamming(g);
            case EUCLIDEAN -> euclidean(g);
            case LINEAR_MANHATTAN -> linearManhattan(g);
            case NONE -> 0;
        };
    }
    
    private int manhattan(Grid g) {
        int retour = 0;
        
        int[] tmp;
        for(int row = 0; row < self.length; row++)
            for(int col = 0; col < self[0].length; col++)
                if(self[row][col] != g.self[row][col] && self[row][col] != 0){
                    tmp = g.findCoordinates(self[row][col], true);
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
                    tmp = g.findCoordinates(self[row][col], true);
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
        
        for (Direction d : new Direction[]{Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN}) {

            if (d != parentDirection) {
                switch (d) {
                    case LEFT -> {
                        if (zeroCol > 0) retour.add(new Grid(this, Direction.LEFT));
                    }
                    case RIGHT -> {
                        if (zeroCol < self[0].length - 1) retour.add(new Grid(this, Direction.RIGHT));
                    }
                    case UP -> {
                        if (zeroRow > 0) retour.add(new Grid(this, Direction.UP));
                    }
                    case DOWN -> {
                        if (zeroRow < self.length - 1) retour.add(new Grid(this, Direction.DOWN));
                    }
                }
            }
        }
        
        return retour;
    }


    // ------
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Grid c) {
            return Arrays.deepEquals(self, c.self);
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
            switch(equalPolicy) {
                case NONE -> {
                    return 0;
                }
                case RANDOM -> {
                    Random rnd = new Random();
                    return (rnd.nextBoolean()) ? -1 : 1;
                }
                case NEWER_FIRST -> {
                    return (key < target.key) ? -1 : 1;
                }
                case OLDER_FIRST -> {
                    return (key < target.key) ? 1 : -1;
                }
                case DEEPER_FIRST -> {
                    return (depth <= target.depth) ? 1 : - 1;
                }
                case HIGHER_FIRST -> {
                    return (depth <= target.depth) ? -1 : 1;
                }
            }
        } 
        
        return Integer.compare(heuristicValue, target.heuristicValue);
    }

    @Override
    public String toString(){
        
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(": ");
        //sb.append(String.format("%02d, %02d\n", depth, hashCode()));
        //sb.append(hashCodes.toString());
        for(var t : self)
            sb.append('\n').append(Arrays.toString(t));
        
        return sb.toString();
        
        //return "{Grid " + key + " (" + ((parent != null) ? parent.key : -2) + "): " + depth + " " + Arrays.deepToString(self) + "}";
        //return "[Grid] " + key + " (" + ((parent != null) ? parent.key : -2) + "):";
    }

}
