package io.github.vqnxiv.taquin.model;


import javafx.beans.property.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class CollectionWrapper<E extends Comparable<E>>{
    
    
    public static class Builder {
        
        public ObjectProperty<Class<?>> subClass;
        public BooleanProperty initialCapacity;
        public IntegerProperty userInitialCapacity;
        
        public Builder(Class<?> c) {
            subClass = new SimpleObjectProperty<>(c);
            initialCapacity = new SimpleBooleanProperty(false);
            userInitialCapacity = new SimpleIntegerProperty(0);
        }
        
        public CollectionWrapper<?> build() {
            return new CollectionWrapper<>(this);
        }
    }

    
    // ------

    private Collection<E> self;

    private boolean naturalOrder;
    private boolean sort;
    private boolean initialCapacity;

    private static final Class<?>[] acceptedSubClasses = {
            ArrayDeque.class, ArrayList.class,
            LinkedHashSet.class, LinkedList.class,
            PriorityQueue.class
    };

    private static final Class<?>[] withInitialCapacity = {
            ArrayDeque.class, ArrayList.class,
            LinkedHashSet.class,
            PriorityQueue.class
    };

    private static final Class<?>[] withNaturalOrder = {
            PriorityQueue.class
    };


    // ------

    private CollectionWrapper(Builder builder) {
        

        if(builder.userInitialCapacity.get() > 0) {
            builder.initialCapacity.set(true);
        }
        
        if(builder.initialCapacity.get() && builder.userInitialCapacity.get() == 0) {
            builder.userInitialCapacity.set(100_000);
        }
        
        
        boolean initialized = false;
        
        if(builder.initialCapacity.get()) {
            initialized = initializeWithCapacity(builder.subClass.get(), builder.userInitialCapacity.get());
        }
        
        if(!initialized) {
            initialized = initialize(builder.subClass.get());
        }
        
        if(!initialized) {
            throw new IllegalArgumentException("Non accepted class: " + builder.subClass);
        }
        
        setProperties();
    }

    @SuppressWarnings("unchecked")
    private boolean initializeWithCapacity(Class<?> subClass, int capacity) {
        for(Class<?> c : withInitialCapacity) {
            if(subClass.equals(c)) {
                try {
                    // ^ later with good generification, e.g CollectionWrapper<C extends Collection<E>>
                    self = (Collection<E>) subClass.getDeclaredConstructor(int.class).newInstance(capacity);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean initialize(Class<?> subClass) {
        for(Class<?> c : acceptedSubClasses) {
            if(subClass.equals(c)) {
                try {
                    self = (Collection<E>) subClass.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        
        return false;
    }
    
    private void setProperties(){
        sort = self instanceof List<E>;
        naturalOrder = self instanceof TreeSet<E> || self instanceof PriorityQueue<E>;
        initialCapacity = !(self instanceof LinkedList<E> || self instanceof TreeSet<E>);
    }
    
    
    // ------
    
    public Collection<E> asCollection() { 
        return self; 
    }

    // Class<?> ?
    @SuppressWarnings("unchecked")
    public Class<? extends Collection<E>> getSubClass() {
        return (Class<? extends Collection<E>>) self.getClass();
    }

    public boolean usesNaturalOrdering() { 
        return naturalOrder; 
    }

    public boolean acceptsInitialCapacity() { 
        return initialCapacity; 
    }

    public boolean isSortable() { 
        return sort; 
    }
    
    public static Class<?>[] getAcceptedSubClasses() { 
        return acceptedSubClasses; 
    }


    // ------

    public static boolean doesClassAcceptInitialCapacity(Class<?> type) {
        for(Class<?> c : withInitialCapacity)
            if(type.equals(c)) return true;

        return false;
    }

    public static boolean doesClassUseNaturalOrder(Class<?> type) {
        for(Class<?> c : withNaturalOrder)
            if(type.equals(c)) return true;

        return false;
    }
    
    
    // ------

    public int size() { 
        return self.size(); 
    }

    public boolean isEmpty() { 
        return self.isEmpty(); 
    }

    public void clear() {
        self.clear();
    }
    
    public boolean contains(E elt) {
        return self.contains(elt);
    }

    public void add(E elt) {
        self.add(elt);
    } 

    public void addAll(Collection<E> toAdd) { 
        self.addAll(toAdd); 
    }
    
    public void sort() { 
        Collections.sort((List<E>) self); 
    }
    
    public void mergeWith(LinkedList<E> toAdd) {
        Collections.sort(toAdd);

        var tmp = new LinkedList<>(self);
        self.clear();
        
        while(!toAdd.isEmpty() && !tmp.isEmpty()) {
            self.add(
                // if toAdd.peekFirst() < tmp.peekFirst()
                (toAdd.peekFirst().compareTo(tmp.peekFirst()) > 0) ?
                tmp.pollFirst() : toAdd.pollFirst()       
            );
            //for(E e : self) System.out.println(e);
        }
        
        // mutually exclusive
        if(!toAdd.isEmpty()) self.addAll(toAdd);
        else if(!tmp.isEmpty()) self.addAll(tmp);
    }

    public E peekFirst() {
        return switch (self){
            // ArrayDeque, LinkedList, PriorityQueue
            case Queue<E> subQueue -> subQueue.peek();
            // TreeSet
            case SortedSet<E> subSSet -> subSSet.first();
            // LinkedHashSet
            case Set<E> subSet -> subSet.stream().findFirst().get();
            // ArrayList
            case List<E> subList -> subList.get(0);
            default -> throw new IllegalStateException("Unexpected value: " + self);
        };
    }

    public E peekLast() {
        return switch (self){
            // ArrayDeque, LinkedList
            case Deque<E> subQueue -> subQueue.peekLast();
            // TreeSet
            case SortedSet<E> subSSet -> subSSet.last();
            // ArrayList
            case List<E> subList -> subList.get(subList.size()-1);
            // PriorityQueue
            case Queue<E> subQueue -> subQueue.stream().skip(subQueue.size()-1).findFirst().get();
            // LinkedHashSet
            case Set<E> subSet -> subSet.stream().skip(subSet.size()-1).findFirst().get();
            default -> throw new IllegalStateException("Unexpected value: " + self);
        };
    }

    public E pollFirst() {
        E retour;

        switch (self) {
            // ArrayDeque, LinkedList, PriorityQueue
            case Queue<E> subQueue -> {
                retour = subQueue.poll();
            }
            // TreeSet
            case SortedSet<E> subSSet -> {
                retour = subSSet.first();
                subSSet.remove(retour);
            }
            // LinkedHashSet
            case Set<E> subSet -> {
                retour = subSet.stream().findFirst().get();
                subSet.remove(retour);
            }
            // ArrayList
            case List<E> subList -> {
                retour = subList.get(0);
                subList.remove(0);
            }
            default -> throw new IllegalStateException("Unexpected value: " + self);
        }

        return retour;
    }

    public E pollLast() {
        E retour;

        switch (self) {
            // ArrayDeque, LinkedList
            case Deque<E> subQueue -> {
                retour = subQueue.pollLast();
            }
            // TreeSet
            case SortedSet<E> subSSet -> {
                retour = subSSet.last();
                subSSet.remove(retour);
            }
            // ArrayList
            case List<E> subList -> {
                retour = subList.get(subList.size()-1);
                subList.remove(subList.size()-1);
            }
            // PriorityQueue
            case Queue<E> subQueue -> {
                retour = subQueue.stream().skip(subQueue.size()-1).findFirst().get();
                subQueue.remove(retour);
            }
            // LinkedHashSet
            case Set<E> subSet -> {
                retour = subSet.stream().skip(subSet.size()-1).findFirst().get();
                subSet.remove(retour);
            }
            default -> throw new IllegalStateException("Unexpected value: " + self);
        }

        return retour;
    }


    // ------
    
    @Override
    public String toString() {
        return self.toString();
    }
}
