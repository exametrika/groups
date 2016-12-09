/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.Arrays;



/**
 * The {@link SparseArray} represents a sparse array of objects.
 * 
 * @param <T> element type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SparseArray<T>
{
    private static final int SHIFT = 8;
    private static final int MASK = (1 << SHIFT) - 1;
    private static final int COUNT = 1 << SHIFT;
    private static final int SHIFT2 = SHIFT * 2;
    private static final int MASK2 = MASK << SHIFT;
    private static final int MASK3 = ~(MASK | MASK2);
    
    private volatile int count;
    private T[][][] values;
    
    public int getCount()
    {
        return count;
    }
    
    public T get(int index)
    {
        int index1 = index & MASK; 
        int index2 = (index & MASK2) >>> SHIFT;
        int index3 = (index & MASK3) >>> SHIFT2;
    
        T[][][] values3 = this.values;
    
        if (values3 != null && index3 < values3.length)
        {
            T[][] values2 = values3[index3];
            if (values2 != null)
            {
                T [] values1 = values2[index2];
                if (values1 != null)
                {
                    values1[index1] = values1[index1]; 
                    return values1[index1];
                }
            }
        }
        
        return null;
    }

    public void set(int index, T value)
    {
        int index1 = index & MASK; 
        int index2 = (index & MASK2) >>> SHIFT;
        int index3 = (index & MASK3) >>> SHIFT2;
    
        T[][][] values3 = this.values;
        
        if (values3 != null && index3 < values3.length)
        {
            T[][] values2 = values3[index3];
            if (values2 != null)
            {
                T[] values1 = values2[index2];
                if (values1 != null)
                {
                    values1[index1] = value;
                    
                    if (index >= count)
                        count = index + 1;
                    else
                        count = count + 0;
                    
                    return;
                }

                values2[index2] = (T[])new Object[COUNT];
                set(index, value);
                
                return;
            }

            values[index3] = (T[][])new Object[COUNT][];
            set(index, value);
            
            return;
        }

        int minCapacity = index3 + 1;
        int oldCapacity = values != null ? values.length : 0;
        int newCapacity = (oldCapacity * 3)/2 + 1;
        if (newCapacity < minCapacity)
            newCapacity = minCapacity;
        if (values != null)
            values = Arrays.copyOf(values, newCapacity);
        else
            values = (T[][][])new Object[newCapacity][][];

        set(index, value);
    }
    
    public void clear()
    {
        values = null;
        count = 0;
    }
}
