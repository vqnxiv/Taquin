package io.github.vqnxiv.taquin.model.structure.jstructure;


import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.structure.Sortable;
import io.github.vqnxiv.taquin.model.structure.Unsorted;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * A {@link DataStructure} wrapper around {@link ArrayList}.
 *
 * @param <E> The type of elements.
 */
public class JArrayList<E extends Comparable<E>> implements DataStructure<E>, Sortable<E>, Unsorted<E> {

    /**
     * Last used comparator to sort the {@link ArrayList}.
     */
    private Comparator<? super E> comparator;

    /**
     * Whether {@link #internal} is currently sorted. By default,
     * we consider a new empty list to be sorted by its elements comparable order.
     */
    private boolean sorted = true;

    /**
     * Whether the last sort was done with {@link #comparator}.
     */
    private boolean lastSortWasComparator = false;

    /**
     * Whether {@link #comparator} sorts in ascending order.
     */
    private boolean ascendingComparator;

    /**
     * Internal {@link ArrayList}.
     */
    private final ArrayList<E> internal;


    /**
     * Defaults no args constructor which calls the {@link ArrayList} no args constructor.
     */
    public JArrayList() {
        internal = new ArrayList<>();
    }

    /**
     * Constructor with initial capacity.
     *
     * @param capacity The initial capacity for the internal {@link ArrayList}.
     */
    public JArrayList(int capacity) {
        internal = new ArrayList<>(capacity);
    }

    /**
     * Constructor with existing content.
     *
     * @param content {@link Collection} that will be passed to the {@link ArrayList} constructor.
     */
    public JArrayList(Collection<E> content) {
        internal = new ArrayList<>(content);
        sorted = isSorted();
    }


    /**
     * {@inheritDoc}
     *
     * @return The first element from {@link #internal}.
     */
    @Override
    public E peekFirst() {
        if(internal.isEmpty()) {
            return null;
        }
        
        return internal.get(0);
    }

