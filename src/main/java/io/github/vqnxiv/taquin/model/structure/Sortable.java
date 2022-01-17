package io.github.vqnxiv.taquin.model.structure;


import io.github.vqnxiv.taquin.model.DataStructure;

import java.util.Comparator;
import java.util.Optional;


/**
 * An interface which indicates that this {@link io.github.vqnxiv.taquin.model.DataStructure}
 * supports sorting operations (but may not necessarily be sorted at all times).
 *
 * @param <E> The type of elements.
 *
 * @see io.github.vqnxiv.taquin.model.DataStructure
 * @see Sorted
 * @see Unsorted
 */
public interface Sortable<E extends Comparable<E>> extends DataStructure<E> {

    /**
     * Method which indicates if the implementing {@link io.github.vqnxiv.taquin.model.DataStructure}
     * is currently sorted.
     * <p>
     * One way is for the implementing class to keep a field which is set to {@code true}
     * on every {@link #sort()} or {@link #sort(Comparator)} call and potentially
     * set to {@code false} when an element is added. It is also possible to call
     * {@link #sort()} but it may be a hassle to check for {@link #sort(Comparator)}
     * (maybe keep the last used {@link Comparator} in a field?).
     * 
     * @return {@code true} if the this structure is sorted; {@code false} otherwise.
     */
    default boolean isSorted() {
        Sortable<E> t;

        t = (Sortable<E>) this.deepCopy();
        t.sort();
        return this.equals(t);
    }

    /**
     * Sorts this structure by its elements' natural order, as defined by {@link Comparable}.
     */
    void sort();

    /**
     * Sorts this structure by the given {@link Comparator}.
     * <p>
     * This method is in this interface and not {@link io.github.vqnxiv.taquin.model.DataStructure}
     * because some of the java collections aren't sortable (e.g {@link java.util.Set}, 
     * {@link java.util.Queue}, {@link java.util.Deque}, etc). And it is possible that future
     * subclass of {@link io.github.vqnxiv.taquin.model.DataStructure} will not be sortable either.
     * <p>
     * While it is limiting to have this here (basic common sense says we don't need {@link Comparable}
     * if we use a {@link Comparator}), we will only use {@link io.github.vqnxiv.taquin.model.Grid},
     * so it doesn't really matter. And it could still be changed to 
     * {@code <T> boolean sort(Comparator<? super T> c);}, or moved into its own interface, e.g
     * {@code ComparatorSortable<E>}, with this one renamed to 
     * {@code ComparableSortable<E extends Comparable<E>>}.
     */
    void sort(Comparator<? super E> c);

    /**
     * Whether this structure was last sorted by using {@link Comparable} or a {@link Comparator}.
     * 
     * @return {@code true} if it was last sorted with a {@link Comparator}, {@code false} otherwise.
     */
    boolean isSortedByComparator();

    /**
     * Getter for the last used {@link Comparator}.
     * 
     * @return {@link Optional#of(Object)} a {@link Comparator} if one was used, {@link Optional#empty()}
     * otherwise.
     */
    Optional<Comparator<? super E>> getLastComparator();
}
