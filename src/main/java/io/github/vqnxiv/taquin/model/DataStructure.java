package io.github.vqnxiv.taquin.model;


import java.util.Collection;


/**
 * Base interface which extends {@link Collection} and is similar to {@link java.util.Deque}.
 * <p>
 * {@code null} elements are not accepted.
 * 
 * @param <E> The type of elements.
 *           
 * @see io.github.vqnxiv.taquin.model.structure.Sortable
 * @see io.github.vqnxiv.taquin.model.structure.Sorted
 * @see io.github.vqnxiv.taquin.model.structure.Unsorted
 * @see io.github.vqnxiv.taquin.model.structure.ImprovedSearch
 */
public interface DataStructure<E> extends Collection<E> {

    // https://github.com/openjdk/jdk17/tree/master/src/java.base/share/classes/java/util
    
    /**
     * Retrieves, but does not remove, the first element in this structure.
     * 
     * @return This structure's first element.
     */
    E peekFirst();

    /**
     * Retrieves and remove the first element in this structure.
     *
     * @return This structure's first element.
     */
    E pollFirst();

    /**
     * Retrieves, but does not remove, the last element in this structure.
     *
     * @return This structure's last element.
     */
    E peekLast();

    /**
     * Retrieves and remove the last element in this structure.
     *
     * @return This structure's last element.
     */
    E pollLast();

    /**
     * Finds the index of a given element in this structure.
     * <p>
     * Named that way to avoid clashing with {@link java.util.List#indexOf(Object)}.
     * 
     * @param e The element to find.
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    int indexOfElt(E e);

    /**
     * Whether this structure accepts duplicate elements in regards to {@code equals}.
     * 
     * @return {@code true} if this strcture accepts duplicates elements; {@code false} otherwise.
     */
    boolean acceptsDuplicates();
    
    /**
     * Creates a deep copy of this structure.
     * 
     * @return New {@link DataStructure} with the content of this structure.
     */
    DataStructure<E> deepCopy();
}
