/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio.socket;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import com.exametrika.common.utils.Assert;


/**
 * The {@link TcpSelector} is an implementation of {@link ITcpSelector}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpSelector implements ITcpSelector
{
    private final Selector selector;

    public TcpSelector() throws IOException
    {
        this(Selector.open());
    }
    
    public Selector getSelector()
    {
        return selector;
    }
    
    @Override
    public boolean isOpen()
    {
        return selector.isOpen();
    }
    
    @Override
    public Set<SelectionKey> keys()
    {
        return selector.keys();
    }
    
    @Override
    public Set<SelectionKey> selectedKeys()
    {
        return selector.selectedKeys();
    }
    
    @Override
    public int selectNow() throws IOException
    {
        return selector.selectNow();
    }
    
    @Override
    public int select(long timeout) throws IOException
    {
        return selector.select(timeout);
    }
    
    @Override
    public int select() throws IOException
    {
        return selector.select();
    }
    
    @Override
    public void wakeup()
    {
        selector.wakeup();
    }
    
    @Override
    public void close() throws IOException
    {
        selector.close();
    }
    
    private TcpSelector(Selector selector)
    {
        Assert.notNull(selector);
        
        this.selector = selector;
    }
}
