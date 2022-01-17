package io.github.vqnxiv.taquin.model.structure;


import io.github.vqnxiv.taquin.util.Utils;


/**
 * A marker interface similar to {@link java.util.RandomAccess} which indicates
 * that the implementing {@link io.github.vqnxiv.taquin.model.DataStructure}
 * supports better search-through than sequentially iterating over its elements one by one.
 * <p>
 * As a requirement, this {@link io.github.vqnxiv.taquin.model.DataStructure}
 * must implement either {@link Sortable} or {@link Sorted} to ensure the structure
 * provides the total ordering necessary for non iterative searches.
 * 
 * @param <E> The type of elements.
 * 
 * @see io.github.vqnxiv.taquin.model.DataStructure
 * @see Sortable
 * @see Sorted
 */
public interface ImprovedSearch<E extends Comparable<E>> {

    /**
     * Enum which contains all the possible search algorithms in this interface.
     */
    enum SearchType {
        /**
         * Iterative search.
         */
        ITERATIVE,
        /**
         * Binary search.
         */
        BINARY,
        /**
         * Exponential search.
         */
        EXPONENTIAL;

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }


    /**
     * Base contains method.
     * 
     * @param e The element to find.
     * @param s The search type to use.
     * @return {@code true} if the element was found, {@code false} otherwise.
     */
    default boolean contains(E e, SearchType s) {
        return switch(s) {
            case ITERATIVE -> iterativeContains(e);
            case BINARY -> binaryContains(e);
            case EXPONENTIAL -> exponentialContains(e);
        };
    }
    
    /**
     * Iterative look up, i.e checking every element one by one.
     * 
     * @param e The element to find.
     * @return {@code true} if the element was found, {@code false} otherwise.
     */
    default boolean iterativeContains(E e) {
        return iterativeIndexOf(e) > -1;
    }
    
    /**
     * Binary search look up.
     *
     * @param e The element to find.
     * @return {@code true} if the element was found, {@code false} otherwise.
     */
    default boolean binaryContains(E e) {
        return binaryIndexOf(e) > -1;
    }

    /**
     * Exponential search look up.
     *
     * @param e The element to find.
     * @return {@code true} if the element was found, {@code false} otherwise.
     */
    default boolean exponentialContains(E e) {
        return exponentialIndexOf(e) > -1;
    }

    
    /**
     * Base {@code indexOf} method.
     *
     * @param e The element to find.
     * @param s The search type to use.
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    default int indexOf(E e, SearchType s) {
        return switch(s) {
            case ITERATIVE -> iterativeIndexOf(e);
            case BINARY -> binaryIndexOf(e);
            case EXPONENTIAL -> exponentialIndexOf(e);
        };
    }
    
    /**
     * Iterative look up, i.e checking every element one by one.
     *
     * @param e The element to find.
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    int iterativeIndexOf(E e);

    /**
     * Binary search look up.
     *
     * @param e The element to find.
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    int binaryIndexOf(E e);

    /**
     * Exponential search look up.
     *
     * @param e The element to find.
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    int exponentialIndexOf(E e);
}
