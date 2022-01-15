package io.github.vqnxiv.taquin.model.structure.jstructure;

import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.structure.Unsorted;

import java.util.ArrayDeque;
import java.util.Collection;


/**
 * A {@link DataStructure} version of {@link ArrayDeque}.
 *
 * @param <E> The type of elements.
 */
public class JArrayDeque<E> extends ArrayDeque<E> 
    implements DataStructure<E>, Unsorted<E> {


    /**
     * Defaults no args constructor which calls the {@link ArrayDeque} no args constructor.
     */
    public JArrayDeque() {
        super();
    }

    /**
     * Constructor with initial capacity.
     *
     * @param capacity Useless param.
     */
    public JArrayDeque(int capacity) {
        super(capacity);
    }

    /**
     * Constructor with existing content.
     *
     * @param content {@link Collection} that will be passed to the {@link ArrayDeque} constructor.
     */
    public JArrayDeque(Collection<E> content) {
        super(content);
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
        return peekFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @return The first element from this list.
     */
    @Override
    public E dsPollFirst() {
        return pollFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from this list.
     */
    @Override
    public E dsPeekLast() {
        return peekLast();
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from this list.
     */
    @Override
    public E dsPollLast() {
        return pollLast();
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

    /**
     * {@inheritDoc}
     *
     * @return New {@link JLinkedList} with the same content as this object.
     */
    @Override
    public DataStructure<E> deepCopy() {
        return new JArrayDeque<>(this);
    }
    

    /*
        Unsorted
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uAddFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uAddLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uAddAllFirst(Collection<E> toAdd) {
        for(var e : toAdd) {
            addFirst(e);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uAddAllLast(Collection<E> toAdd) {
        for(var e : toAdd) {
            addLast(e);
        }
        return true;
    }
    
    
    /*
        ArrayDeque overrides
        add, addAll, push, offer, etc are not overriden as ArrayDeque
        already does not permit null elements.
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof JArrayDeque<?> jad) {
            return super.equals(jad);
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
