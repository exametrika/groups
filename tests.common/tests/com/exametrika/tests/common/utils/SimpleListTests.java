/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;


/**
 * The {@link SimpleListTests} are tests for {@link SimpleList}.
 * @see SimpleList
 * 
 * @author Medvedev-A
 */
public class SimpleListTests
{
    @Test
    public void testList() throws Throwable
    {
        final SimpleList<String> list = new SimpleList<String>();
        assertThat(list.isEmpty(), is(true));
        
        final Element element1 = new Element("1");
        element1.remove();
        Element element2 = new Element("2");
        Element element3 = new Element("3");
        Element element4 = new Element("4");
        final Element element5 = new Element("5");
        final Element element6 = new Element("5");
        
        list.addLast(element1);
        list.addLast(element2);
        list.addFirst(element3);
        
        element1.addBefore(element4);
        element1.addAfter(element5);
        
        assertThat(list.find("4") == element4, is(true));
        assertThat(list.find("10"), nullValue());
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                element1.addBefore(element5);
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                element1.addAfter(element5);
            }
        });
        
        checkStructure(list);
        check(list, Arrays.asList(element3, element4, element1, element5, element2));
        
        assertThat(list.toList(), is(Arrays.<String>asList("3", "4", "1", "5", "2")));
        
        assertThat(list.isEmpty(), is(false));
        
        element3.remove();
        element2.remove();
        element1.remove();
        element1.remove();
        
        assertThat(element1.isRemoved(), is(true));
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                element1.addBefore(element6);
            }
        });
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                element1.addAfter(element6);
            }
        });
        
        new Expected(UnsupportedOperationException.class, new Runnable()
        {
            @Override
            public void run()
            {
                list.getHead().remove();
            }
        });
        
        checkStructure(list);
        check(list, Arrays.asList(element4, element5));
        
        element4.remove();
        element5.remove();
        
        checkStructure(list);
        check(list, Arrays.asList());
        
        assertThat(list.isEmpty(), is(true));
        
        Element element10 = new Element("10");
        Element element11 = new Element("11");
        list.addLast(element10);
        list.addLast(element11);
        list.clear();
        
        assertThat(list.isEmpty(), is(true));
        assertThat(element10.isRemoved(), is(true));
        assertThat(element11.isRemoved(), is(true));
    }
    
    @Test
    public void testIterator() throws Throwable
    {
        SimpleList<String> list = new SimpleList<String>();
        
        final Iterator<Element<String>> it = list.iterator();
        assertThat(it.hasNext(), is(false));
        
        new Expected(NoSuchElementException.class, new Runnable()
        {
            @Override
            public void run()
            {
                it.next();
            }
        });
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                it.remove();
            }
        });
        
        Element element1 = new Element("1");
        final Element element2 = new Element("2");
        
        list.addLast(element1);
        list.addLast(element2);

        checkStructure(list);
        
        final Iterator<Element<String>> it2 = list.iterator();
        assertThat(it2.next() == element1, is(true));
        it2.remove();
        checkStructure(list);
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                it2.remove();
            }
        });
        
        element1.reset();
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                element2.reset();
            }
        });
        
        assertThat(it2.next() == element2, is(true));
        it2.remove();
        checkStructure(list);
        element2.reset();
        
        assertThat(it2.hasNext(), is(false));
        assertThat(list.isEmpty(), is(true));
        
        list.addLast(element1);
        list.addLast(element2);
        
        List<Element<String>> l = new ArrayList<Element<String>>();
        for (Iterator<Element<String>> it3 = list.iterator(); it3.hasNext();)
        {
            Element<String> element = it3.next();
            it3.remove();
            element.reset();
            l.add(element);
            
            assertThat(element.isAttached(), is(false));
            assertThat(element.isRemoved(), is(false));
        }
        
        assertThat(list.isEmpty(), is(true));
        assertThat(l, is((List)Arrays.asList(element1, element2)));
    }
    
    @Test
    public void testReverseIterator() throws Throwable
    {
        SimpleList<String> list = new SimpleList<String>();
        
        final Iterator<Element<String>> it = list.reverseIterator();
        assertThat(it.hasNext(), is(false));
        
        new Expected(NoSuchElementException.class, new Runnable()
        {
            @Override
            public void run()
            {
                it.next();
            }
        });
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                it.remove();
            }
        });
        
        final Element element1 = new Element("1");
        Element element2 = new Element("2");
        
        list.addLast(element1);
        list.addLast(element2);

        checkStructure(list);
        
        final Iterator<Element<String>> it2 = list.reverseIterator();
        assertThat(it2.next() == element2, is(true));
        it2.remove();
        checkStructure(list);
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                it2.remove();
            }
        });
        
        element2.reset();
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                element1.reset();
            }
        });
        
        assertThat(it2.next() == element1, is(true));
        it2.remove();
        checkStructure(list);
        element1.reset();
        
        assertThat(it2.hasNext(), is(false));
        assertThat(list.isEmpty(), is(true));
        
        list.addLast(element1);
        list.addLast(element2);
        
        List<Element<String>> l = new ArrayList<Element<String>>();
        for (Iterator<Element<String>> it3 = list.reverseIterator(); it3.hasNext();)
        {
            Element<String> element = it3.next();
            it3.remove();
            element.reset();
            l.add(element);
            
            assertThat(element.isAttached(), is(false));
            assertThat(element.isRemoved(), is(false));
        }
        
        assertThat(list.isEmpty(), is(true));
        assertThat(l, is((List)Arrays.asList(element2, element1)));
    }
    
    private void check(SimpleList list1, List list2)
    {
        List list = new ArrayList();
        for (Object e : list1)
            list.add(e);
        
        assertThat(list, is(list2));
    }
    
    private void checkStructure(SimpleList list) throws Throwable
    {
        Element element = list.getHead();
        while (true)
        {
            Element prev = Tests.get(element, "prev");
            assertThat(prev.getNext() == element, is(true));
            assertThat(Tests.get(element.getNext(), "prev") == element, is(true));
            
            element = element.getNext();
            
            if (element == list.getHead())
                break;
        }
    }
}
