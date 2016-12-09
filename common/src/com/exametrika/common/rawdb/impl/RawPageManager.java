/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.impl.RawTransactionLog.FlushInfo;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;



/**
 * The {@link RawPageManager} is a page manager.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPageManager implements ITimerListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final RawDatabase database;
    private final Timer timer;
    private final List<RawDataFile> writeFiles = new ArrayList<RawDataFile>();
    private final List<RawDataFile> committedFiles = new ArrayList<RawDataFile>();
    private final List<RawDataFile> flushedFiles = new ArrayList<RawDataFile>();
    private final SimpleList<RawPage> writePages = new SimpleList<RawPage>();
    private final SimpleList<RawPage> committedPages = new SimpleList<RawPage>();
    private long lastFlushToMemoryTime;
    private long lastFlushToDiskTime;
    private final List<FlushInfo> flushedPages = new ArrayList<FlushInfo>();
    private boolean bigTransaction;
    private boolean asyncFlush;
    private long flushPeriod;
    private long maxFlushSize;
    private boolean flushDisabled;
    private volatile long flushCount;
    private long committedSize;
    private volatile long flushSize;
    
    public RawPageManager(RawDatabase database, RawDatabaseConfiguration configuration)
    {
        Assert.notNull(database);
        Assert.notNull(configuration);
        
        this.database = database;
        
        this.flushPeriod = configuration.getFlushPeriod();
        this.maxFlushSize = configuration.getMaxFlushSize();
        
        timer = new Timer(configuration.getTimerPeriod(), this, false, "[" + configuration.getName() + "] page flush thread", null);
    }
    
    public Timer getTimer()
    {
        return timer;
    }
    
    public RawDatabase getDatabase()
    {
        return database;
    }
    
    public void setFlushDisabled(boolean value)
    {
        synchronized (flushedPages)
        {
            flushDisabled = value;
        }
    }
    
    public long getFlushCount()
    {
        return flushCount;
    }
    
    public void setTimerPeriod(long timerPeriod)
    {
        timer.setPeriod(timerPeriod);
    }

    public void setFlushPeriod(long flushPeriod)
    {
        this.flushPeriod = flushPeriod; 
    }

    public void setMaxFlushSize(long maxFlushSize)
    {
        this.maxFlushSize = maxFlushSize;
    }

    public void addWriteFile(RawDataFile file)
    {
        writeFiles.add(file);
    }

    public void addWritePage(RawPage page)
    {
        page.getWriteElement().reset();
        writePages.addLast(page.getWriteElement());
    }
    
    public void begin(int options)
    {
        if ((options & IRawOperation.FLUSH) != 0 || ((options & IRawOperation.DURABLE) != 0 && asyncFlush))
            flush(true);
    }
    
    public void commit(int options)
    {
        boolean flush = bigTransaction | ((options & IRawOperation.FLUSH) != 0);
        bigTransaction = false;
        
        if (!writeFiles.isEmpty())
        {
            flushPendingPages(true);
            
            for (RawDataFile file : writeFiles)
                file.commit();
            
            committedFiles.addAll(writeFiles);
            
            writeFiles.clear();
            flush = true;
        }
        
        for (RawPage page : writePages.values())
        {
            page.commit();
            
            Element<RawPage> committedElement = page.getCommittedElement();
            if (!committedElement.isAttached() || committedElement.isRemoved())
            {
                committedElement.reset();
                committedPages.addLast(committedElement);
                committedSize += page.getSize();
            }
        }
        
        writePages.clear();
        
        database.getFileCache().commit();
        
        if (flush)
            flush(true);
        else if ((options & IRawOperation.DURABLE) != 0)
        {
            if (!committedPages.isEmpty())
            {
                List<FlushInfo> durablePages = new ArrayList<FlushInfo>();
                for (RawPage page : committedPages.values())
                {
                    FlushInfo info = page.flush(true);
                    if (info != null)
                        durablePages.add(info);
                }
                    
                database.getTransactionLog().flushRedo(durablePages);
                committedSize = 0;
            }
            
            asyncFlush = false;
        }
        else if (flushSize + committedSize > maxFlushSize)
            flush(false);
    }

    public boolean rollback(boolean clearCache)
    {
        for (RawDataFile file : writeFiles)
            file.rollback();
        
        writeFiles.clear();
        
        database.getFileCache().rollback();
        
        if (!clearCache && !bigTransaction)
        {
            for (RawPage page : writePages.values())
                page.rollback();

            writePages.clear();
            
            return false;
        }
        else
        {
            flush(true);
            
            if (clearCache)
                database.getPageTypeManager().close();
            else
                database.getPageTypeManager().clear();

            database.getFileCache().close();
            
            database.getTransactionLog().recover();
            
            bigTransaction = false;
            
            return true;
        }
    }
    
    public void flush(boolean sync)
    {
        if (!committedFiles.isEmpty() || !committedPages.isEmpty())
        {
            long flushSize;
            synchronized (flushedPages)
            {
                flushedFiles.addAll(committedFiles);
                committedFiles.clear();
                
                for (RawPage page : committedPages.values())
                    flushedPages.add(page.flush(false));
        
                this.flushSize += committedSize;
                committedPages.clear();
                committedSize = 0;
                asyncFlush = true;
                flushSize = this.flushSize;
            }
            
            if (!sync && flushSize > maxFlushSize)
                timer.signal();
        }
        
        lastFlushToMemoryTime = database.getCurrentTime();

        if (sync)
            flushPendingPages(true);
    }
    
    public void addFlushedPages(List<FlushInfo> pages)
    {
        synchronized (flushedPages)
        {
            flushedPages.addAll(pages);
            bigTransaction = true;
        }
    }
    
    public void flushPendingPages(boolean full)
    {
        synchronized (flushedPages)
        {
            if (flushDisabled)
                return;
            
            if (!flushedPages.isEmpty() || !flushedFiles.isEmpty())
            {
                database.getTransactionLog().flush(flushedPages, flushedFiles, full);
                flushedPages.clear();
                flushedFiles.clear();
                
                flushCount++;
                flushSize = 0;
            }
            
            asyncFlush = false;
        }
    }
    
    public void onTimer(long currentTime)
    {
        if (lastFlushToMemoryTime == 0 || currentTime - lastFlushToMemoryTime >= flushPeriod)
        {
            flush(false);
        
            lastFlushToMemoryTime = currentTime;
        }
    }
    
    @Override
    public void onTimer()
    {
        long currentTime = database.getCurrentTime();
        
        if (lastFlushToDiskTime == 0 || currentTime - lastFlushToDiskTime >= flushPeriod || flushSize > maxFlushSize)
        {
            flushPendingPages(true);
            lastFlushToDiskTime = currentTime;
        }
    }
    
    public void start()
    {
        timer.start();
    }
    
    public void stop()
    {
        setFlushDisabled(true);
        timer.stop();
        setFlushDisabled(false);
    }
    
    public void close()
    {
        writeFiles.clear();
        committedFiles.clear();
        writePages.clear();
        committedPages.clear();
    }
    
    public String printStatistics()
    {
        return messages.statistics(flushCount).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("page manager - flush count: {0}")
        ILocalizedMessage statistics(long flushCount);
    }
}
