package io.github.vqnxiv.taquin.model.structure.jstructure;


import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.structure.Unsorted;

import java.util.*;


/**
 * A {@link DataStructure} version of {@link java.util.LinkedHashSet}.
 *
 * @param <E> The type of elements.
 */
public class JLinkedHashSet<E> extends LinkedHashSet<E>
    implements DataStructure<E>, Unsorted<E> {

    
    /**
     * Defaults no args constructor which calls the {@link LinkedHashSet} no args constructor.
     */
    public JLinkedHashSet() {
        super();
    }

    /**
     * Constructor with initial capacity.
     *
     * @param capacity Useless param.
     */
    public JLinkedHashSet(int capacity) {
        super(capacity);
    }

    /**
     * Constructor with existing content.
     *
     * @param content {@link Collection} that will be passed to the {@link LinkedHashSet} constructor.
     */
    public JLinkedHashSet(Collection<E> content) {
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
        return stream().findFirst().get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The first element from this list.
     */
    @Override
    public E dsPollFirst() {
        E e = stream().findFirst().get();
        remove(e);
        return e; 
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from this list.
     */
    @Override
    public E dsPeekLast() {
        return stream().skip(size()-1).findFirst().get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The last element from this list.
     */
    @Override
    public E dsPollLast() {
        E e = stream().skip(size()-1).findFirst().get();
        remove(e);
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
     * @return {@code false}.
     */
    @Override
    public boolean acceptsDuplicates() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return New {@link JLinkedHashSet} with the same content as this object.
     */
    @Override
    public DataStructure<E> deepCopy() {
        return new JLinkedHashSet<>(this);
    }
    
    
    /*
        Unsorted
     */

    /**
     * UNSUPPORTED
     * <p>
     * Unfortunately this is unsupported as we would need to abuse reflection
     * and it might not even be possible in the end as {@code LinkedHashMap.Entry}
     * is a nested class?
     * 
     * <pre><code>
     *   try {
     *      var mField = (HashSet.class).getDeclaredField("map");
     *      mField.setAccessible(true);
     *      {@literal var m = (LinkedHashMap<E, Object>) mField.get(this);}
     *
     *      var hField = m.getClass().getDeclaredField("head");
     *      hField.setAccessible(true);
     *      {@literal var head = (LinkedHashMap.Entry<E, Object>) hField.get(m);}
     *
     *      var tField = m.getClass().getDeclaredField("tail");
     *      tField.setAccessible(true);
     *      var tail = tField.get(m);
     *
     *   } catch(NoSuchFieldException ex) {
     *             
     *   } catch(IllegalAccessException ex) {
     *             
     *   }
     * </code></pre>
     * 
     * @throws UnsupportedOperationException Operation not supported.
     */
    @Override
    public boolean uAddFirst(E e) {
        throw new UnsupportedOperationException("Cannot add at the start of a LinkedHashSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uAddLast(E e) {
        return add(e);
    }

    /**
     * UNSUPPORTED. See {@link #uAddFirst(Object)}.
     * 
     * @throws UnsupportedOperationException Operation not supported.
     */
    @Override
    public boolean uAddAllFirst(Collection<E> toAdd) {
        throw new UnsupportedOperationException("Cannot add at the start of a LinkedHashSet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uAddAllLast(Collection<E> toAdd) {
        addAll(toAdd);
        return true;
    }


    /*
        LinkedHashSet overrides
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
        return super.add(e);
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
            // keep the catch
        }
        return super.addAll(c);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof JLinkedHashSet<?> jlhs) {
            return super.equals(jlhs);
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
