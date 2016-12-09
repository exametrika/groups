/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.compartment.ICompartmentTaskSize;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawFileNotFoundException;
import com.exametrika.common.rawdb.RawPageNotFoundException;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.common.utils.Times;

/**
 * The {@link RawTransaction} is an implementation of {@link IRawTransaction}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawTransaction implements IRawTransaction, Runnable, ICompartmentTaskSize
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(RawTransaction.class);
    private int options;
    private final IRawOperation operation;
    private final RawTransactionManager transactionManager;
    private final RawDatabase database;
    private final Object sync;
    private RawPage lastPage;
    private Map<String, Object> parameters;
    private boolean completed;
    private Throwable exception;
    private boolean cachingEnabled = true;
    private int nonCachedPagesInvalidationQueueSize = 10;
    private SimpleDeque<RawPage> nonCachedPagesInvalidationQueue = new SimpleDeque<RawPage>();
    private long time;

    public RawTransaction(IRawOperation operation, RawTransactionManager transactionManager, Object sync)
    {
        Assert.notNull(operation);
        Assert.notNull(transactionManager);

        this.options = operation.getOptions();
        this.operation = operation;
        this.transactionManager = transactionManager;
        this.database = transactionManager.getDatabase();
        this.sync = sync;
    }

    @Override
    public int getSize()
    {
        return operation.getSize();
    }

    @Override
    public boolean isReadOnly()
    {
        return (options & IRawOperation.READ_ONLY) != 0;
    }
    
    @Override
    public boolean isCompleted()
    {
        return completed;
    }
    
    @Override
    public RawDatabase getDatabase()
    {
        return database;
    }
    
    @Override
    public IRawOperation getOperation()
    {
        return operation;
    }
    
    @Override
    public long getTime()
    {
        if (time == 0)
            time = Times.getCurrentTime();
        
        return time;
    }
    
    @Override
    public Map<String, Object> getParameters()
    {
        if (parameters == null)
            parameters = new LinkedHashMap<String, Object>();
        
        return parameters;
    }
    
    @Override
    public boolean isFileBound(int fileIndex)
    {
        Assert.checkState(!database.isStopped());
        
        return database.getFileCache().isFileBound(fileIndex);
    }
    
    @Override
    public RawDataFile bindFile(int fileIndex, RawBindInfo bindInfo)
    {
        Assert.notNull(bindInfo);
        
        Assert.checkState(!database.isStopped());
        
        RawDataFile file = database.getFileCache().bindFile(fileIndex, isReadOnly(), bindInfo);
        if (file != null)
            return file;
        else
            throw new RawFileNotFoundException();
    }
    
    @Override
    public void unbindFile(int fileIndex)
    {
        Assert.checkState(!database.isStopped());
        
        database.getFileCache().unbindFile(fileIndex);
    }
    
    @Override
    public IRawDataFile getFile(int fileIndex)
    {
        Assert.checkState(!database.isStopped());
        
        RawDataFile file = database.getFileCache().getFile(fileIndex, isReadOnly());
        if (file != null)
            return file;
        else
            throw new RawFileNotFoundException();
    }
    
    @Override
    public RawPageProxy getPage(int fileIndex, long pageIndex)
    {
        if (lastPage != null && !lastPage.isStale() && lastPage.getFile().getIndex() == fileIndex && lastPage.getIndex() == pageIndex)
            return lastPage.getProxy();

        return getPageFromFile(fileIndex, pageIndex, null).getProxy();
    }

    @Override
    public void run()
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.transactionStarted());
        
        boolean interceptResult = RawDatabaseInterceptor.INSTANCE.onTransactionStarted(database.getInterceptId());
        transactionManager.setTransaction(this);
        
        boolean run = false;
        Throwable exception = null;
        boolean completed = false;
        boolean readOnly = isReadOnly();
        boolean clearCache = false;
        
        try
        {
            operation.onBeforeStarted(this);
            
            database.getPageManager().begin(options);
                
            operation.run(this);
            
            if (!readOnly)
                operation.validate();
            
            run = true;
            
            if (!readOnly)
                commit();
            else
                operation.onCommitted();
            
            completed = operation.isCompleted();
            
            if (interceptResult)
                RawDatabaseInterceptor.INSTANCE.onTransactionCommitted(database.getInterceptId());
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, database.getMarker(), messages.transactionCommitted());
        }
        catch (Throwable e)
        {
            if (e instanceof RawClearCacheException)
                clearCache = true;
            else
            {
                exception = e;
                
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, database.getMarker(), e);
            }
            
            completed = true;
            
            if (!run)
            {
                try
                {
                    if (!readOnly)
                        clearCache = rollback(clearCache);
                    else
                        operation.onCommitted();
                }
                catch (Throwable t)
                {
                    exception = t;
                    completed = true;
                    
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, database.getMarker(), t);
                }
            }
            
            if (interceptResult)
                RawDatabaseInterceptor.INSTANCE.onTransactionRolledBack(database.getInterceptId(), exception);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, database.getMarker(), messages.transactionRolledBack());
        }
        
        transactionManager.setTransaction(null);
        
        while (!nonCachedPagesInvalidationQueue.isEmpty())
            nonCachedPagesInvalidationQueue.poll().setStale();

        if (clearCache)
        {
            database.getPagePool().close();
            database.getProxyCache().clear();
        }
        
        if (completed)
        {
            if (sync != null)
                notifyCompleted(exception);
            else
                this.completed = true;
        }
    }

    public void waitCompleted()
    {
        synchronized (sync)
        {
            while (!completed && !transactionManager.isClosed())
            {
                try
                {
                    sync.wait();
                }
                catch (InterruptedException e)
                {
                    throw new ThreadInterruptedException(e);
                }
            }
            
            if (completed)
            {
                if (exception != null)
                    throw new RawDatabaseException(exception);
            }
            else 
                Assert.checkState(!transactionManager.isClosed());
        }
    }

    @Override
    public String toString()
    {
        return operation.toString();
    }
    
    public boolean isCachingEnabled()
    {
        return cachingEnabled;
    }
    
    public void setCachingEnabled(boolean value)
    {
        cachingEnabled = value;
    }

    public void setNonCachedPagesInvalidationQueueSize(int value)
    {
        Assert.isTrue(value > 0);
        nonCachedPagesInvalidationQueueSize = value;
        
        while (nonCachedPagesInvalidationQueue.size() > nonCachedPagesInvalidationQueueSize)
            nonCachedPagesInvalidationQueue.poll().setStale();
    }

    public void setReadWrite()
    {
        options &= ~IRawOperation.READ_ONLY;
    }

    public RawPage getPageFromFile(int fileIndex, long pageIndex, RawPageProxy proxy)
    {
        Assert.checkState(!database.isStopped());
        
        RawDataFile file = database.getFileCache().getFile(fileIndex, isReadOnly());
        if (file == null)
            throw new RawFileNotFoundException();
        RawPage page = file.getPage(pageIndex, isReadOnly(), cachingEnabled, proxy);
        if (page == null)
            throw new RawPageNotFoundException();
        
        if (page.isCached())
        {
            page.refresh();
            lastPage = page;
        }
        else
            addNonCachedPage(page);
        return page;
    }

    private void notifyCompleted(Throwable e)
    {
        synchronized (sync)
        {
            exception = e;
            completed = true;
            sync.notifyAll();
        }
    }

    private void commit()
    {
        operation.onBeforeCommitted();
        
        database.getPageManager().commit(options);
        lastPage = null;
        
        operation.onCommitted();
    }

    private boolean rollback(boolean clearCache)
    {
        clearCache = operation.onBeforeRolledBack() || clearCache;
        
        while (!nonCachedPagesInvalidationQueue.isEmpty())
            nonCachedPagesInvalidationQueue.poll().setStale();
        
        clearCache = database.getPageManager().rollback(clearCache);
        lastPage = null;
        
        operation.onRolledBack(clearCache);
        
        if (clearCache)
            database.getBatchManager().clearCache();
        
        return clearCache;
    }
    
    private void addNonCachedPage(RawPage page)
    {
        nonCachedPagesInvalidationQueue.offer(page);
        if (nonCachedPagesInvalidationQueue.size() > nonCachedPagesInvalidationQueueSize)
            nonCachedPagesInvalidationQueue.poll().setStale();
    }
    
    private interface IMessages
    {
        @DefaultMessage("Transaction is started.")
        ILocalizedMessage transactionStarted();
        
        @DefaultMessage("Transaction is committed.")
        ILocalizedMessage transactionCommitted();
        
        @DefaultMessage("Transaction is rolled back.")
        ILocalizedMessage transactionRolledBack();
    }
}
