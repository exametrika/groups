/**
 * Copyright 2011 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The {@link WeakIdentityHashMap} is a {@link WeakHashMap} and {@link IdentityHashMap } analog. This class implements only
 * subset of {@link Map} interface;
 * 
 * @param <K> key type
 * @param <V> value type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WeakIdentityHashMap<K, V>
{
    private final Map<WeakReference<K>, V> map = new HashMap<WeakReference<K>, V>();
    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

    public V get(K key)
    {
        expunge();
        WeakReference<K> ref = new IdentityWeakReference<K>(key);
        return map.get(ref);
    }

    public V put(K key, V value)
    {
        Assert.notNull(key);
        
        expunge();
        WeakReference<K> ref = new IdentityWeakReference<K>(key, queue);
        return map.put(ref, value);
    }

    public V remove(K key)
    {
        expunge();
        WeakReference<K> ref = new IdentityWeakReference<K>(key);
        return map.remove(ref);
    }

    private void expunge()
    {
        Reference<? extends K> ref;
        while ((ref = queue.poll()) != null)
            map.remove(ref);
    }

    private static class IdentityWeakReference<T> extends WeakReference<T>
    {
        private final int hashCode;

        private IdentityWeakReference(T referent)
        {
            this(referent, null);
        }

        private IdentityWeakReference(T referent, ReferenceQueue<T> queue)
        {
            super(referent, queue);
            this.hashCode = (referent == null) ? 0 : System.identityHashCode(referent);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof IdentityWeakReference))
                return false;
            
            IdentityWeakReference<?> ref = (IdentityWeakReference<?>)o;
            Object referent = get();
            return (referent != null && referent == ref.get());
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }
    }
}
