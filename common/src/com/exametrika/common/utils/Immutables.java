/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * The {@link Immutables} contains different utility methods for immutable wrapper manipulation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Immutables
{
    /**
     * Returns an unmodifiable view of the specified collection. This method allows modules to provide users with
     * "read-only" access to internal collections. Query operations on the returned collection "read through" to the
     * specified collection, and attempts to modify the returned collection, whether direct or via its iterator, result
     * in an <tt>UnsupportedOperationException</tt>.
     * <p>
     * The returned collection does <i>not</i> pass the hashCode and equals operations through to the backing
     * collection, but relies on <tt>Object</tt>'s <tt>equals</tt> and <tt>hashCode</tt> methods. This is necessary to
     * preserve the contracts of these operations in the case that the backing collection is a set or a list.
     * <p>
     * The returned collection will be serializable if the specified collection is serializable.
     * 
     * @param <T> element type
     * @param c the collection for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified collection.
     */
    public static <T> Collection<T> wrap(Collection<? extends T> c)
    {
        if (c == null)
            return null;
        if (c instanceof IWrapper)
            return (Collection)c;
        return new ImmutableCollection<T>(c);
    }

    /**
     * Returns an unmodifiable view of the specified set. This method allows modules to provide users with "read-only"
     * access to internal sets. Query operations on the returned set "read through" to the specified set, and attempts
     * to modify the returned set, whether direct or via its iterator, result in an
     * <tt>UnsupportedOperationException</tt>.
     * <p>
     * The returned set will be serializable if the specified set is serializable.
     * 
     * @param <T> element type
     * @param s the set for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified set.
     */
    public static <T> Set<T> wrap(Set<? extends T> s)
    {
        if (s == null)
            return null;
        if (s instanceof IWrapper)
            return (Set)s;
        return new ImmutableSet<T>(s);
    }

    /**
     * Returns an unmodifiable view of the specified sorted set. This method allows modules to provide users with
     * "read-only" access to internal sorted sets. Query operations on the returned sorted set "read through" to the
     * specified sorted set. Attempts to modify the returned sorted set, whether direct, via its iterator, or via its
     * <tt>subSet</tt>, <tt>headSet</tt>, or <tt>tailSet</tt> views, result in an <tt>UnsupportedOperationException</tt>
     * .
     * <p>
     * The returned sorted set will be serializable if the specified sorted set is serializable.
     * 
     * @param <T> element type
     * @param s the sorted set for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified sorted set.
     */
    public static <T> SortedSet<T> wrap(SortedSet<? extends T> s)
    {
        if (s == null)
            return null;
        if (s instanceof IWrapper)
            return (SortedSet)s;
        return new ImmutableSortedSet<T>(s);
    }

    /**
     * Returns an unmodifiable view of the specified list. This method allows modules to provide users with "read-only"
     * access to internal lists. Query operations on the returned list "read through" to the specified list, and
     * attempts to modify the returned list, whether direct or via its iterator, result in an
     * <tt>UnsupportedOperationException</tt>.
     * <p>
     * The returned list will be serializable if the specified list is serializable. Similarly, the returned list will
     * implement {@link RandomAccess} if the specified list does.
     *
     * @param <T> element type
     * @param list the list for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified list.
     */
    public static <T> List<T> wrap(List<? extends T> list)
    {
        if (list == null)
            return null;
        if (list instanceof IWrapper)
            return (List)list;
        return (list instanceof RandomAccess ? new ImmutableRandomAccessList<T>(list) : new ImmutableList<T>(list));
    }

    /**
     * Returns an unmodifiable view of the specified map. This method allows modules to provide users with "read-only"
     * access to internal maps. Query operations on the returned map "read through" to the specified map, and attempts
     * to modify the returned map, whether direct or via its collection views, result in an
     * <tt>UnsupportedOperationException</tt>.
     * <p>
     * The returned map will be serializable if the specified map is serializable.
     * 
     * @param <K> key type
     * @param <V> value type
     * @param m the map for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified map.
     */
    public static <K, V> Map<K, V> wrap(Map<? extends K, ? extends V> m)
    {
        if (m == null)
            return null;
        if (m instanceof IWrapper)
            return (Map)m;
        return new ImmutableMap<K, V>(m);
    }

    /**
     * Returns an unmodifiable view of the specified sorted map. This method allows modules to provide users with
     * "read-only" access to internal sorted maps. Query operations on the returned sorted map "read through" to the
     * specified sorted map. Attempts to modify the returned sorted map, whether direct, via its collection views, or
     * via its <tt>subMap</tt>, <tt>headMap</tt>, or <tt>tailMap</tt> views, result in an
     * <tt>UnsupportedOperationException</tt>.
     * <p>
     * The returned sorted map will be serializable if the specified sorted map is serializable.
     * 
     * @param <K> key type
     * @param <V> value type
     * @param m the sorted map for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified sorted map.
     */
    public static <K, V> SortedMap<K, V> wrap(SortedMap<K, ? extends V> m)
    {
        if (m == null)
            return null;
        if (m instanceof IWrapper)
            return (SortedMap)m;
        return new ImmutableSortedMap<K, V>(m);
    }

    /**
     * Returns "original" modifiable collection of the specified immutable wrapper collection.
     *
     * @param <T> collection type
     * @param collection immutable wrapper collection
     * @return "original" modifiable collection or specified collection if specified collection is not immutable wrapper
     */
    public static <T> T unwrap(T collection)
    {
        if (collection instanceof IWrapper)
        {
            return (T)((IWrapper)collection).getObject();
        }
        
        return collection;
    }
    
    private static boolean eq(Object o1, Object o2)
    {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    private Immutables()
    {
    }

    private interface IWrapper
    {
        Object getObject();
    }

    private static class ImmutableCollection<E> implements Collection<E>, IWrapper, Serializable
    {
        final Collection<? extends E> c;

        ImmutableCollection(Collection<? extends E> c)
        {
            if (c == null)
                throw new NullPointerException();
            this.c = c;
        }

        @Override
        public Object getObject()
        {
            return c;
        }
        
        @Override
        public int size()
        {
            return c.size();
        }

        @Override
        public boolean isEmpty()
        {
            return c.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return c.contains(o);
        }

        @Override
        public Object[] toArray()
        {
            return c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return c.toArray(a);
        }

        @Override
        public String toString()
        {
            return c.toString();
        }

        @Override
        public Iterator<E> iterator()
        {
            return new Iterator<E>()
            {
                Iterator<? extends E> i = c.iterator();

                @Override
                public boolean hasNext()
                {
                    return i.hasNext();
                }

                @Override
                public E next()
                {
                    return i.next();
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public boolean add(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> coll)
        {
            return c.containsAll(coll);
        }

        @Override
        public boolean addAll(Collection<? extends E> coll)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> coll)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> coll)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }
    }

    private static class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E>
    {
        ImmutableSet(Set<? extends E> s)
        {
            super(s);
        }

        @Override
        public boolean equals(Object o)
        {
            return o == this || c.equals(o);
        }

        @Override
        public int hashCode()
        {
            return c.hashCode();
        }
    }

    private static class ImmutableSortedSet<E> extends ImmutableSet<E> implements SortedSet<E>
    {
        private final SortedSet<E> ss;

        ImmutableSortedSet(SortedSet<? extends E> s)
        {
            super(s);
            ss = (SortedSet)s;
        }

        @Override
        public Comparator<? super E> comparator()
        {
            return ss.comparator();
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement)
        {
            return new ImmutableSortedSet<E>(ss.subSet(fromElement, toElement));
        }

        @Override
        public SortedSet<E> headSet(E toElement)
        {
            return new ImmutableSortedSet<E>(ss.headSet(toElement));
        }

        @Override
        public SortedSet<E> tailSet(E fromElement)
        {
            return new ImmutableSortedSet<E>(ss.tailSet(fromElement));
        }

        @Override
        public E first()
        {
            return ss.first();
        }

        @Override
        public E last()
        {
            return ss.last();
        }
    }

    private static class ImmutableList<E> extends ImmutableCollection<E> implements List<E>
    {
        final List<? extends E> list;

        ImmutableList(List<? extends E> list)
        {
            super(list);
            this.list = list;
        }

        @Override
        public boolean equals(Object o)
        {
            return o == this || list.equals(o);
        }

        @Override
        public int hashCode()
        {
            return list.hashCode();
        }

        @Override
        public E get(int index)
        {
            return list.get(index);
        }

        @Override
        public E set(int index, E element)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, E element)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E remove(int index)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o)
        {
            return list.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o)
        {
            return list.lastIndexOf(o);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<E> listIterator()
        {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(final int index)
        {
            return new ListIterator<E>()
            {
                ListIterator<? extends E> i = list.listIterator(index);

                @Override
                public boolean hasNext()
                {
                    return i.hasNext();
                }

                @Override
                public E next()
                {
                    return i.next();
                }

                @Override
                public boolean hasPrevious()
                {
                    return i.hasPrevious();
                }

                @Override
                public E previous()
                {
                    return i.previous();
                }

                @Override
                public int nextIndex()
                {
                    return i.nextIndex();
                }

                @Override
                public int previousIndex()
                {
                    return i.previousIndex();
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(E e)
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void add(E e)
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex)
        {
            return new ImmutableList<E>(list.subList(fromIndex, toIndex));
        }
    }

    private static class ImmutableRandomAccessList<E> extends ImmutableList<E> implements RandomAccess
    {
        ImmutableRandomAccessList(List<? extends E> list)
        {
            super(list);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex)
        {
            return new ImmutableRandomAccessList<E>(list.subList(fromIndex, toIndex));
        }
    }

    private static class ImmutableMap<K, V> implements Map<K, V>, IWrapper, Serializable
    {
        private final Map<? extends K, ? extends V> m;

        ImmutableMap(Map<? extends K, ? extends V> m)
        {
            if (m == null)
                throw new NullPointerException();
            this.m = m;
        }

        @Override
        public Object getObject()
        {
            return m;
        }
        
        @Override
        public int size()
        {
            return m.size();
        }

        @Override
        public boolean isEmpty()
        {
            return m.isEmpty();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return m.containsKey(key);
        }

        @Override
        public boolean containsValue(Object val)
        {
            return m.containsValue(val);
        }

        @Override
        public V get(Object key)
        {
            return m.get(key);
        }

        @Override
        public V put(K key, V value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        private transient Set<K> keySet = null;
        private transient Set<Map.Entry<K, V>> entrySet = null;
        private transient Collection<V> values = null;

        @Override
        public Set<K> keySet()
        {
            if (keySet == null)
                keySet = wrap(m.keySet());
            return keySet;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet()
        {
            if (entrySet == null)
                entrySet = new ImmutableEntrySet<K, V>(m.entrySet());
            return entrySet;
        }

        @Override
        public Collection<V> values()
        {
            if (values == null)
                values = wrap(m.values());
            return values;
        }

        @Override
        public boolean equals(Object o)
        {
            return o == this || m.equals(o);
        }

        @Override
        public int hashCode()
        {
            return m.hashCode();
        }

        @Override
        public String toString()
        {
            return m.toString();
        }

        private static class ImmutableEntrySet<K, V> extends ImmutableSet<Map.Entry<K, V>>
        {
            ImmutableEntrySet(Set<? extends Map.Entry<? extends K, ? extends V>> s)
            {
                super((Set)s);
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator()
            {
                return new Iterator<Map.Entry<K, V>>()
                {
                    Iterator<? extends Map.Entry<? extends K, ? extends V>> i = c.iterator();

                    @Override
                    public boolean hasNext()
                    {
                        return i.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next()
                    {
                        return new ImmutableEntry<K, V>(i.next());
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public Object[] toArray()
            {
                Object[] a = c.toArray();
                for (int i = 0; i < a.length; i++)
                    a[i] = new ImmutableEntry<K, V>((Map.Entry<K, V>)a[i]);
                return a;
            }

            @Override
            public <T> T[] toArray(T[] a)
            {
                Object[] arr = c.toArray(a.length == 0 ? a : Arrays.copyOf(a, 0));

                for (int i = 0; i < arr.length; i++)
                    arr[i] = new ImmutableEntry<K, V>((Map.Entry<K, V>)arr[i]);

                if (arr.length > a.length)
                    return (T[])arr;

                System.arraycopy(arr, 0, a, 0, arr.length);
                if (a.length > arr.length)
                    a[arr.length] = null;
                return a;
            }

            @Override
            public boolean contains(Object o)
            {
                if (!(o instanceof Map.Entry))
                    return false;
                return c.contains(new ImmutableEntry<K, V>((Map.Entry<K, V>)o));
            }

            @Override
            public boolean containsAll(Collection<?> coll)
            {
                Iterator<?> e = coll.iterator();
                while (e.hasNext())
                    if (!contains(e.next()))
                        return false;
                return true;
            }

            @Override
            public boolean equals(Object o)
            {
                if (o == this)
                    return true;

                if (!(o instanceof Set))
                    return false;
                Set s = (Set)o;
                if (s.size() != c.size())
                    return false;
                return containsAll(s);
            }

            private static class ImmutableEntry<K, V> implements Map.Entry<K, V>, Serializable
            {
                private Map.Entry<? extends K, ? extends V> e;

                ImmutableEntry(Map.Entry<? extends K, ? extends V> e)
                {
                    this.e = e;
                }

                @Override
                public K getKey()
                {
                    return e.getKey();
                }
                
                @Override
                public V getValue()
                {
                    return e.getValue();
                }

                @Override
                public V setValue(V value)
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int hashCode()
                {
                    return e.hashCode();
                }

                @Override
                public boolean equals(Object o)
                {
                    if (!(o instanceof Map.Entry))
                        return false;
                    Map.Entry t = (Map.Entry)o;
                    return eq(e.getKey(), t.getKey()) && eq(e.getValue(), t.getValue());
                }

                @Override
                public String toString()
                {
                    return e.toString();
                }
            }
        }
    }

    private static class ImmutableSortedMap<K, V> extends ImmutableMap<K, V> implements SortedMap<K, V>
    {
        private final SortedMap<K, ? extends V> sm;

        ImmutableSortedMap(SortedMap<K, ? extends V> m)
        {
            super(m);
            sm = m;
        }

        @Override
        public Comparator<? super K> comparator()
        {
            return sm.comparator();
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey)
        {
            return new ImmutableSortedMap<K, V>(sm.subMap(fromKey, toKey));
        }

        @Override
        public SortedMap<K, V> headMap(K toKey)
        {
            return new ImmutableSortedMap<K, V>(sm.headMap(toKey));
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey)
        {
            return new ImmutableSortedMap<K, V>(sm.tailMap(fromKey));
        }

        @Override
        public K firstKey()
        {
            return sm.firstKey();
        }

        @Override
        public K lastKey()
        {
            return sm.lastKey();
        }
    }
}
