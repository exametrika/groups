/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import sun.misc.Unsafe;

/**
 * The {@link CacheSizes} contains different utility methods to get cache sizes of various classes.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CacheSizes
{
    public static final int ARRAY_CACHE_SIZE = Memory.getShallowSize(Object[].class, 0);
    public static final int IMMUTABLES_MAP_CACHE_SIZE = Memory.getShallowSize(
        Classes.forName("com.exametrika.common.utils.Immutables$ImmutableMap"));
    public static final int IMMUTABLES_SET_CACHE_SIZE = Memory.getShallowSize(
        Classes.forName("com.exametrika.common.utils.Immutables$ImmutableSet"));
    public static final int IMMUTABLES_LIST_CACHE_SIZE = Memory.getShallowSize(
        Classes.forName("com.exametrika.common.utils.Immutables$ImmutableRandomAccessList"));
    public static final int LINKED_HASH_MAP_CACHE_SIZE = Memory.getShallowSize(LinkedHashMap.class);
    public static final int LINKED_HASH_MAP_ENTRY_CACHE_SIZE = Memory.getShallowSize(
        Classes.forName("java.util.LinkedHashMap$Entry"));
    public static final int HASH_MAP_ENTRY_CACHE_SIZE = LINKED_HASH_MAP_ENTRY_CACHE_SIZE;
    public static final int LINKED_HASH_SET_CACHE_SIZE = Memory.getShallowSize(LinkedHashSet.class);
    public static final int TREE_MAP_CACHE_SIZE = Memory.getShallowSize(TreeSet.class);
    public static final int TREE_SET_CACHE_SIZE = Memory.getShallowSize(TreeSet.class);
    public static final int TREE_MAP_ENTRY_CACHE_SIZE = Memory.getShallowSize(
        Classes.forName("java.util.TreeMap$Entry"));
    public static final int BYTE_ARRAY_CACHE_SIZE = Memory.getShallowSize(ByteArray.class);
    public static final int ARRAY_LIST_CACHE_SIZE = Memory.getShallowSize(ArrayList.class);
    public static final int STRING_CACHE_SIZE = Memory.getShallowSize(String.class);
    public static final int UUID_CACHE_SIZE = Memory.getShallowSize(UUID.class);
    
    public static int getStringCacheSize(String value)
    {
        return STRING_CACHE_SIZE + Numbers.pad(ARRAY_CACHE_SIZE + value.length() * 2, 8);
    }
    
    public static int getArrayListCacheSize(List value)
    {
        return ARRAY_LIST_CACHE_SIZE + Numbers.pad(ARRAY_CACHE_SIZE + value.size() * Unsafe.ADDRESS_SIZE, 8);
    }
    
    public static int getByteArrayCacheSize(ByteArray value)
    {
        return BYTE_ARRAY_CACHE_SIZE + Numbers.pad(ARRAY_CACHE_SIZE + value.getLength(), 8);
    }
    
    public static int getLinkedHashMapCacheSize(Map value)
    {
        return LINKED_HASH_MAP_CACHE_SIZE + Numbers.pad(ARRAY_CACHE_SIZE + value.size() * Unsafe.ADDRESS_SIZE, 8) + 
            LINKED_HASH_MAP_ENTRY_CACHE_SIZE * value.size();
    }
    
    public static int getLinkedHashSetCacheSize(Set value)
    {
        return LINKED_HASH_MAP_CACHE_SIZE + Numbers.pad(ARRAY_CACHE_SIZE + value.size() * Unsafe.ADDRESS_SIZE, 8) + 
            LINKED_HASH_MAP_ENTRY_CACHE_SIZE * value.size() + LINKED_HASH_SET_CACHE_SIZE;
    }
    
    public static int getTreeMapCacheSize(Map value)
    {
        return TREE_MAP_CACHE_SIZE + TREE_MAP_ENTRY_CACHE_SIZE * value.size();
    }
    
    public static int getTreeSetCacheSize(Set value)
    {
        return TREE_MAP_CACHE_SIZE + TREE_MAP_ENTRY_CACHE_SIZE * value.size() + TREE_SET_CACHE_SIZE;
    }
}
