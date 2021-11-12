package io.github.vqnxiv.taquin.model;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;


public class SearchSpace {
    
    
    public static class Builder {
        private Grid start;
        private Grid end;
        private CollectionWrapper<?> explored;
        private CollectionWrapper<?> queued;
        
        public ObjectProperty<Grid.EqualPolicy> equalPolicy;
        
        public Builder() {
            equalPolicy = new SimpleObjectProperty<>(Grid.EqualPolicy.NEWER_FIRST);
        }
        
        public Builder start(Grid g) {
            start = g;
            return this;
        }
        
        public Builder end(Grid g) {
            end = g;
            return this;
        }
        
        public Builder explored(CollectionWrapper<?> cw) {
            explored = cw;
            return this;
        }
        
        public Builder queued(CollectionWrapper<?> cw) {
            queued = cw;
            return this;
        }
        
        
        public Grid getGrid(String s) {
            return switch(s.toLowerCase()) {
                case "start" -> start;
                case "end" -> end;
                default -> throw new IllegalArgumentException("Grid doesn't exist");
            };
        }
        
        public SearchSpace build() {
            if(start == null || end == null || explored == null || queued == null) {
                try {
                    throw new Exception("null fields");
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            
            return new SearchSpace(start, end, explored, queued, equalPolicy.get());
        }
    }
    
    
    // ------

    private final Grid startGrid;
    private final Grid goalGrid;
    private Grid currentGrid;

    private final CollectionWrapper<Grid> explored;
    private final CollectionWrapper<Grid> queued;

    private int currentKeyCounter = 0;


    // ------
    
    private SearchSpace(Grid start, Grid end, CollectionWrapper<?> explored, CollectionWrapper<?> queued, Grid.EqualPolicy ep) {

        startGrid = new Grid(start.getSelf(), ep);
        goalGrid = new Grid(end.getSelf(), ep);

        startGrid.setKey(0);
        goalGrid.setKey(-1);

        this.explored = (CollectionWrapper<Grid>) explored;
        this.queued = (CollectionWrapper<Grid>) queued;
    
        this.queued.add(startGrid);
        currentGrid = startGrid;

    }
    

    // ------

    public Grid getStart() { 
        return startGrid; 
    }

    public Grid getCurrent() { 
        return currentGrid; 
    }

    public Grid getGoal() { 
        return goalGrid; 
    }
    
    public Grid[] getGrids(){ 
        return new Grid[] { startGrid, currentGrid, goalGrid }; 
    }
    
    public CollectionWrapper<Grid> getExplored() { 
        return explored; 
    }
    
    public CollectionWrapper<Grid> getQueued() {
        return queued;
    }


    // ------

    public void setCurrent(Grid g) { 
        currentGrid = g; 
    }

    public boolean isCurrentGoal() { 
        return currentGrid.equals(goalGrid); 
    }

    public boolean isGoal(Grid g) { 
        return g.equals(goalGrid); 
    }
    
    public void pathFromStart() {
        var l = new ArrayList<Grid>();
        var g = currentGrid;
        
        while(!g.equals(startGrid)) {
            l.add(g);
            g = g.getParent();
        }

        // start
        l.add(g);
        
        l.sort(Comparator.comparingInt(Grid::getDepth));
        
        for(Grid g2 : l) {
            System.out.println(g2);
        }
    }
    
    
    // todo: refactor
    public LinkedList<Grid> getNewNeighbors(boolean filterExplored, boolean filtereQueued, boolean linkExisting){

        var possibleNewStates = currentGrid.generateNeighbors();
        var retour = new LinkedList<Grid>();

        for(Grid g : possibleNewStates){
            
            
            if((filterExplored && explored.contains(g)) || (filtereQueued && queued.contains(g))) {
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

        return retour;
    }

    private void linkExisting(Grid g) {
        boolean wasFound = false;

        for(Grid g2 : explored.asCollection()) {
            if(g.equals(g2)) {
                g = g2;
                wasFound = true;
            }
        }

        if(!wasFound) {
            for(Grid g2 : queued.asCollection()) {
                if(g.equals(g2)) {
                    g = g2;
                    wasFound = true;
                }
            }
        }

        if(wasFound) currentGrid.addNeighbor(g, false);
    }
    

    // ------

    @Override
    public String toString(){
        return "Start:\n" + startGrid.toString() + 
                "\nCurrent:\n" + currentGrid.toString() + 
                "\nSolved:\n" + goalGrid.toString();
    }

}