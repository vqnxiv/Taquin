package io.github.vqnxiv.taquin.model.structure.jstructure;


import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.structure.Sortable;
import io.github.vqnxiv.taquin.model.structure.Unsorted;

import java.util.*;


/**
 * A {@link DataStructure} version of {@link ArrayList}.
 *
 * @param <E> The type of elements.
 */
public class JArrayList<E extends Comparable<E>> extends ArrayList<E> 
    implements DataStructure<E>, Sortable<E>, Unsorted<E> {


    /**
     * Last used comparator to sort this {@link ArrayList}.
     */
    private transient Comparator<? super E> comparator;

    /**
     * Whether this list is currently sorted. By default, we consider a new 
     * empty list to be sorted by its elements comparable order.
     */
    private transient boolean sorted = true;

    /**
     * Whether the last sort was done with {@link #comparator}.
     */
    private transient boolean lastSortWasComparator = false;

    /**
     * Whether {@link #comparator} sorts in ascending order.
     */
    private transient boolean ascendingComparator;


    /**
     * Defaults no args constructor which calls the {@link ArrayList} no args constructor.
     */
    public JArrayList() {
        super();
    }

    /**
     * Constructor with initial capacity.
     *
     * @param capacity The initial capacity for the internal {@link ArrayList}.
     */
    public JArrayList(int capacity) {
        super(capacity);
    }

    /**
     * Constructor with existing content.
     *
     * @param content {@link Collection} that will be passed to the {@link ArrayList} constructor.
     */
    public JArrayList(Collection<E> content) {
        super(content);
        sorted = isSorted();
    }


    /**
     * Compares two elements e1 and e2 according to this list's ordering.
     * 
     * @param e1 The first element to compare as in {@code e1.compareTo(e2)}.
     * @param e2 The second element to compare as in {@code e1.compareTo(e2)}.
     * @return {@code true} if e1 > e2; {@code false} otherwise.
     */
    private boolean compare(E e1, E e2) {
        if(lastSortWasComparator) {
            return (ascendingComparator) ?
                // <= and >= because == respects both ascending and descending order
                comparator.compare(e1, e2) <= 0 :
                comparator.compare(e1, e2) >= 0;
        }
        else {
            return e1.compareTo(e2) <= 0;
        }
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
        if(isEmpty()) {
            return null;
        }

        return get(0);
    }

    /**
     * {@inheritDoc}
     *
     * @return The first element from this list.
     */
    @Override
    public E dsPollFirst() {
        if(isEmpty()) {
            return null;
        }

        E e = get(0);
        remove(0);
        return e;
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from this list.
     */
    @Override
    public E dsPeekLast() {
        if(isEmpty()) {
            return null;
        }

        return get(size() - 1);
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from this list.
     */
    @Override
    public E dsPollLast() {
        if(isEmpty()) {
            return null;
        }

        E e = get(size() - 1);
        remove(size() - 1);
        return e;
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
        
        return indexOf(e);
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
     * @return New {@link JArrayList} with the same content as this object.
     */
    @Override
    public DataStructure<E> deepCopy() {
        return new JArrayList<>(this);
    }

    
    /*
        Sortable
     */
    
    /**
     * Checks whether this list is sorted and sets {@link #sorted} value accordingly.
     * 
     * @return {@code true} if this list is sorted, {@code false} otherwise.
     */
    @Override
    public boolean isSorted() {
        if(sorted) {
            return true;
        }

        var al = new JArrayList<>(this);
        if(lastSortWasComparator) {
            al.sort(comparator);
        }
        else {
            al.sort(null);
        }

        if(equals(al)) {
            sorted = true;
            return true;
        }
        
        return false;
    }


    /**
     * Sorts this list by its elements' natural order,
     * as defined by their {@link Comparable} implementation.
     */
    @Override
    public void sort() {
        super.sort(null);
        lastSortWasComparator = false;
        sorted = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSortedByComparator() {
        return lastSortWasComparator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Comparator<? super E>> getLastComparator() {
        return Optional.ofNullable(comparator);
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
        add(0, e);
        
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true} if the element was successfully added; {@code false} otherwise.
     */
    @Override
    public boolean uAddLast(E e) {
        return add(e);
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true} if at least one element was successfully added; {@code false} otherwise.
     */
    @Override
    public boolean uAddAllFirst(Collection<E> toAdd) {
        return addAll(0, toAdd);
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
        ArrayList overrides
     */

    /**
     * Non null requirement.
     * <p>
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e);

        // we only change sorted if the element is added
        boolean willBeSorted = sorted;
        
        if(sorted && !isEmpty()) {
            willBeSorted = compare(dsPeekLast(), e);

        }
        
        if(super.add(e)) {
            sorted = willBeSorted;
            return true;
        }
        
        return false;
    }

    /**
     * Non null requirement.
     * <p>
     *
     * {@inheritDoc}
     */
    @Override
    public void add(int index, E e) {
        Objects.requireNonNull(e);
        
        boolean willBeSorted = sorted;
        
        if(sorted && !isEmpty()) {
            if(index > 0 && index < size()) {
                willBeSorted = compare(get(index - 1), e)
                    && compare(e, get(index));
            } else if(index == 0) {
                willBeSorted = compare(e, dsPeekFirst());
            } else if(index == size()) {
                willBeSorted = compare(dsPeekLast(), e);
            }
        }
        
        super.add(index, e);
        sorted = willBeSorted;
    }

    /**
     * Non null requirement.
     * <p>
     *
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        try {
            c.removeIf(Objects::isNull);
        } catch(UnsupportedOperationException ignored) {
            // just ignore the error as unmodifiable collections cant have null anyway?
            // well, can't from List.of(e1, e2) etc
            // otherwise reflection with?
            // c instanceof ImmutableCollections.AbstractImmutableCollection<E>
        }
        return super.addAll(c);
    }

    /**
     * Non null requirement.
     * <p>
     *
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        try {
            c.removeIf(Objects::isNull);
        } catch(UnsupportedOperationException ignored) { 
            // keep the catch
        }
        
        return super.addAll(index, c);
    }

    /**
     * Sorts the internal {@link ArrayList} by the given {@link Comparator}'s order.
     * Defers to {@link #sort()} if the {@link Comparator} is {@code null}.
     *
     * @param c {@link Comparator} to sort the internal {@link ArrayList} with.
     */
    @Override
    public void sort(Comparator<? super E> c) {
        if(c == null) {
            sort();
        }

        super.sort(c);

        lastSortWasComparator = true;
        comparator = c;
        sorted = true;
        ascendingComparator = dsPeekFirst().compareTo(dsPeekLast()) <= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof JArrayList<?> jal) {
            return super.equals(jal);
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
