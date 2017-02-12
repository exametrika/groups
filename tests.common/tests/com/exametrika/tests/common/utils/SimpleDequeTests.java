/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.common.utils.SimpleDeque.IIterator;
import com.exametrika.common.utils.SimpleIntDeque;


/**
 * The {@link SimpleDequeTests} are tests for {@link SimpleDeque}.
 * @see SimpleDeque
 * 
 * @author Medvedev-A
 */
public class SimpleDequeTests
{
    @Test
    public void testDeque()
    {
        int COUNT = 10000;
        
        SimpleDeque<Integer> queue = new SimpleDeque<Integer>();
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (int i = 0; i < COUNT; i++)
            queue.offer(i);
        
        assertThat(queue.size(), is(COUNT));
        assertThat(queue.isEmpty(), is(false));
        
        for (int i = 0; i < COUNT; i++)
        {
            assertThat(queue.peek(), is(i));
            assertThat(queue.poll(), is(i));
        }
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (int i = 0; i < COUNT; i++)
            queue.offer((i % 2) == 1 ? i : null);
        
        assertThat(queue.size(), is(COUNT));
        assertThat(queue.isEmpty(), is(false));
        
        for (int i = 0; i < COUNT / 2; i++)
            assertThat(queue.pollIgnoreNulls(), is(2 * i + 1));
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (int i = 0; i < COUNT; i++)
            queue.offer((i % 2) == 1 ? i : null);
        
        assertThat(queue.size(), is(COUNT));
        assertThat(queue.isEmpty(), is(false));
        
        for (int i = 0; i < COUNT / 2; i++)
        {
            assertThat(queue.peekIgnoreNulls(), is(2 * i + 1));
            assertThat(queue.peekIgnoreNulls() == queue.peekIgnoreNulls(), is(true));
            assertThat(queue.peekIgnoreNulls() == queue.poll(), is(true));
        }
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (int i = 0; i < COUNT; i++)
            queue.offer(i);

        int i = 0;
        for (Integer v : queue)
        {
            assertThat(v, is(i));
            i++;
        }
        
        for (i = 0; i < COUNT; i++)
        {
            assertThat(queue.get(i), is(i));
            queue.set(i, i * 2);
            assertThat(queue.get(i), is(2 * i));
            queue.set(i, i);
        }
        
        i = 0;
        for (IIterator<Integer> it = queue.iterator(); it.hasNext();)
        {
            Integer v = it.next();
            assertThat(v, is(i));
            i++;
            it.set(null);
        }
        
        assertThat(queue.size(), is(COUNT));
        assertThat(queue.isEmpty(), is(false));
        
        for (Integer v : queue)
            assertThat(v, nullValue());
        
        assertThat(queue.pollIgnoreNulls(), nullValue());
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (i = 0; i < COUNT; i++)
            queue.offer(i);
        
        queue.clear();
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (i = 0; i < COUNT; i++)
        {
           queue.addFirst(i);
           assertThat(queue.getFirst(), is(i));
        }
        
        for (i = 0; i < COUNT; i++)
        {
            assertThat(queue.getLast(), is(i));
            assertThat(queue.removeLast(), is(i));
        }
    }
    
    @Test
    public void testIntDeque()
    {
        int COUNT = 10000;
        
        SimpleIntDeque queue = new SimpleIntDeque();
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (int i = 0; i < COUNT; i++)
            queue.addLast(i);
        
        assertThat(queue.size(), is(COUNT));
        assertThat(queue.isEmpty(), is(false));
        
        for (int i = 0; i < COUNT; i++)
        {
            assertThat(queue.getFirst(), is(i));
            assertThat(queue.removeFirst(), is(i));
        }
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (int i = 0; i < COUNT; i++)
            queue.addLast(i);
        
        queue.clear();
        
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));
        
        for (int i = 0; i < COUNT; i++)
        {
           queue.addFirst(i);
           assertThat(queue.getFirst(), is(i));
        }
        
        for (int i = 0; i < COUNT; i++)
        {
            assertThat(queue.getLast(), is(i));
            assertThat(queue.removeLast(), is(i));
        }
    }
}
