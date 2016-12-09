/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import com.exametrika.common.compartment.ICompartmentQueue.Event;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.rawdb.IRawBatchOperation;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawBatchLock;
import com.exametrika.common.rawdb.impl.RawDbBatchOperationState.PageCacheConstraint;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.SimpleList.Element;

/**
 * The {@link RawDbBatchOperation} is a batch operation.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class RawDbBatchOperation implements IRawOperation, IRawBatchControl
{
    private static final ILogger logger = Loggers.get(RawDbBatchOperation.class);
    private final RawBatchManager batchManager;
    private final RawTransactionManager transactionManager;
    private final RawPageTypeManager pageTypeManager;
    private final RawDbBatchOperationState state;
    private final long runPeriod;
    private final long idlePeriod;
    private final IRawBatchContext batchContext;
    private final SimpleList<Event> pendingTransactions = new SimpleList<Event>();
    private boolean validating;
    private boolean completed;
    private boolean running;
    private long startTime;
    private Iterator<Event> pendingTransactionIterator;
    private List<RawBatchLock> locks;
    
    public RawDbBatchOperation(RawBatchManager batchManager, RawTransactionManager transactionManager, RawPageTypeManager pageTypeManager,
        RawDbBatchOperationState state, boolean validating, long runPeriod, long idlePeriod)
    {
        Assert.notNull(batchManager);
        Assert.notNull(transactionManager);
        Assert.notNull(pageTypeManager);
        Assert.notNull(state);
        
        this.batchManager = batchManager;
        this.transactionManager = transactionManager;
        this.pageTypeManager = pageTypeManager;
        this.state = state;
        this.validating = validating;
        this.runPeriod = runPeriod;
        this.idlePeriod = idlePeriod;
        this.pendingTransactionIterator = pendingTransactions.reverseValues().iterator();
        this.locks = state.operation.getLocks();
        this.batchContext = batchManager.getBatchContext();
    }
    
    @Override
    public int getOptions()
    {
        return state.operation.getOptions();
    }
    
    @Override
    public int getSize()
    {
        return state.operation.getSize();
    }
    
    @Override
    public List<String> getBatchLockPredicates()
    {
        return null;
    }

    @Override
    public boolean isPageCachingEnabled()
    {
        return state.cachingEnabled;
    }

    @Override
    public void setMaxPageCacheSize(int pageTypeIndex, String category, long value)
    {
        RawPageType pageType = pageTypeManager.getPageType(pageTypeIndex);
        RawPageCache pageCache = pageType.getExistingPageCache(category);
        if (pageCache != null)
            pageCache.setBatchMaxPageCacheSize(value);
        
        if (state.constraints == null)
            state.constraints = new LinkedHashSet<PageCacheConstraint>();

        state.constraints.add(new PageCacheConstraint(pageTypeIndex, category, value));
    }

    @Override
    public void setPageCachingEnabled(boolean value)
    {
        state.cachingEnabled = value;
        
        RawTransaction transaction = transactionManager.getTransaction();
        Assert.checkState(transaction != null);
        
        transaction.setCachingEnabled(value);
    }

    @Override
    public int getNonCachedPagesInvalidationQueueSize()
    {
        return state.nonCachedPagesInvalidationQueueSize;
    }

    @Override
    public void setNonCachedPagesInvalidationQueueSize(int value)
    {
        state.nonCachedPagesInvalidationQueueSize = value;
        
        RawTransaction transaction = transactionManager.getTransaction();
        Assert.checkState(transaction != null);
        
        transaction.setNonCachedPagesInvalidationQueueSize(value);
    }
    
    public IRawBatchOperation getOperation()
    {
        return state.operation;
    }
    
    public Iterable<Event> getPendingTransactions()
    {
        return pendingTransactions.values();
    }
    
    public void addPendingTransaction(Event transaction)
    {
        Assert.notNull(transaction);
        
        pendingTransactions.addFirst(new Element<Event>(transaction));
    }
    
    public Event takePendingTransaction()
    {
        if (pendingTransactions.isEmpty())
            return null;
        
        if (locks != state.operation.getLocks())
        {
            pendingTransactionIterator = pendingTransactions.reverseValues().iterator();
            locks = state.operation.getLocks();
        }
        
        while (pendingTransactionIterator.hasNext())
        {
            Event pendingTransactionEvent = pendingTransactionIterator.next();
            IRawTransaction pendingTransaction = (IRawTransaction)pendingTransactionEvent.task;
            IRawOperation pendingOperation = pendingTransaction.getOperation();
            if (allow(pendingTransaction.isReadOnly(), pendingOperation))
            {
                pendingTransactionIterator.remove();
                return pendingTransactionEvent;
            }
        }

        return null;
    }

    @Override
    public boolean isCompleted()
    {
        return completed;
    }
    
    public boolean allow(boolean readOnly, IRawOperation operation)
    {
        for (RawBatchLock lock : state.operation.getLocks())
        {
            if (!lock.allow(readOnly, operation.getBatchLockPredicates()))
                return false;
        }
        
        return true;
    }
    
    @Override
    public boolean canContinue()
    {
        long currentTime = Times.getCurrentTime();
        
        if (running)
        {
            if (currentTime - startTime <= runPeriod)
                return true;
            else
                return false;
        }
        else
        {
            if (currentTime - startTime <= idlePeriod)
                return false;
            else
                return true;
        }
    }
    
    @Override
    public void onBeforeStarted(IRawTransaction transaction)
    {
        state.operation.setContext(batchContext);
        state.operation.onBeforeStarted(transaction);
    }
    
    @Override
    public void run(IRawTransaction transaction)
    {
        running = true;
        startTime = Times.getCurrentTime();
        
        ((RawTransaction)transaction).setCachingEnabled(state.cachingEnabled);
        ((RawTransaction)transaction).setNonCachedPagesInvalidationQueueSize(state.nonCachedPagesInvalidationQueueSize);
        enableConstraints(true);
        
        if (validating)
        {
            state.operation.validate(transaction);
            validating = false;
        }
        
        try
        {
            completed = state.operation.run(transaction, this);
        }
        catch (Exception e)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
            
            completed = true;
        }
        catch (Error e)
        {
            completed = true;
            throw e;
        }
        
        running = false;
        startTime = Times.getCurrentTime();
    }

    @Override
    public void validate()
    {
    }
    
    @Override
    public void onBeforeCommitted()
    {
        state.operation.onBeforeCommitted(completed);
        
        RawTransaction transaction = transactionManager.getTransaction();
        Assert.checkState(transaction != null);
        
        transaction.setCachingEnabled(true);
        transaction.setReadWrite();
        enableConstraints(false);
        
        if (completed)
            batchManager.getBatchOperationSpace().clearOperation();
        else
            batchManager.getBatchOperationSpace().setOperation(batchManager.getBatchContext(), state);
    }
    
    @Override
    public void onCommitted()
    {
        state.operation.onCommitted(completed);
    }

    @Override
    public boolean onBeforeRolledBack()
    {
        return state.operation.onBeforeRolledBack();
    }
    
    @Override
    public void onRolledBack(boolean clearCache)
    {
        enableConstraints(false);
        state.operation.onRolledBack(clearCache);
        completed = true;
    }
    
    private void enableConstraints(boolean enable)
    {
        if (state.constraints == null)
            return;
        
        for (PageCacheConstraint constraint : state.constraints)
        {
            RawPageType pageType = pageTypeManager.getPageType(constraint.pageTypeIndex);
            RawPageCache pageCache = pageType.getExistingPageCache(constraint.category);
            if (pageCache != null)
                pageCache.setBatchMaxPageCacheSize(enable ? constraint.maxPageCacheSize : Long.MAX_VALUE);
        }
    }
}