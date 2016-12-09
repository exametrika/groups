/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.util.List;



/**
 * The {@link IRawBatchOperation} represents a batch (long running) operation executed in transaction cooperatively 
 * with other (ordinary) transactions. Batch operation is executed in several steps. On first step operation validation
 * is performed, only on this step transaction can be rolled back. On other steps operation is running by time slices, 
 * allowing other ordinary transactions to execute. If process failure is occured, transaction is restarted from last 
 * successfully executed step. All exceptions during run steps are considered as successfull operation completion 
 * because transaction can not be rolled back on run steps.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author AndreyM
 */
public interface IRawBatchOperation
{
    /**
     * Returns operation transaction options as defined in {@link IRawOperation}.
     *
     * @return operation transaction options
     */
    int getOptions();
    
    /**
     * Returns estimated operation size.
     *
     * @return estimated operation size
     */
    int getSize();
    
    /**
     * Returns locks of batch operation. Batch locks can be changed dynamically during course of execution of batch operation.
     * Using this method batch operation can dynamically updates list of required
     * locks allowing or denying cooperative execution of other transactions.
     *
     * @return locks of batch operation or empty list if batch operation does not require locks
     */
    List<RawBatchLock> getLocks();
    
    /**
     * Sets batch context.
     *
     * @param context batch context
     */
    void setContext(IRawBatchContext context);
    
    /**
     * Called when transaction is about to be started.
     * 
     * @param transaction enclosing transaction
     */
    void onBeforeStarted(IRawTransaction transaction);
    
    /**
     * Checks batch preconditions. Check operation must be small enough, because it blocks all other transactions.
     *
     * @param transaction transaction
     * @exception RawRollbackException (or any other exception) if batch transaction is rolled back
     */
    void validate(IRawTransaction transaction);
    
    /**
     * Runs operation in transaction.
     *
     * @param transaction enclosing transaction
     * @param batchControl batch control
     * @return true if operation has been completed, false if additional operation steps are required
     * @exception RawRollbackException (or any other exception) operation is considered as successfully completed
     */
    boolean run(IRawTransaction transaction, IRawBatchControl batchControl);
    
    /**
     * Called when pages of write transaction representing single batch step are about to be committed. Can be used for flushing cached data to pages before commit.
     * 
     * @param completed if true last run step of write batch transaction is about to be successfully committed
     */
    void onBeforeCommitted(boolean completed);
    
    /**
     * Called when read-only or write transaction representing single batch step is successfully committed.
     * 
     * @param completed if true last run step of write batch transaction is successfully committed
     */
    void onCommitted(boolean completed);

    /**
     * Called when validation step of write batch transaction failed and transaction is rolled back.
     * 
     * @return if true all internal caches of operation must be cleared
     */
    boolean onBeforeRolledBack();
    
    /**
     * Called when validation step of write batch transaction failed and transaction is rolled back.
     * 
     * @param clearCache if true all internal caches of operation must be cleared
     */
    void onRolledBack(boolean clearCache);
}
