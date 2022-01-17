package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.model.structure.Sorted;
import io.github.vqnxiv.taquin.model.structure.Unsorted;
import io.github.vqnxiv.taquin.util.IBuilder;
import javafx.beans.property.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 * Base interface which extends {@link Collection} and is similar to {@link java.util.Deque}.
 * <p>
 * Method names start with {@code ds*} to avoid clashing with other methods from
 * {@link Collection} and its subinterfaces.
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

    class Builder implements IBuilder {
    
        private final ObjectProperty<Class<?>> klass;
        private final BooleanProperty initialCapacity;
        private final IntegerProperty userInitialCapacity;
        private Comparator<?> comparator;
        
        
        public Builder(String name, Class<?> c) {
            klass = new SimpleObjectProperty<>(this, name + " class", c);
            initialCapacity = new SimpleBooleanProperty(this, name + " increase capacity", false);
            userInitialCapacity = new SimpleIntegerProperty(this, name + " capacity", 0);
        }

        public Builder klass(Class<?> c) {
            klass.set(c);
            return this;
        }
        
        public Builder initialCapacity(boolean b) {
            initialCapacity.set(b);
            return this;
        }
        
        public Builder userInitialCapacity(int n) {
            userInitialCapacity.set(n);
            return this;
        }

        public Builder comparator(Comparator<?> comparator) {
            this.comparator = comparator;
            return this;
        }
        
        @Override
        public Map<String, Property<?>> getNamedProperties() {
            return Map.of(klass.getName(), klass);
        }
        
        @Override
        public EnumMap<Category, List<Property<?>>> getBatchProperties() {
            return new EnumMap<>(Map.of(
                IBuilder.Category.COLLECTION,
                List.of(initialCapacity, userInitialCapacity)
            ));
        }

        public DataStructure<?> build() {
            var c = klass.getValue();
            
            if(!(Sorted.class).isAssignableFrom(c) && !(Unsorted.class).isAssignableFrom(c)) {
                throw new IllegalArgumentException("Invalid class " + c);
            }
            
            DataStructure<?> ret = null;
            
            int cap = 0;
            
            if(initialCapacity.get() || userInitialCapacity.get() > 0) {
                cap = (userInitialCapacity.get() != 0) ? userInitialCapacity.get() : 100_000;
            }   
            
            try {
                if((Sorted.class).isAssignableFrom(c)) {
                    // null comparator will make the structure use comparable
                    if(cap != 0) {
                        ret = (Sorted<?>) c.getDeclaredConstructor(int.class, Comparator.class).newInstance(cap, comparator);
                    } else {
                        ret = (Sorted<?>) c.getDeclaredConstructor(Comparator.class).newInstance(comparator);
                    }
                }
                else {
                    if(cap != 0) {
                        ret = (Unsorted<?>) c.getDeclaredConstructor(int.class).newInstance(cap);
                    } else {
                        ret = (Unsorted<?>) c.getDeclaredConstructor().newInstance();
                    }
                }
            } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                // e.printStackTrace();
            }
            
            if(ret == null) {
                throw new IllegalArgumentException("couldnt create");
            }

            return ret;
        }
    }
    
    
    /**
     * Retrieves, but does not remove, the first element in this structure.
     * 
     * @return This structure's first element.
     */
    E dsPeekFirst();

    /**
     * Retrieves and removes the first element in this structure.
     *
     * @return This structure's first element.
     */
    E dsPollFirst();

    /**
     * Retrieves, but does not remove, the last element in this structure.
     *
     * @return This structure's last element.
     */
    E dsPeekLast();

    /**
     * Retrieves and removes the last element in this structure.
     *
     * @return This structure's last element.
     */
    E dsPollLast();

    /**
     * Finds the index of a given element in this structure.
     * 
     * @param e The element to find.
     * @return Positive int if the element was found, {@code -1} otherwise.
     */
    int dsIndexOf(E e);

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
