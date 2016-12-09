/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;


/**
 * The {@link Enums} contains different utility methods for enum manipulation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Enums
{
    private static final int MAX_SMALL_ENUMSET_SIZE = 50;
    
    /**
     * Creates an empty enum set with the specified element type.
     *
     * @param <E> enum type
     * @param elementType the class object of the element type for this enum set
     * @return enum set
     */
    public static <E extends Enum<E>> Set<E> noneOf(Class<E> elementType)
    {
        return create(elementType);
    }

    /**
     * Creates an enum set containing all of the elements in the specified element type.
     * 
     * @param <E> enum type
     * @param elementType the class object of the element type for this enum set
     * @return enum set
     */
    public static <E extends Enum<E>> Set<E> allOf(Class<E> elementType)
    {
        EnumSet<E> result = create(elementType);
        result.addAll();
        return result;
    }

    /**
     * Creates an enum set initialized from the specified collection. If the specified collection is created by {@link Enums}, 
     * creates an enum set with the same element type as the specified enum set, initially containing the same elements (if any). 
     * Otherwise, the specified collection must contain at least one element (in order to determine the new enum set's element type).
     * 
     * @param <E> enum type
     * @param c the collection from which to initialize this enum set
     * @return enum set
     * @throws IllegalArgumentException if <tt>c</tt> is not created by {@link Enums} and contains no elements
     */
    public static <E extends Enum<E>> Set<E> copyOf(Collection<E> c)
    {
        return createCopy(c);
    }

    /**
     * Creates an enum set with the same element type as the specified enum set, initially containing all the elements
     * of this type that are <i>not</i> contained in the specified set.
     * 
     * @param <E> enum type
     * @param s the enum set from whose complement to initialize this enum set
     * @return enum set
     */
    public static <E extends Enum<E>> Set<E> complementOf(Set<E> s)
    {
        EnumSet<E> result = createCopy(s);
        result.complement();
        return result;
    }

    /**
     * Creates an enum set initially containing the specified element. Overloadings of this method exist to initialize
     * an enum set with one through five elements. A sixth overloading is provided that uses the varargs feature. This
     * overloading may be used to create an enum set initially containing an arbitrary number of elements, but is likely
     * to run slower than the overloadings that do not use varargs.
     * 
     * @param <E> enum type
     * @param e the element that this set is to contain initially
     * @return an enum set initially containing the specified element
     */
    public static <E extends Enum<E>> Set<E> of(E e)
    {
        EnumSet<E> result = create(e.getDeclaringClass());
        result.add(e);
        return result;
    }

    /**
     * Creates an enum set initially containing the specified elements. Overloadings of this method exist to initialize
     * an enum set with one through five elements. A sixth overloading is provided that uses the varargs feature. This
     * overloading may be used to create an enum set initially containing an arbitrary number of elements, but is likely
     * to run slower than the overloadings that do not use varargs.
     * 
     * @param <E> enum type
     * @param e1 an element that this set is to contain initially
     * @param e2 another element that this set is to contain initially
     * @return an enum set initially containing the specified elements
     */
    public static <E extends Enum<E>> Set<E> of(E e1, E e2)
    {
        EnumSet<E> result = create(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        return result;
    }

    /**
     * Creates an enum set initially containing the specified elements. Overloadings of this method exist to initialize
     * an enum set with one through five elements. A sixth overloading is provided that uses the varargs feature. This
     * overloading may be used to create an enum set initially containing an arbitrary number of elements, but is likely
     * to run slower than the overloadings that do not use varargs.
     *
     * @param <E> enum type
     * @param e1 an element that this set is to contain initially
     * @param e2 another element that this set is to contain initially
     * @param e3 another element that this set is to contain initially
     * @return an enum set initially containing the specified elements
     */
    public static <E extends Enum<E>> Set<E> of(E e1, E e2, E e3)
    {
        EnumSet<E> result = create(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        return result;
    }

    /**
     * Creates an enum set initially containing the specified elements. Overloadings of this method exist to initialize
     * an enum set with one through five elements. A sixth overloading is provided that uses the varargs feature. This
     * overloading may be used to create an enum set initially containing an arbitrary number of elements, but is likely
     * to run slower than the overloadings that do not use varargs.
     * 
     * @param <E> enum type
     * @param e1 an element that this set is to contain initially
     * @param e2 another element that this set is to contain initially
     * @param e3 another element that this set is to contain initially
     * @param e4 another element that this set is to contain initially
     * @return an enum set initially containing the specified elements
     */
    public static <E extends Enum<E>> Set<E> of(E e1, E e2, E e3, E e4)
    {
        EnumSet<E> result = create(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        return result;
    }

    /**
     * Creates an enum set initially containing the specified elements. Overloadings of this method exist to initialize
     * an enum set with one through five elements. A sixth overloading is provided that uses the varargs feature. This
     * overloading may be used to create an enum set initially containing an arbitrary number of elements, but is likely
     * to run slower than the overloadings that do not use varargs.
     * 
     * @param <E> enum type
     * @param e1 an element that this set is to contain initially
     * @param e2 another element that this set is to contain initially
     * @param e3 another element that this set is to contain initially
     * @param e4 another element that this set is to contain initially
     * @param e5 another element that this set is to contain initially
     * @return an enum set initially containing the specified elements
     */
    public static <E extends Enum<E>> Set<E> of(E e1, E e2, E e3, E e4, E e5)
    {
        EnumSet<E> result = create(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        result.add(e5);
        return result;
    }

    /**
     * Creates an enum set initially containing the specified elements. This factory, whose parameter list uses the
     * varargs feature, may be used to create an enum set initially containing an arbitrary number of elements, but it
     * is likely to run slower than the overloadings that do not use varargs.
     * 
     * @param <E> enum type
     * @param first an element that the set is to contain initially
     * @param rest the remaining elements the set is to contain initially
     * @return an enum set initially containing the specified elements
     */
    public static <E extends Enum<E>> Set<E> of(E first, E... rest)
    {
        EnumSet<E> result = create(first.getDeclaringClass());
        result.add(first);
        for (E e : rest)
            result.add(e);
        return result;
    }

    /**
     * Creates an enum set initially containing all of the elements in the range defined by the two specified endpoints.
     * The returned set will contain the endpoints themselves, which may be identical but must not be out of order.
     * 
     * @param <E> enum type
     * @param from the first element in the range
     * @param to the last element in the range
     * @throws IllegalArgumentException if <tt>first.compareTo(last) &gt; 0</tt>
     * @return an enum set initially containing all of the elements in the range defined by the two specified endpoints
     */
    public static <E extends Enum<E>> Set<E> range(E from, E to)
    {
        if (from.compareTo(to) > 0)
            throw new IllegalArgumentException(from + " > " + to);
        EnumSet<E> result = create(from.getDeclaringClass());
        result.addRange(from, to);
        return result;
    }
    
    /**
     * Serializes enum set, created by {@link Enums} class.
     *
     * @param <E> enum type
     * @param serialization serialization helper
     * @param set enum set, created by {@link Enums} class
     * @exception IllegalArgumentException if enum set is not created by {@link Enums} class
     */
    public static <E extends Enum<E>> void serialize(IDataSerialization serialization, Set<E> set)
    {
        Assert.notNull(serialization);
        Assert.notNull(set);
        
        set = Immutables.unwrap(set);
        
        if (set instanceof SmallEnumSet)
        {
            SmallEnumSet smallSet = (SmallEnumSet)set;
            serialization.writeLong(smallSet.getElements());
        }
        else if (set instanceof LargeEnumSet)
        {
            LargeEnumSet largeSet = (LargeEnumSet)set;
            serialization.writeInt(largeSet.getSize());
            
            serialization.writeInt(largeSet.getElements().length);
            
            for (int i = 0; i < largeSet.getElements().length; i++)
            {
                long element = largeSet.getElements()[i];
                serialization.writeLong(element);
            }
        }
        else
            throw new IllegalArgumentException();
    }
    
    /**
     * Deserializes enum set.
     *
     * @param <E> enum type
     * @param deserialization deserialization helper
     * @param elementType enum type
     * @param values enum values
     * @return enum set
     */
    public static <E extends Enum<E>> Set<E> deserialize(IDataDeserialization deserialization, Class<E> elementType, E[] values)
    {
        Assert.notNull(deserialization);
        Assert.notNull(elementType);
        Assert.notNull(values);
        
        if (values.length <= MAX_SMALL_ENUMSET_SIZE)
        {
            long elements = deserialization.readLong();
            return new SmallEnumSet(elementType, values, elements);
        }

        int size = deserialization.readInt();
        
        int count = deserialization.readInt();
        long[] elements = new long[count];
        for (int i = 0; i < count; i++)
            elements[i] = deserialization.readLong();
        
        return new LargeEnumSet(elementType, values, elements, size);
    }

    private static <E extends Enum<E>> EnumSet<E> create(Class<E> elementType)
    {
        Enum[] universe = getUniverse(elementType);
        if (universe == null)
            throw new ClassCastException(elementType + " not an enum");

        if (universe.length <= MAX_SMALL_ENUMSET_SIZE)
            return new SmallEnumSet<E>(elementType, universe, 0L);

        return new LargeEnumSet<E>(elementType, universe);
    }
    
    private static <E extends Enum<E>> EnumSet<E> createCopy(Collection<E> c)
    {
        c = Immutables.unwrap(c);
        
        if (c instanceof EnumSet)
        {
            return ((EnumSet<E>)c).clone();
        }

        if (c.isEmpty())
            throw new IllegalArgumentException();
        Iterator<E> i = c.iterator();
        E first = i.next();

        EnumSet<E> result = create(first.getDeclaringClass());
        result.add(first);
        
        while (i.hasNext())
            result.add(i.next());
        return result;
    }

    private static <E extends Enum<E>> E[] getUniverse(Class<E> elementType)
    {
        return elementType.getEnumConstants();
    }

    private Enums()
    {
    }
    
    private static abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, Serializable
    {
        protected final Class<E> elementType;
        protected final Enum[] universe;

        public EnumSet(Class<E> elementType, Enum[] universe)
        {
            Assert.notNull(elementType);
            Assert.notNull(universe);
            
            this.elementType = elementType;
            this.universe = universe;
        }

        @Override
        public EnumSet<E> clone()
        {
            try
            {
                return (EnumSet<E>)super.clone();
            }
            catch (CloneNotSupportedException e)
            {
                throw new AssertionError(e);
            }
        }

        abstract void addRange(E from, E to);

        abstract void addAll();

        abstract void complement();

        final void typeCheck(E e)
        {
            Class eClass = e.getClass();
            if (eClass != elementType && eClass.getSuperclass() != elementType)
                throw new ClassCastException(eClass + " != " + elementType);
        }
    }

    private static class SmallEnumSet<E extends Enum<E>> extends EnumSet<E>
    {
        private long elements;

        public SmallEnumSet(Class<E> elementType, Enum[] universe, long elements)
        {
            super(elementType, universe);
            
            this.elements = elements;
        }

        public long getElements()
        {
            return elements;
        }
        
        @Override
        void addRange(E from, E to)
        {
            elements = (-1L >>> (from.ordinal() - to.ordinal() - 1)) << from.ordinal();
        }

        @Override
        void addAll()
        {
            if (universe.length != 0)
                elements = -1L >>> -universe.length;
        }

        @Override
        void complement()
        {
            if (universe.length != 0)
            {
                elements = ~elements;
                elements &= -1L >>> -universe.length; // Mask unused bits
            }
        }

        @Override
        public Iterator<E> iterator()
        {
            return new EnumSetIterator<E>();
        }

        private class EnumSetIterator<T extends Enum<T>> implements Iterator<T>
        {
            long unseen;
            long lastReturned = 0;

            EnumSetIterator()
            {
                unseen = elements;
            }

            @Override
            public boolean hasNext()
            {
                return unseen != 0;
            }

            @Override
            public T next()
            {
                if (unseen == 0)
                    throw new NoSuchElementException();
                lastReturned = unseen & -unseen;
                unseen -= lastReturned;
                return (T)universe[Long.numberOfTrailingZeros(lastReturned)];
            }

            @Override
            public void remove()
            {
                if (lastReturned == 0)
                    throw new IllegalStateException();
                elements -= lastReturned;
                lastReturned = 0;
            }
        }

        @Override
        public int size()
        {
            return Long.bitCount(elements);
        }

        @Override
        public boolean isEmpty()
        {
            return elements == 0;
        }

        @Override
        public boolean contains(Object e)
        {
            if (e == null)
                return false;
            Class eClass = e.getClass();
            if (eClass != elementType && eClass.getSuperclass() != elementType)
                return false;

            return (elements & (1L << ((Enum)e).ordinal())) != 0;
        }

        @Override
        public boolean add(E e)
        {
            typeCheck(e);

            long oldElements = elements;
            elements |= (1L << ((Enum)e).ordinal());
            return elements != oldElements;
        }

        @Override
        public boolean remove(Object e)
        {
            if (e == null)
                return false;
            Class eClass = e.getClass();
            if (eClass != elementType && eClass.getSuperclass() != elementType)
                return false;

            long oldElements = elements;
            elements &= ~(1L << ((Enum)e).ordinal());
            return elements != oldElements;
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof SmallEnumSet))
                return super.containsAll(c);

            SmallEnumSet es = (SmallEnumSet)c;
            if (es.elementType != elementType)
                return es.isEmpty();

            return (es.elements & ~elements) == 0;
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof SmallEnumSet))
                return super.addAll(c);

            SmallEnumSet es = (SmallEnumSet)c;
            if (es.elementType != elementType)
            {
                if (es.isEmpty())
                    return false;
                
                throw new ClassCastException(es.elementType + " != " + elementType);
            }

            long oldElements = elements;
            elements |= es.elements;
            return elements != oldElements;
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof SmallEnumSet))
                return super.removeAll(c);

            SmallEnumSet es = (SmallEnumSet)c;
            if (es.elementType != elementType)
                return false;

            long oldElements = elements;
            elements &= ~es.elements;
            return elements != oldElements;
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof SmallEnumSet))
                return super.retainAll(c);

            SmallEnumSet<?> es = (SmallEnumSet<?>)c;
            if (es.elementType != elementType)
            {
                boolean changed = (elements != 0);
                elements = 0;
                return changed;
            }

            long oldElements = elements;
            elements &= es.elements;
            return elements != oldElements;
        }

        @Override
        public void clear()
        {
            elements = 0;
        }

        @Override
        public boolean equals(Object o)
        {
            o = Immutables.unwrap(o);
            
            if (!(o instanceof SmallEnumSet))
                return super.equals(o);

            SmallEnumSet es = (SmallEnumSet)o;
            if (es.elementType != elementType)
                return elements == 0 && es.elements == 0;
            return es.elements == elements;
        }
    }

    private static class LargeEnumSet<E extends Enum<E>> extends EnumSet<E>
    {
        private long elements[];
        private int size = 0;

        public LargeEnumSet(Class<E> elementType, Enum[] universe)
        {
            super(elementType, universe);
            elements = new long[(universe.length + 63) >>> 6];
        }

        public LargeEnumSet(Class<E> elementType, Enum[] universe, long elements[], int size)
        {
            super(elementType, universe);
            this.elements = elements;
            this.size = size;
        }

        public long[] getElements()
        {
            return elements;
        }

        public int getSize()
        {
            return size;
        }

        @Override
        void addRange(E from, E to)
        {
            int fromIndex = from.ordinal() >>> 6;
            int toIndex = to.ordinal() >>> 6;

            if (fromIndex == toIndex)
            {
                elements[fromIndex] = (-1L >>> (from.ordinal() - to.ordinal() - 1)) << from.ordinal();
            }
            else
            {
                elements[fromIndex] = (-1L << from.ordinal());
                for (int i = fromIndex + 1; i < toIndex; i++)
                    elements[i] = -1;
                elements[toIndex] = -1L >>> (63 - to.ordinal());
            }
            size = to.ordinal() - from.ordinal() + 1;
        }

        @Override
        void addAll()
        {
            for (int i = 0; i < elements.length; i++)
                elements[i] = -1;
            elements[elements.length - 1] >>>= -universe.length;
            size = universe.length;
        }

        @Override
        void complement()
        {
            for (int i = 0; i < elements.length; i++)
                elements[i] = ~elements[i];
            elements[elements.length - 1] &= (-1L >>> -universe.length);
            size = universe.length - size;
        }

        @Override
        public Iterator<E> iterator()
        {
            return new EnumSetIterator<E>();
        }

        private class EnumSetIterator<T extends Enum<T>> implements Iterator<T>
        {
            long unseen;
            int unseenIndex = 0;
            long lastReturned = 0;
            int lastReturnedIndex = 0;

            EnumSetIterator()
            {
                unseen = elements[0];
            }

            @Override
            public boolean hasNext()
            {
                while (unseen == 0 && unseenIndex < elements.length - 1)
                    unseen = elements[++unseenIndex];
                return unseen != 0;
            }

            @Override
            public T next()
            {
                if (!hasNext())
                    throw new NoSuchElementException();
                lastReturned = unseen & -unseen;
                lastReturnedIndex = unseenIndex;
                unseen -= lastReturned;
                return (T)universe[(lastReturnedIndex << 6) + Long.numberOfTrailingZeros(lastReturned)];
            }

            @Override
            public void remove()
            {
                if (lastReturned == 0)
                    throw new IllegalStateException();
                elements[lastReturnedIndex] -= lastReturned;
                size--;
                lastReturned = 0;
            }
        }

        @Override
        public int size()
        {
            return size;
        }

        @Override
        public boolean isEmpty()
        {
            return size == 0;
        }

        @Override
        public boolean contains(Object e)
        {
            if (e == null)
                return false;
            Class eClass = e.getClass();
            if (eClass != elementType && eClass.getSuperclass() != elementType)
                return false;

            int eOrdinal = ((Enum)e).ordinal();
            return (elements[eOrdinal >>> 6] & (1L << eOrdinal)) != 0;
        }

        @Override
        public boolean add(E e)
        {
            typeCheck(e);

            int eOrdinal = e.ordinal();
            int eWordNum = eOrdinal >>> 6;

            long oldElements = elements[eWordNum];
            elements[eWordNum] |= (1L << eOrdinal);
            boolean result = (elements[eWordNum] != oldElements);
            if (result)
                size++;
            return result;
        }

        @Override
        public boolean remove(Object e)
        {
            if (e == null)
                return false;
            Class eClass = e.getClass();
            if (eClass != elementType && eClass.getSuperclass() != elementType)
                return false;
            int eOrdinal = ((Enum)e).ordinal();
            int eWordNum = eOrdinal >>> 6;

            long oldElements = elements[eWordNum];
            elements[eWordNum] &= ~(1L << eOrdinal);
            boolean result = (elements[eWordNum] != oldElements);
            if (result)
                size--;
            return result;
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof LargeEnumSet))
                return super.containsAll(c);

            LargeEnumSet es = (LargeEnumSet)c;
            if (es.elementType != elementType)
                return es.isEmpty();

            for (int i = 0; i < elements.length; i++)
                if ((es.elements[i] & ~elements[i]) != 0)
                    return false;
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof LargeEnumSet))
                return super.addAll(c);

            LargeEnumSet es = (LargeEnumSet)c;
            if (es.elementType != elementType)
            {
                if (es.isEmpty())
                    return false;
                
                throw new ClassCastException(es.elementType + " != " + elementType);
            }

            for (int i = 0; i < elements.length; i++)
                elements[i] |= es.elements[i];
            return recalculateSize();
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof LargeEnumSet))
                return super.removeAll(c);

            LargeEnumSet es = (LargeEnumSet)c;
            if (es.elementType != elementType)
                return false;

            for (int i = 0; i < elements.length; i++)
                elements[i] &= ~es.elements[i];
            return recalculateSize();
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            c = Immutables.unwrap(c);
            
            if (!(c instanceof LargeEnumSet))
                return super.retainAll(c);

            LargeEnumSet<?> es = (LargeEnumSet<?>)c;
            if (es.elementType != elementType)
            {
                boolean changed = (size != 0);
                clear();
                return changed;
            }

            for (int i = 0; i < elements.length; i++)
                elements[i] &= es.elements[i];
            return recalculateSize();
        }

        @Override
        public void clear()
        {
            Arrays.fill(elements, 0);
            size = 0;
        }

        @Override
        public boolean equals(Object o)
        {
            o = Immutables.unwrap(o);
            
            if (!(o instanceof LargeEnumSet))
                return super.equals(o);

            LargeEnumSet es = (LargeEnumSet)o;
            if (es.elementType != elementType)
                return size == 0 && es.size == 0;

            return Arrays.equals(es.elements, elements);
        }

        private boolean recalculateSize()
        {
            int oldSize = size;
            size = 0;
            for (int i = 0; i < elements.length; i++)
            {
                long elt = elements[i];
                size += Long.bitCount(elt);
            }

            return size != oldSize;
        }

        @Override
        public EnumSet<E> clone()
        {
            LargeEnumSet<E> result = (LargeEnumSet<E>)super.clone();
            result.elements = result.elements.clone();
            return result;
        }
    }
}
