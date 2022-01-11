package io.github.vqnxiv.taquin.model.structure;


import io.github.vqnxiv.taquin.model.DataStructure;

import java.util.Comparator;
import java.util.Objects;


/**
 * Interface which indicates that this structure does not impose a total ordering on its
 * elements, and as such allows for new elements to be added at specific positions or indexes.
 * <p>
 * This is the opposite of {@link Sorted}; however it is not incompatble with {@link Sortable}
 * (e.g adding first may change whether this structure is currently sorted but not whether
 * it is sortable).
 * 
 * @param <E> The type of elements.
 *           
 * @see DataStructure
 * @see Sorted
 * @see Sortable
 */
public interface Unsorted<E> extends DataStructure<E> {

    /**
     * Adds a new element in the first position of this structure.
     * 
     * @param e The element to add.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    boolean addFirst(E e);

    /**
     * Adds a new element in the last position of this structure.
     *
     * @param e The element to add.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    boolean addLast(E e);

    /**
     * Adds new elements in the first positions of this structure, with the same order
     * as in {@code toAdd}.
     *
     * @param toAdd The elements to add.
     * @return {@code true} if at least one element was successfully added, {@code false} otherwise.
     */
    boolean addAllFirst(DataStructure<E> toAdd);

    /**
     * Adds new elements in the last positions of this structure, with the same order
     * as in {@code toAdd}.
     *
     * @param toAdd The elements to add.
     * @return {@code true} if at least one element was successfully added, {@code false} otherwise.
     */
    boolean addAllLast(DataStructure<E> toAdd);


    /**
     * Adds a new element at a specific given position of this structure.
     *
     * @param e The element to add.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    // boolean insert(E e, int index);

    /**
     * Adds new elements at a specific given position of this structure, with the same order
     * as in {@code toAdd}.
     *
     * @param toAdd The elements to add.
     * @return {@code true} if at least one element was successfully added, {@code false} otherwise.
     */
    // boolean insertAll(DataStructure<E> toAdd, int index);

    /**
     * Merges this structure with another, with ordering dictated by the given comparator.
     * 
     * @param toAdd The {@link DataStructure} to add to this one.
     * @param c The {@link Comparator} which decides the order in which elements are added.
     * @return {@code true} if at least one element was successfully added, {@code false} otherwise.
     */
    // todo one with Sorted<E> and the other with (Sortable<E> && isSorted())
    // actually should this be in sorted??
    // idk, maybe completely remove it
    /*
    default boolean mergeWith(DataStructure<E> toAdd, Comparator<? super E> c) {
        toAdd.removeIf(Objects::isNull);

        if(toAdd.isEmpty()) {
            return false;
        }

        var tmp = this.deepCopy();
        clear();

        while(!toAdd.isEmpty() && !tmp.isEmpty()) {
            add(
                (c.compare(toAdd.peekFirst(), tmp.peekFirst()) > 0) ?
                    tmp.pollFirst() : toAdd.pollFirst()
            );
        }

        if(!toAdd.isEmpty()) addAll(toAdd);
        else if(!tmp.isEmpty()) addAll(tmp);

        return true;
    }
    */
}
