package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.controller.BuilderController;
import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.*;



public class SearchSpace {
    
    
    public static class Builder implements IBuilder {
        private CollectionWrapper<?> explored;
        private CollectionWrapper<?> queued;
        
        private final ObjectProperty<Grid.EqualPolicy> equalPolicy;
        private final ObjectProperty<Grid> start;
        private final ObjectProperty<Grid> end;
        
        // todo: Grid.empty()
        // todo: immutable Grid
        public Builder() {
            equalPolicy = new SimpleObjectProperty<>(this, "equal policy", Grid.EqualPolicy.NONE);
            start = new SimpleObjectProperty<>(this, "start", Grid.from(new int[1][1], Grid.EqualPolicy.RANDOM).get());
            end = new SimpleObjectProperty<>(this, "end", Grid.from(new int[1][1], Grid.EqualPolicy.RANDOM).get());
        }
        
        public Builder explored(CollectionWrapper<?> cw) {
            explored = cw;
            return this;
        }
        
        public Builder queued(CollectionWrapper<?> cw) {
            queued = cw;
            return this;
        }

        public SearchSpace build() {
            if(start.get() == null || end.get() == null || explored == null || queued == null) {
                try {
                    throw new NullPointerException("null fields");
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            
            return new SearchSpace(start.getValue(), end.getValue(), explored, queued, equalPolicy.get());
        }

        @Override
        public Map<String, Property<?>> getNamedProperties() {
            return Map.of(
                start.getName(), start,
                end.getName(), end
            );
        }

        @Override
        public EnumMap<BuilderController.TabPaneItem, List<Property<?>>> getBatchProperties() {
            return new EnumMap<>(Map.of(
                BuilderController.TabPaneItem.SEARCH_MAIN, List.of(equalPolicy) 
            ));
        }
    }
    
    
    // ------

    private final Grid startGrid;
    private final Grid goalGrid;
    private Grid currentGrid;

    private final CollectionWrapper<Grid> explored;
    private final CollectionWrapper<Grid> queued;

    private int currentKeyCounter = 0;
    
    public Property<Grid> currentGridProperty;


    // ------
    
    @SuppressWarnings("unchecked")
    private SearchSpace(Grid start, Grid end, CollectionWrapper<?> explored, CollectionWrapper<?> queued, Grid.EqualPolicy ep) {

        // startGrid = new Grid(start.getCopyOfSelf(), ep);
        // goalGrid = new Grid(end.getCopyOfSelf(), ep);
        startGrid = Grid.from(start.getCopyOfSelf(), ep).orElse(start);
        goalGrid = Grid.from(end.getCopyOfSelf(), ep).orElse(end);

        startGrid.setKey(0);
        goalGrid.setKey(-1);

        this.explored = (CollectionWrapper<Grid>) explored;
        this.queued = (CollectionWrapper<Grid>) queued;
    
        this.queued.add(startGrid);
        currentGrid = startGrid;

        currentGridProperty = new SimpleObjectProperty<>(currentGrid);
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
        currentGridProperty.setValue(g);
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
    
    //public LinkedList<Grid> getNewNeighbors(boolean filterExplored, boolean filterQueued, boolean linkExisting){
    @SuppressWarnings("unchecked")
    public <T extends Queue<Grid> & List<Grid>> T getNewNeighbors(boolean filterExplored, boolean filterQueued, boolean linkExisting){

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