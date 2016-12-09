/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;




/**
 * The {@link SimpleList} is a simple doubly linked list.
 * 
 * @param <T> element type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SimpleList<T> implements Iterable<SimpleList.Element<T>>
{
    private final Element<T> head = new Element();
    
    public static class Element<T>
    {
        private final T value;
        private Element<T> next;
        private Element<T> prev;
        private Object reverse;
        
        public Element(T value)
        {
            this(value, null);
        }
        
        public Element(T value, Object reverse)
        {
            Assert.notNull(value);
            
            this.value = value;
            this.reverse = reverse;
            next = prev = this;
        }
        
        public final T getValue()
        {
            return value;
        }
        
        public final <V> V getReverse()
        {
            return (V)reverse;
        }
        
        public final Element<T> getNext()
        {
            return next;
        }
        
        public boolean isAttached()
        {
            return !(next == this && prev == this);
        }
        
        public final boolean isRemoved()
        {
            return prev == null;
        }
        
        public final void addBefore(Element<T> element)
        {
            Assert.notNull(element);
            Assert.isTrue(!element.isAttached());
            Assert.checkState(!isRemoved());
            
            element.next = this;
            element.prev = prev;
            prev.next = element;
            prev = element;
        }
        
        public final void addAfter(Element<T> element)
        {
            Assert.notNull(element);
            Assert.isTrue(!element.isAttached());
            Assert.checkState(!isRemoved());
            
            element.prev = this;
            element.next = next;
            next.prev = element;
            next = element;
        }
        
        public final void remove()
        {
            Assert.supports(value != null);
            if (!isAttached() || isRemoved())
                return;
            
            prev.next = next;
            next.prev = prev;
            
            prev = null;
        }
        
        public final void reset()
        {
            reset(null);
        }
        
        public final void reset(Object reverse)
        {
            Assert.checkState(!isAttached() || isRemoved());
            
            next = prev = this;
            this.reverse = reverse;
        }
        
        private Element()
        {
            value = null;
            next = prev = this;   
        }
    }
    
    public SimpleList()
    {
    }
    
    public SimpleList(Iterable<T> iterable)
    {
        for (T value : iterable)
            addLast(new Element<T>(value));
    }
    
    public boolean isEmpty()
    {
        return !head.isAttached();
    }
    
    public Element<T> getHead()
    {
        return head;
    }
    
    public Element<T> getFirst()
    {
        Assert.isTrue(!isEmpty());
        return head.next;
    }
    
    public Element<T> getLast()
    {
        Assert.isTrue(!isEmpty());
        return head.prev;
    }
    
    public Element<T> find(T value)
    {
        Assert.notNull(value);
        
        Element<T> element = head.next;
        while (element != head)
        {
            if (element.value.equals(value))
                return element;
            
            element = element.next;
        }
        
        return null;
    }
    
    public void addFirst(Element<T> element)
    {
        head.addAfter(element);
    }
    
    public void addLast(Element<T> element)
    {
        head.addBefore(element);
    }
    
    public void clear()
    {
        Element<T> element = head.next;
        while (element != head)
        {
            element.remove();
            element = element.next;
        }
    }

    public List<T> toList()
    {
        List<T> list = new ArrayList<T>();
        for (Element<T> element : this)
            list.add(element.value);
        
        return list;
    }
    
    public Iterable<T> values()
    {
        return new SimpleValueIterable();
    }
    
    public Iterable<T> reverseValues()
    {
        return new SimpleValueReverseIterable();
    }
    
    @Override
    public Iterator<Element<T>> iterator()
    {
        return new SimpleIterator(head.next);
    }
    
    public Iterator<Element<T>> reverseIterator()
    {
        return new SimpleReverseIterator(head.prev);
    }
    
    @Override
    public String toString()
    {
        return toList().toString();
    }
    
    private class SimpleIterator implements Iterator<Element<T>>
    {
        private Element<T> nextElement;
        private boolean removed;
        
        public SimpleIterator(Element<T> nextElement)
        {
            this.nextElement = nextElement;
        }
        
        @Override
        public boolean hasNext()
        {
            return nextElement != head;
        }

        @Override
        public Element<T> next()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            
            Element<T> element = nextElement;
            nextElement = nextElement.next;
            removed = false;
            return element;
        }

        @Override
        public void remove()
        {
            Assert.checkState(!removed && nextElement.prev != head);
            nextElement.prev.remove();
            removed = true;
        }
    }
    
    private class SimpleReverseIterator implements Iterator<Element<T>>
    {
        private Element<T> prevElement;
        private boolean removed;
        
        public SimpleReverseIterator(Element<T> prevElement)
        {
            this.prevElement = prevElement;
        }
        
        @Override
        public boolean hasNext()
        {
            return prevElement != head;
        }

        @Override
        public Element<T> next()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            
            Element<T> element = prevElement;
            prevElement = prevElement.prev;
            removed = false;
            return element;
        }

        @Override
        public void remove()
        {
            Assert.checkState(!removed && prevElement.next != head);
            prevElement.next.remove();
            removed = true;
        }
    }
    
    private class SimpleValueIterable implements Iterable<T>
    {
        @Override
        public Iterator<T> iterator()
        {
            return new SimpleValueIterator(SimpleList.this.iterator());
        }
    }
    
    private class SimpleValueReverseIterable implements Iterable<T>
    {
        @Override
        public Iterator<T> iterator()
        {
            return new SimpleValueIterator(SimpleList.this.reverseIterator());
        }
    }
    
    private class SimpleValueIterator implements Iterator<T>
    {
        private final Iterator<Element<T>> it;
        
        public SimpleValueIterator(Iterator<Element<T>> it)
        {
            this.it = it;
        }
        
        @Override
        public boolean hasNext()
        {
            return it.hasNext();
        }

        @Override
        public T next()
        {
            return it.next().value;
        }

        @Override
        public void remove()
        {
            it.remove();
        }
    }
}