    /**
     * {@inheritDoc}
     *
     * @return The first element from {@link #internal}.
     */
    @Override
    public E pollFirst() {
        if(internal.isEmpty()) {
            return null;
        }
        
        E e = internal.get(0);
        internal.remove(0);
        return e;
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from {@link #internal}.
     */
    @Override
    public E peekLast() {
        if(internal.isEmpty()) {
            return null;
        }
        
        return internal.get(internal.size() - 1);
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from {@link #internal}.
     */
    @Override
    public E pollLast() {
        if(internal.isEmpty()) {
            return null;
        }
        
        E e = internal.get(internal.size() - 1);
        internal.remove(internal.size() - 1);
        return e;
    }

    /**
     * {@inheritDoc}
     *
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    @Override
    public int indexOf(E e) {
        if(e == null) {
            return -1;
        }

        return internal.indexOf(e);
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
        return new JArrayList<>(internal);
    }

    /**
     * {@inheritDoc}
     *
     * @return The number of elements present in this structure.
     */
    @Override
    public int size() {
        return internal.size();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the internal {@link ArrayList} is empty.
     */
    @Override
    public boolean isEmpty() {
        return internal.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @param o The element to find.
     * @return {@code true} if such an element was found, {@code false} otherwise.
     */
    @Override
    public boolean contains(Object o) {
        return internal.contains(o);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Iterator} of the internal {@link ArrayList}.
     */
    @Override
    public Iterator<E> iterator() {
        return internal.iterator();
    }

    /**
     * {@inheritDoc}
     *
     * @param action The action to perform.
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        internal.forEach(action);
    }

    /**
     * {@inheritDoc}
     *
     * @return An array containing a copy of all elements from the internal {@link ArrayList}.
     */
    @Override
    public Object[] toArray() {
        return internal.toArray();
    }

    /**
     * {@inheritDoc}
     *
     * @param a Array whose class is that of the returned array; potentially the returned array itself.
     * @param <T> The type for the returned array.
     * @return Array of type {@code T} which contains all element from the internal {@link ArrayList}.
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return internal.toArray(a);
    }

    /**
     * {@inheritDoc}
     *
     * @param generator Function to allocate the array.
     * @param <T> Type of the returned array.
     * @return Array of type {@code T} which contains all element from the internal {@link ArrayList}.
     */
    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return internal.toArray(generator);
    }

    /**
     * Adds a non {@code null} element at the end of the internal {@link ArrayList}.
     *
     * @param e The element to add.
     * @return {@code false} if {@code e} is {@code null} or could not be added; otherwise, {@code true}.
     */
    @Override
    public boolean add(E e) {
        if(e == null) {
            return false;
        }
        
        if(sorted && !internal.isEmpty()) {
            if(lastSortWasComparator) {
                sorted = (ascendingComparator) ? 
                    // <= and >= because == respects both ascending and descending order
                    comparator.compare(peekLast(), e) <= 0 :
                    comparator.compare(peekLast(), e) >= 0;
            }
            else {
                sorted = peekLast().compareTo(e) <= 0;
            }
        }

        return internal.add(e);
    }

    /**
     * Removes an object from the internal {@link ArrayList}.
     *
     * @param o The object to remove.
     * @return {@code true} if it was successfully removed; {@code false} otherwise.
     */
    @Override
    public boolean remove(Object o) {
        return internal.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return internal.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        c.removeIf(Objects::isNull);
        
        boolean r = false;
        for(var v : c) {
            if(add(v)) {
                r = true;
            }
        }
        
        return r;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return internal.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return internal.removeIf(filter);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return internal.retainAll(c);
    }

    @Override
    public void clear() {
        internal.clear();
    }

    @Override
    public Spliterator<E> spliterator() {
        return internal.spliterator();
    }

    @Override
    public Stream<E> stream() {
        return internal.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return internal.parallelStream();
    }

    // TODO:
    @Override
    public boolean isSorted() {
        if(sorted) {
            return sorted;
        }
        
        var al = new ArrayList<>(internal);
        if(lastSortWasComparator) {
            al.sort(comparator);
        }
        else {
            al.sort(null);
        }
        
        return internal.equals(al);
    }

    /**
     * Sorts the internal {@link ArrayList} by its elements' natural order,
     * as defined by their {@link Comparable} implementation.
     */
    @Override
    public void sort() {
        internal.sort(null);
        lastSortWasComparator = false;
        sorted = true;
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
        
        internal.sort(c);
        
        lastSortWasComparator = true;
        comparator = c;
        sorted = true;
        ascendingComparator = peekFirst().compareTo(peekLast()) <= 0;
    }

    /**
     * Whether the last sort was performed using a {@link Comparator}.
     * 
     * @return {@code true} if {@link #comparator} was used during the last sorting
     * of the {{@link #internal}.
     */
    @Override
    public boolean isSortedByComparator() {
        return lastSortWasComparator;
    }

    /**
     * Getter for {@link #comparator}.
     * 
     * @return {@link #comparator}.
     */
    @Override
    public Optional<Comparator<? super E>> getLastComparator() {
        return Optional.ofNullable(comparator);
    }

    @Override
    public boolean addFirst(E e) {
        //return insert(e, 0);
        if(e == null) {
            return false;
        }

        if(sorted) {
            if(lastSortWasComparator) {
                sorted = (ascendingComparator) ?
                    // <= and >= because == respects both ascending and descending order
                    // inversed comparisons of .add() because here the previous first
                    // element will be after the new one, and not before
                    comparator.compare(peekFirst(), e) >= 0 :
                    comparator.compare(peekFirst(), e) <= 0;
            }
            else {
                sorted = peekFirst().compareTo(e) >= 0;
            }
        }
        
        internal.add(0, e);
        return true;
    }

    @Override
    public boolean addLast(E e) {
        return add(e);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean addAllFirst(DataStructure<E> toAdd) {
        boolean ret = false;
        
        var t = toAdd.toArray();
        for(int i = t.length - 1; i >= 0; i--) {
            if(addFirst((E) t[i])) {
                ret = true;
            }
        }
        
        return ret;
    }

    @Override
    public boolean addAllLast(DataStructure<E> toAdd) {
        toAdd.removeIf(Objects::isNull);

        if(toAdd.isEmpty()) {
            return false;
        }

        addAll(toAdd);

        return true;
    }

    /*
    @Override
    public boolean insert(E e, int index) {
        if(e == null) {
            return false;
        }

        internal.add(index, e);
        return true;
    }
    */
    // TODO
    /*
    @Override
    public boolean insertAll(DataStructure<E> toAdd, int index) {
        return false;
    }
    */
    /*
    @Override
    public boolean mergeWith(DataStructure<E> toAdd, Comparator<? super E> c) {
        toAdd.removeIf(Objects::isNull);

        if(toAdd.isEmpty()) {
            return false;
        }

        var tmp = new ArrayList<E>();

        while(!toAdd.isEmpty() && !internal.isEmpty()) {
            tmp.add(
                (c.compare(toAdd.peekFirst(), peekFirst()) > 0) ?
                    pollFirst() : toAdd.pollFirst()
            );
        }

        if(!toAdd.isEmpty()) tmp.addAll(toAdd);
        else if(!internal.isEmpty()) tmp.addAll(internal);

        internal.addAll(tmp);

        return true;
    }
    */

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        JArrayList<?> that = (JArrayList<?>) o;

        return Objects.equals(internal, that.internal);
    }

    @Override
    public int hashCode() {
        return internal != null ? internal.hashCode() : 0;
    }

    @Override
    public String toString() {
        return internal.toString();
    }
}
