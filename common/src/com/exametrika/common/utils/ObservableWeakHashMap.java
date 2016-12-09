/**
 * Copyright 2011 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The {@link ObservableWeakHashMap} is a {@link WeakHashMap} analog which has ability to observe expunging stale entries
 * from the map.
 * 
 * @param <K> key type
 * @param <V> value type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ObservableWeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>
{
    private static final Object NULL_KEY = new Object();
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private Entry[] table;
    private int size;
    private int threshold;
    private final float loadFactor;
    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();
    private volatile int modCount;
    private transient Set<Map.Entry<K, V>> entrySet = null;
    private transient volatile Set<K>        keySet = null;
    private transient volatile Collection<V> values = null;
    private final IStaleEntryObserver<V> observer;

    public interface IStaleEntryObserver<V>
    {
        void onExpunge(V value);
    }
    
    public ObservableWeakHashMap(int initialCapacity, float loadFactor, IStaleEntryObserver<V> observer)
    {
        Assert.notNull(observer);
        
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: " + initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: " + loadFactor);
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        table = new Entry[capacity];
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        this.observer = observer;
    }

    public ObservableWeakHashMap(int initialCapacity, IStaleEntryObserver<V> observer)
    {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, observer);
    }

    public ObservableWeakHashMap(IStaleEntryObserver<V> observer)
    {
        Assert.notNull(observer);
        
        this.observer = observer;
        loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = DEFAULT_INITIAL_CAPACITY;
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }

    public ObservableWeakHashMap(Map<? extends K, ? extends V> m, IStaleEntryObserver<V> observer)
    {
        this(Math.max((int)(m.size() / DEFAULT_LOAD_FACTOR) + 1, 16), DEFAULT_LOAD_FACTOR, observer);
        putAll(m);
    }

    @Override
    public int size()
    {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    @Override
    public V get(Object key)
    {
        Object k = maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K, V> e = tab[index];
        while (e != null)
        {
            if (e.hash == h && eq(k, e.get()))
                return e.value;
            e = e.next;
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return getEntry(key) != null;
    }

    @Override
    public V put(K key, V value)
    {
        K k = (K)maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = getTable();
        int i = indexFor(h, tab.length);

        for (Entry<K, V> e = tab[i]; e != null; e = e.next)
        {
            if (h == e.hash && eq(k, e.get()))
            {
                V oldValue = e.value;
                if (value != oldValue)
                    e.value = value;
                return oldValue;
            }
        }

        modCount++;
        Entry<K, V> e = tab[i];
        tab[i] = new Entry<K, V>(k, value, queue, h, e);
        if (++size >= threshold)
            resize(tab.length * 2);
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;

        if (numKeysToBeAdded > threshold)
        {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    @Override
    public V remove(Object key)
    {
        Object k = maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry<K, V> prev = tab[i];
        Entry<K, V> e = prev;

        while (e != null)
        {
            Entry<K, V> next = e.next;
            if (h == e.hash && eq(k, e.get()))
            {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    @Override
    public void clear()
    {
        Entry<K, V> e;
        while ((e = (Entry<K, V>)queue.poll()) != null)
        {
            observer.onExpunge(e.getValue());
        }

        modCount++;
        Entry[] tab = table;
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;
        size = 0;

        while ((e = (Entry<K, V>)queue.poll()) != null)
        {
            observer.onExpunge(e.getValue());
        }
    }

    @Override
    public boolean containsValue(Object value)
    {
        if (value == null)
            return containsNullValue();

        Entry[] tab = getTable();
        for (int i = tab.length; i-- > 0;)
            for (Entry e = tab[i]; e != null; e = e.next)
                if (value.equals(e.value))
                    return true;
        return false;
    }

    @Override
    public Set<K> keySet()
    {
        Set<K> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    @Override
    public Collection<V> values()
    {
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        Set<Map.Entry<K, V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    @Override
    protected Object clone() throws CloneNotSupportedException 
    {
        ObservableWeakHashMap<K,V> result = (ObservableWeakHashMap<K,V>)super.clone();
        result.keySet = null;
        result.values = null;
        return result;
    }

    private static Object maskNull(Object key)
    {
        return (key == null ? NULL_KEY : key);
    }

    private static <K> K unmaskNull(Object key)
    {
        return (K)(key == NULL_KEY ? null : key);
    }

    private static boolean eq(Object x, Object y)
    {
        return x == y || x.equals(y);
    }

    private static int indexFor(int h, int length)
    {
        return h & (length - 1);
    }

    private void expungeStaleEntries()
    {
        Entry<K, V> e;
        while ((e = (Entry<K, V>)queue.poll()) != null)
        {
            int h = e.hash;
            int i = indexFor(h, table.length);

            Entry<K, V> prev = table[i];
            Entry<K, V> p = prev;
            while (p != null)
            {
                Entry<K, V> next = p.next;
                if (p == e)
                {
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    e.next = null;
                    observer.onExpunge(e.value);
                    e.value = null;
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    private Entry[] getTable()
    {
        expungeStaleEntries();
        return table;
    }

    private Entry<K, V> getEntry(Object key)
    {
        Object k = maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K, V> e = tab[index];
        while (e != null && !(e.hash == h && eq(k, e.get())))
            e = e.next;
        return e;
    }

    private void resize(int newCapacity)
    {
        Entry[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY)
        {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry[] newTable = new Entry[newCapacity];
        transfer(oldTable, newTable);
        table = newTable;

        if (size >= threshold / 2)
            threshold = (int)(newCapacity * loadFactor);
        else
        {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    private void transfer(Entry[] src, Entry[] dest)
    {
        for (int j = 0; j < src.length; ++j)
        {
            Entry<K, V> e = src[j];
            src[j] = null;
            while (e != null)
            {
                Entry<K, V> next = e.next;
                Object key = e.get();
                if (key == null)
                {
                    e.next = null;
                    e.value = null;
                    size--;
                }
                else
                {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    private Entry<K, V> removeMapping(Object o)
    {
        if (!(o instanceof Map.Entry))
            return null;
        Entry[] tab = getTable();
        Map.Entry entry = (Map.Entry)o;
        Object k = maskNull(entry.getKey());
        int h = hash(k.hashCode());
        int i = indexFor(h, tab.length);
        Entry<K, V> prev = tab[i];
        Entry<K, V> e = prev;

        while (e != null)
        {
            Entry<K, V> next = e.next;
            if (h == e.hash && e.equals(entry))
            {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    private boolean containsNullValue()
    {
        Entry[] tab = getTable();
        for (int i = tab.length; i-- > 0;)
            for (Entry e = tab[i]; e != null; e = e.next)
                if (e.value == null)
                    return true;
        return false;
    }

    private static int hash(int h) 
    {
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
    
    private static class Entry<K, V> extends WeakReference<K> implements Map.Entry<K, V>
    {
        private V value;
        private final int hash;
        private Entry<K, V> next;

        public Entry(K key, V value, ReferenceQueue<K> queue, int hash, Entry<K, V> next)
        {
            super(key, queue);
            this.value = value;
            this.hash = hash;
            this.next = next;
        }

        @Override
        public K getKey()
        {
            return ObservableWeakHashMap.<K> unmaskNull(get());
        }

        @Override
        public V getValue()
        {
            return value;
        }

        @Override
        public V setValue(V newValue)
        {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2)))
            {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            Object k = getKey();
            Object v = getValue();
            return ((k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode()));
        }

        @Override
        public String toString()
        {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator<T> implements Iterator<T>
    {
        int index;
        Entry<K, V> entry = null;
        Entry<K, V> lastReturned = null;
        int expectedModCount = modCount;

        Object nextKey = null;

        Object currentKey = null;

        public HashIterator()
        {
            index = (size() != 0 ? table.length : 0);
        }

        @Override
        public boolean hasNext()
        {
            Entry[] t = table;

            while (nextKey == null)
            {
                Entry<K, V> e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null)
                {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get();
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        protected Entry<K, V> nextEntry()
        {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        @Override
        public void remove()
        {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            ObservableWeakHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class ValueIterator extends HashIterator<V>
    {
        @Override
        public V next()
        {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator<K>
    {
        @Override
        public K next()
        {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator<Map.Entry<K, V>>
    {
        @Override
        public Map.Entry<K, V> next()
        {
            return nextEntry();
        }
    }

    private class KeySet extends AbstractSet<K>
    {
        @Override
        public Iterator<K> iterator()
        {
            return new KeyIterator();
        }

        @Override
        public int size()
        {
            return ObservableWeakHashMap.this.size();
        }

        @Override
        public boolean contains(Object o)
        {
            return containsKey(o);
        }

        @Override
        public boolean remove(Object o)
        {
            if (containsKey(o))
            {
                ObservableWeakHashMap.this.remove(o);
                return true;
            }
            else
                return false;
        }

        @Override
        public void clear()
        {
            ObservableWeakHashMap.this.clear();
        }
    }

    private class Values extends AbstractCollection<V>
    {
        @Override
        public Iterator<V> iterator()
        {
            return new ValueIterator();
        }

        @Override
        public int size()
        {
            return ObservableWeakHashMap.this.size();
        }

        @Override
        public boolean contains(Object o)
        {
            return containsValue(o);
        }

        @Override
        public void clear()
        {
            ObservableWeakHashMap.this.clear();
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>>
    {
        @Override
        public Iterator<Map.Entry<K, V>> iterator()
        {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o)
        {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Entry candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }

        @Override
        public boolean remove(Object o)
        {
            return removeMapping(o) != null;
        }

        @Override
        public int size()
        {
            return ObservableWeakHashMap.this.size();
        }

        @Override
        public void clear()
        {
            ObservableWeakHashMap.this.clear();
        }

        private List<Map.Entry<K, V>> deepCopy()
        {
            List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(size());
            for (Map.Entry<K, V> e : this)
                list.add(new AbstractMap.SimpleEntry<K, V>(e));
            return list;
        }

        @Override
        public Object[] toArray()
        {
            return deepCopy().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return deepCopy().toArray(a);
        }
    }
}
