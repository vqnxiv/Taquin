package io.github.vqnxiv.taquin.model.structure;


import io.github.vqnxiv.taquin.model.DataStructure;

import java.util.Collection;


/**
 * Interface which indicates that this structure does not impose a total ordering on its
 * elements, and as such allows for new elements to be added at specific positions or indexes.
 * <p>
 * This is the opposite of {@link Sorted}; however it is not incompatible with {@link Sortable}
 * (e.g adding first may change whether this structure is currently sorted but not whether
 * it is sortable).
 * <p>
 * Method names start with {@code u*} to avoid clashing with {@link Collection} methods.
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
    boolean uAddFirst(E e);

    /**
     * Adds a new element in the last position of this structure.
     *
     * @param e The element to add.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    boolean uAddLast(E e);

    /**
     * Adds new elements in the first positions of this structure, with the same order
     * as in {@code toAdd}.
     *
     * @param toAdd The elements to add.
     * @return {@code true} if at least one element was successfully added, {@code false} otherwise.
     */
    boolean uAddAllFirst(Collection<E> toAdd);

    /**
     * Adds new elements in the last positions of this structure, with the same order
     * as in {@code toAdd}.
     *
     * @param toAdd The elements to add.
     * @return {@code true} if at least one element was successfully added, {@code false} otherwise.
     */
    boolean uAddAllLast(Collection<E> toAdd);
}
