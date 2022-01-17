package io.github.vqnxiv.taquin.model.structure;


import io.github.vqnxiv.taquin.model.DataStructure;

import java.util.Comparator;
import java.util.Optional;


/**
 * An interface similar to {@link java.util.SortedSet} which indicates
 * that this {@link io.github.vqnxiv.taquin.model.DataStructure}
 * imposes a total ordering on its elements at all times; either by their 
 * {@link Comparable} implementation, or by a given {@link java.util.Comparator}.
 * <p>
 * Implementing classes should provide at least one constructor which takes
 * a {@link Comparator} argument.
 * <p>
 * See {@link Sortable#sort(Comparator)} doc about enforcing {@link Comparable} here.
 * 
 * @param <E> The type of elements.
 *
 * @see io.github.vqnxiv.taquin.model.DataStructure
 * @see Sortable
 * @see Unsorted
 */
public interface Sorted<E extends Comparable<E>> extends DataStructure<E> {

    /**
     * Whether this structure uses {@link Comparable} or a {@link  Comparator}.
     * 
     * @return {@code true} if this instance uses a {@link Comparator}, {@code false} otherwise.
     */
    boolean hasComparator();

    /**
     * Getter for this structure's {@link Comparator}, if it uses one.
     * 
     * @return {@link Optional#of(Object)} {@link Comparator} if it uses one, {@link Optional#empty()}
     * otherwise.
     */
    Optional<Comparator<? super E>> getComparator();
}
