/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;




/**
 * The {@link RawPagePool} is a pool of pages.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPagePool
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private long maxPageIdlePeriod;
    private final SimpleDeque<RawPage> pages = new SimpleDeque<RawPage>();
    private long lastStatisticsTime;
    private int minCount;
    
    public RawPagePool(long maxPageIdlePeriod)
    {
        this.maxPageIdlePeriod = maxPageIdlePeriod;
    }
    
    public int getSize()
    {
        return pages.size();
    }
    
    public void setMaxPageIdlePeriod(long maxPageIdlePeriod)
    {
        this.maxPageIdlePeriod = maxPageIdlePeriod;
    }

    public void add(RawPage page)
    {
        Assert.notNull(page);

        pages.offer(page);
    }

    public RawPage remove()
    {
        if (pages.isEmpty())
            return null;
        
        RawPage page = pages.poll();
        
        if (minCount > pages.size())
            minCount = pages.size();
        
        return page;
    }
    
    public void close()
    {
        pages.clear();
        minCount = 0;
    }
    
    public void onTimer(long currentTime)
    {
        if (lastStatisticsTime == 0 || currentTime > lastStatisticsTime + maxPageIdlePeriod)
        {
            if (lastStatisticsTime > 0 && minCount > 0)
            {
                while (true)
                {
                    if (pages.isEmpty() || minCount == 0)
                        break;
                    
                    pages.poll();
                    minCount--;
                }
            }
            
            minCount = pages.size();
            lastStatisticsTime = currentTime;
        }
    }
    
    public String printStatistics()
    {
        return messages.statistics(pages.size()).toString();
    }

    private interface IMessages
    {
        @DefaultMessage("free pages count: {0}")
        ILocalizedMessage statistics(int freePagesCount);
    }
}
