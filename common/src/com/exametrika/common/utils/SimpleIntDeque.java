/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;




/**
 * The {@link SimpleIntDeque} is a simple array based deque for integers.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SimpleIntDeque
{
    private int[] elements;
    private int head;
    private int tail;
    
    public SimpleIntDeque()
    {
        this(16);
    }
    
    public SimpleIntDeque(int capacity)
    {
        elements = new int[Numbers.roundToPowerOfTwo(capacity)];
    }
    
    public boolean isEmpty() 
    {
        return head == tail;
    }
    
    public int size()
    {
        return (tail - head) & (elements.length - 1);
    }
    
    public int getFirst()
    {
        return elements[head];
    }
    
    public int getLast()
    {
        return elements[(tail - 1) & (elements.length - 1)];
    }
    
    public void addFirst(int value) 
    {
        head = (head - 1) & (elements.length - 1);
        elements[head] = value;
        if (head == tail)
            doubleCapacity();
    }
    
    public void addLast(int value)
    {
        elements[tail] = value;
        tail = (tail + 1) & (elements.length - 1);
        if (tail == head)
            doubleCapacity();
    }

    public int removeFirst()
    {
        Assert.checkState(!isEmpty());
        
        int h = head;
        int result = elements[h];
        
        elements[h] = 0;
        head = (h + 1) & (elements.length - 1);

        return result;
    }
    
    public int removeLast() 
    {
        Assert.checkState(!isEmpty());
        
        int t = (tail - 1) & (elements.length - 1);
        int result = elements[t];
        
        elements[t] = 0;
        tail = t;
        
        return result;
    }
    
    public void clear() 
    {
        int h = head;
        int t = tail;
        if (h != t) 
        {
            head = tail = 0;
            int i = h;
            int mask = elements.length - 1;
            do 
            {
                elements[i] = 0;
                i = (i + 1) & mask;
            } 
            while (i != t);
        }
    }
    
    private void doubleCapacity() 
    {
        Assert.checkState(head == tail);
        
        int p = head;
        int n = elements.length;
        int r = n - p;
        int newCapacity = n << 1;
        Assert.checkState(newCapacity >= 0);
        
        int[] a = new int[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = n;
    }
}

