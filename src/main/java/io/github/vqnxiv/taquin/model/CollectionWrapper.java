package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class CollectionWrapper<E extends Comparable<E>> {
    
    
    public static class Builder implements IBuilder {
        
        private final ObjectProperty<Class<?>> subClass;
        private final BooleanProperty initialCapacity;
        private final IntegerProperty userInitialCapacity;
        private Comparator<?> comparator;
        
        public Builder(String s, Class<?> c) {
            subClass = new SimpleObjectProperty<>(this, s + " class", c);
            initialCapacity = new SimpleBooleanProperty(this, s + " increase capacity", false);
            userInitialCapacity = new SimpleIntegerProperty(this, s + " capacity", 0);
        }
        
        public CollectionWrapper<?> build() {
            return new CollectionWrapper<>(this);
        }
        
        public Builder comparator(Comparator<?> comparator) {
            this.comparator = comparator;
            return this;
        }

        @Override
        public Map<String, Property<?>> getNamedProperties() {
            return Map.of(subClass.getName(), subClass);
        }

        @Override
        public EnumMap<Category, List<Property<?>>> getBatchProperties() {
            return new EnumMap<>(Map.of(
                IBuilder.Category.COLLECTION, 
                List.of(initialCapacity, userInitialCapacity)
            ));
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
        
        initialized = initializeWithComparator(builder.subClass.get(), builder.comparator);
        
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
    private boolean initializeWithComparator(Class<?> subClass, Comparator<?> comparator) {
        for(Class<?> c : withNaturalOrder) {
            if(subClass.equals(c)) {
                try {
                    self = (Collection<E>) subClass.getDeclaredConstructor(Comparator.class).newInstance(comparator);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        return false;
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
        naturalOrder = self instanceof PriorityQueue<E>;
        initialCapacity = !(self instanceof LinkedList<E>);
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
    
    public int indexOf(E elt) {
        if(self instanceof List<E> l) {
            return l.indexOf(elt);
        }
        int i = 0;
        for(E e : self) {
            if(e.equals(elt)) {
                return i;
            }
            i++;
        }
        return -1;
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
    
    public void sort(Comparator<? super E> c) {
        Collections.sort((List<E>) self, c);
    }
    
    public void mergeWith(Queue<E> toAdd) {
        var tmp = new LinkedList<>(self);
        self.clear();
        
        while(!toAdd.isEmpty() && !tmp.isEmpty()) {
            self.add(
                // if toAdd.peekFirst() < tmp.peekFirst()
                (toAdd.peek().compareTo(tmp.peekFirst()) > 0) ?
                tmp.pollFirst() : toAdd.poll()       
            );
        }
        
        // mutually exclusive
        if(!toAdd.isEmpty()) self.addAll(toAdd);
        else if(!tmp.isEmpty()) self.addAll(tmp);
    }

    public void mergeWith(Queue<E> toAdd, Comparator<? super E> c) {
        var tmp = new LinkedList<>(self);
        self.clear();

        while(!toAdd.isEmpty() && !tmp.isEmpty()) {
            self.add(
                (c.compare(toAdd.peek(), tmp.peekFirst()) > 0) ?
                    tmp.pollFirst() : toAdd.poll()
            );
        }
        
        if(!toAdd.isEmpty()) self.addAll(toAdd);
        else if(!tmp.isEmpty()) self.addAll(tmp);
    }
    
    public E peekFirst() {
        return switch (self){
            // ArrayDeque, LinkedList, PriorityQueue
            case Queue<E> subQueue -> subQueue.peek();
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
        return switch (self) {
            // ArrayDeque, LinkedList, PriorityQueue
            case Queue<E> subQueue -> subQueue.poll();
            // LinkedHashSet
            case Set<E> subSet -> {
                E e = subSet.stream().findFirst().get();
                subSet.remove(e);
                yield e;
            }
            // ArrayList
            case List<E> subList -> {
                E e = subList.get(0);
                subList.remove(0);
                yield e;
            }
            default -> throw new IllegalStateException("Unexpected value: " + self);
        };
    }

    public E pollLast() {
        return switch (self) {
            // ArrayDeque, LinkedList
            case Deque<E> subQueue -> subQueue.pollLast();
            // ArrayList
            case List<E> subList -> {
                E e = subList.get(subList.size()-1);
                subList.remove(subList.size()-1);
                yield e;
            }
            // PriorityQueue
            case Queue<E> subQueue -> {
                E e = subQueue.stream().skip(subQueue.size()-1).findFirst().get();
                subQueue.remove(e);
                yield e;
            }
            // LinkedHashSet
            case Set<E> subSet -> {
                E e = subSet.stream().skip(subSet.size()-1).findFirst().get();
                subSet.remove(e);
                yield e;
            }
            default -> throw new IllegalStateException("Unexpected value: " + self);
        };
    }


    // ------
    
    @Override
    public String toString() {
        return self.toString();
    }
}
