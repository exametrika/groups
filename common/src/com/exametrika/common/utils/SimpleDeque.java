/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;



/**
 * The {@link SimpleDeque} is a simple array based deque.
 * 
 * @param <T> type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SimpleDeque<T> implements Iterable<T>
{
    private T[] elements;
    private int head;
    private int tail;
    private int modCount;
    
    public interface IIterator<T> extends Iterator<T>
    {
        void set(T e);
    }
    
    public SimpleDeque()
    {
        this(16);
    }
    
    public SimpleDeque(int capacity)
    {
        elements = (T[])new Object[Numbers.roundToPowerOfTwo(capacity)];
    }
    
    public boolean isEmpty() 
    {
        return head == tail;
    }
    
    public int size()
    {
        return (tail - head) & (elements.length - 1);
    }
    
    public T getFirst()
    {
        return elements[head];
    }
    
    public T getLast()
    {
        return elements[(tail - 1) & (elements.length - 1)];
    }
    
    public T get(int pos)
    {
        return elements[pos & (elements.length - 1)];
    }
    
    public void set(int pos, T value)
    {
        elements[pos & (elements.length - 1)] = value;
    }
    
    public void addFirst(T value) 
    {
        head = (head - 1) & (elements.length - 1);
        elements[head] = value;
        if (head == tail)
            doubleCapacity();
        
        modCount++;
    }
    
    public void addLast(T value)
    {
        elements[tail] = value;
        tail = (tail + 1) & (elements.length - 1);
        if (tail == head)
            doubleCapacity();
        
        modCount++;
    }

    public T removeFirst()
    {
        if (isEmpty())
            return null;
        
        int h = head;
        T result = elements[h];
        
        elements[h] = null;
        head = (h + 1) & (elements.length - 1);
        modCount++;

        return result;
    }
    
    public T removeLast() 
    {
        if (isEmpty())
            return null;
        
        int t = (tail - 1) & (elements.length - 1);
        T result = elements[t];
        
        elements[t] = null;
        tail = t;
        modCount++;
        
        return result;
    }
    
    public void offer(T value)
    {
        addLast(value);
    }
    
    public T peek()
    {
        return elements[head];
    }
    
    public T peekIgnoreNulls()
    {
        modCount++;
        
        while (head != tail)
        {
            int h = head;
            
            T result = elements[h];
            if (result != null)
                return result;
            
            head = (h + 1) & (elements.length - 1);
        }
        
        return null;
    }
    
    public T poll()
    {
        return removeFirst();
    }
    
    public T pollIgnoreNulls()
    {
        modCount++;
        
        while (head != tail)
        {
            int h = head;
            head = (h + 1) & (elements.length - 1);
            
            T result = elements[h];
            if (result == null)
                continue;
            
            elements[h] = null;
            
            return result;
        }
        
        return null;
    }
    
    public void clear() 
    {
        modCount++;
        int h = head;
        int t = tail;
        if (h != t) 
        {
            head = tail = 0;
            int i = h;
            int mask = elements.length - 1;
            do 
            {
                elements[i] = null;
                i = (i + 1) & mask;
            } 
            while (i != t);
        }
    }
    
    @Override
    public IIterator<T> iterator()
    {
        return new SimpleDequeIterator();
    }
    
    private void doubleCapacity() 
    {
        Assert.checkState(head == tail);
        
        int p = head;
        int n = elements.length;
        int r = n - p;
        int newCapacity = n << 1;
        Assert.checkState(newCapacity >= 0);
        
        T[] a = (T[])new Object[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = n;
    }
    
    private class SimpleDequeIterator implements IIterator<T> 
    {
        private int pos = head;
        private int modCount = SimpleDeque.this.modCount;
        private int lastPos = -1;

        @Override
        public boolean hasNext() 
        {
            return pos != tail;
        }

        @Override
        public T next() 
        {
            if (pos == tail)
                throw new NoSuchElementException();
            
            if (modCount != SimpleDeque.this.modCount)
                throw new ConcurrentModificationException();
            
            T result = elements[pos];
            lastPos = pos;
            
            pos = (pos + 1) & (elements.length - 1);
            return result;
        }

        @Override
        public void set(T e)
        {
            Assert.checkState(lastPos != -1);
            
            elements[lastPos] = e;
        }

        @Override
        public void remove() 
        {
            Assert.supports(false);
        }
    }
}

