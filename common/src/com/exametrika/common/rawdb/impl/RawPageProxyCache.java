/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;




/**
 * The {@link RawPageProxyCache} is a weak cache of page proxies.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPageProxyCache
{
    private final Map<PageId, Entry> proxies = new HashMap<PageId, Entry>();
    private final ReferenceQueue<RawPageProxy> queue = new ReferenceQueue<RawPageProxy>();
    
    public boolean isEmpty()
    {
        expungeStaleEntries();
        return proxies.isEmpty();
    }
    
    public int size()
    {
        expungeStaleEntries();
        return proxies.size();
    }
    
    public RawPageProxy get(int fileIndex, long pageIndex)
    {
        expungeStaleEntries();
        Entry entry = proxies.get(new PageId(fileIndex, pageIndex));
        if (entry != null)
            return entry.get();
        else
            return null;
    }
    
    public void put(int fileIndex, long pageIndex, RawPageProxy proxy)
    {
        expungeStaleEntries();
        PageId id = new PageId(fileIndex, pageIndex);
        Entry prev = proxies.put(id, new Entry(id, proxy, queue));
        delete(prev, true);
    }
    
    public void clear()
    {
        for (Entry entry : proxies.values())
            delete(entry, false);
        
        proxies.clear();
        expungeStaleEntries();
    }

    @Override
    public String toString()
    {
        return Strings.toString(proxies.entrySet(), false);
    }
    
    private void delete(Entry entry, boolean checkUnloaded)
    {
        if (entry == null)
            return;

        RawPageProxy prevProxy = entry.get();
        if (prevProxy == null)
            return;

        Assert.isTrue(!checkUnloaded || !prevProxy.isLoaded());
        prevProxy.setStale();
    }
    
    private void expungeStaleEntries()
    {
        while (true)
        {
            Entry entry = (Entry)queue.poll();
            if (entry == null)
                break;
            
            proxies.remove(entry.id);
        }
    }
    
    private static class PageId
    {
        private final int fileIndex;
        private final long pageIndex;
        
        public PageId(int fileIndex, long pageIndex)
        {
            this.fileIndex = fileIndex;
            this.pageIndex = pageIndex;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            else if (!(o instanceof PageId))
                return false;
            
            PageId id = (PageId)o;
            return fileIndex == id.fileIndex && pageIndex == id.pageIndex;
        }
        
        @Override
        public int hashCode()
        {
            return 31 * fileIndex + (int)(pageIndex ^ (pageIndex >>> 32));
        }
        
        @Override
        public String toString()
        {
            return fileIndex + "[" + pageIndex + "]"; 
        }
    }
    
    private static class Entry extends WeakReference<RawPageProxy>
    {
        private final PageId id;

        public Entry(PageId id, RawPageProxy referent, ReferenceQueue<RawPageProxy> queue)
        {
            super(referent, queue);
            
            this.id = id;
        }
        
        @Override
        public String toString()
        {
            RawPageProxy proxy = get();
            if (proxy != null)
                return Integer.toString(System.identityHashCode(proxy));
            else
                return "<null>";
        }
    }
}
