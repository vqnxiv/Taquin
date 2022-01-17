package io.github.vqnxiv.taquin.model.structure.jstructure;


import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.structure.Sorted;

import java.util.*;


public class JPriorityQueue<E extends Comparable<E>> extends PriorityQueue<E> 
    implements DataStructure<E>, Sorted<E> {


    /**
     * Defaults no args constructor which calls the {@link ArrayDeque} no args constructor.
     */
    public JPriorityQueue() {
        super();
    }

    /**
     * Constructor with initial capacity.
     *
     * @param capacity Initial capacity.
     */
    public JPriorityQueue(int capacity) {
        super(capacity);
    }

    /**
     * Constructor with existing content.
     *
     * @param content {@link Collection} that will be passed to the {@link ArrayDeque} constructor.
     */
    public JPriorityQueue(Collection<E> content) {
        super(content);
    }

    /**
     * Constructor with {@link Comparator}.
     * 
     * @param comparator The comparator to use to order this queue's elements.
     */
    public JPriorityQueue(Comparator<? super E> comparator) {
        super(comparator);
    }

    /**
     * Constructor with capacity and comparator.
     * 
     * @param capacity Initial capacity.
     * @param comparator The comparator to use to order this queue's elements.
     */
    public JPriorityQueue(int capacity, Comparator<E> comparator) {
        super(capacity, comparator);
    }
    
    
    /*
        DataStructure
     */

    /**
     * {@inheritDoc}
     *
     * @return The first element from this list.
     */
    @Override
    public E dsPeekFirst() {
        return peek();
    }

    /**
     * {@inheritDoc}
     *
     * @return The first element from this list.
     */
    @Override
    public E dsPollFirst() {
        return poll();
    }

    /**
     * {@inheritDoc}
     *
     * @return The 'last' element from this list according to its sorting; ie the highest one.
     */
    @Override
    public E dsPeekLast() {
        var max = dsPeekFirst();
        
        if(comparator() == null) {
            for(var e : this) {
                if(e.compareTo(max) > 0) {
                    max = e;
                }
            }
        }
        else {
            var c = comparator();
            for(var e : this) {
                if(c.compare(e, max) > 0) {
                    max = e;
                }
            }
        }
        
        return max;
    }

    /**
     * {@inheritDoc}
     *
     * @return The 'last' element from this list according to its sorting; ie the highest one.
     */
    @Override
    public E dsPollLast() {
        var max = dsPeekFirst();

        if(comparator() == null) {
            for(var e : this) {
                if(e.compareTo(max) > 0) {
                    max = e;
                }
            }
        }
        else {
            var c = comparator();
            for(var e : this) {
                if(c.compare(e, max) > 0) {
                    max = e;
                }
            }
        }
        
        remove(max);
        return max;
    }

    /**
     * {@inheritDoc}
     *
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    @Override
    public int dsIndexOf(E e) {
        if(e == null) {
            return -1;
        }

        int i = 0;
        for(E elt : this) {
            if(e.equals(elt)) {
                return i;
            }
            i++;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true}.
     */
    @Override
    public boolean acceptsDuplicates() {
        return true;
    }

    @Override
    public DataStructure<E> deepCopy() {
        return new JPriorityQueue<>(this);
    }
    
    
    /*
        Sorted
     */
    
    @Override
    public boolean hasComparator() {
        return comparator() != null;
    }

    @Override
    public Optional<Comparator<? super E>> getComparator() {
        return Optional.ofNullable(comparator());
    }
    
    
    /*
        PriorityQueue overrides
        add is not overriden as PriorityQueue does not permit null elements.
     */
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof JPriorityQueue<?> jpq) {
            var itr1 = iterator();
            var itr2 = jpq.iterator();

            while(itr1.hasNext() && itr2.hasNext()) {
                if(!itr1.next().equals(itr2.next())) {
                    return false;
                }
            }

            return !(itr1.hasNext() || itr2.hasNext());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
