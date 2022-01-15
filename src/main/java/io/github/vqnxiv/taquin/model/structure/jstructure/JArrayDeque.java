package io.github.vqnxiv.taquin.model.structure.jstructure;

import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.structure.Unsorted;

import java.util.*;


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
     * 
     * @return {@code true} if the element was successfully added; {@code false} otherwise.
     */
    @Override
    public boolean uAddFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true} if the element was successfully added; {@code false} otherwise.
     */
    @Override
    public boolean uAddLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true} if at least one element was successfully added; {@code false} otherwise.
     */
    @Override
    public boolean uAddAllFirst(Collection<E> toAdd) {
        List<E> l = new ArrayList<>(toAdd);
        Collections.reverse(l);
        
        for(var e : l) {
            addFirst(e);
        }
        
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true} if at least one element was successfully added; {@code false} otherwise.
     */
    @Override
    public boolean uAddAllLast(Collection<E> toAdd) {
        return addAll(toAdd);
    }
    
    
    /*
        ArrayDeque overrides
        add, addAll, push, offer, etc are not overriden as ArrayDeque
        already does not permit null elements.
     */

    /**
     * Apparently {@link Queue#equals(Object)} is not defined.
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof JArrayDeque<?> jad) {
            var itr1 = iterator();
            var itr2 = jad.iterator();
            
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
