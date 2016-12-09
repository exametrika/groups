/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.rawdb.IRawBatchOperation;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.utils.Assert;



/**
 * The {@link RawTransactionManager} is used to manage database transactions.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawTransactionManager
{
    private final RawDatabase database;
    private final ICompartment compartment;
    private final Object sync = new Object();
    private boolean closed;
    private RawTransaction transaction;
    
    public RawTransactionManager(RawDatabase database, ICompartment compartment)
    {
        Assert.notNull(database);
        Assert.notNull(compartment);
        
        this.database = database;
        this.compartment = compartment;
    }
    
    public RawDatabase getDatabase()
    {
        return database;
    }
    
    public boolean isClosed()
    {
        return closed;
    }
    
    public RawTransaction getTransaction()
    {
        return transaction;
    }
    
    public void setTransaction(RawTransaction transaction)
    {
        this.transaction = transaction;
    }
    
    public void close()
    {
        synchronized (sync)
        {
            closed = true;
            sync.notifyAll();
        }
    }

    public void transaction(IRawOperation operation)
    {
        RawTransaction transaction = new RawTransaction(operation, this, null);
        compartment.offer(transaction);
    }
    
    public void transaction(List<IRawOperation> operations)
    {
        final List<Runnable> transactions = new ArrayList<Runnable>(operations.size());
        for (IRawOperation operation : operations)
            transactions.add(new RawTransaction(operation, this, null));
        
        compartment.offer(transactions);
    }
    
    public void transactionSync(IRawOperation operation)
    {
        RawTransaction transaction = new RawTransaction(operation, this, sync);
        compartment.offer(transaction);
        
        transaction.waitCompleted();
    }
    
    public void transaction(IRawBatchOperation operation)
    {
        RawDatabaseConfiguration configuration = database.getConfiguration();
        RawTransaction transaction = new RawTransaction(new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(operation), true, 
            configuration.getBatchRunPeriod(), configuration.getBatchIdlePeriod()), this, null);
        compartment.offer(transaction);
    }
    
    public void transactionSync(IRawBatchOperation operation)
    {
        RawDatabaseConfiguration configuration = database.getConfiguration();
        RawTransaction transaction = new RawTransaction(new RawDbBatchOperation(database.getBatchManager(), 
            database.getTransactionManager(), database.getPageTypeManager(), new RawDbBatchOperationState(operation), 
            true, configuration.getBatchRunPeriod(), configuration.getBatchIdlePeriod()), this, sync);
        compartment.offer(transaction);
        
        transaction.waitCompleted();
    }
}
