/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.util.List;



/**
 * The {@link IRawOperation} represents an operation executed in transaction.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawOperation
{
    /** Is transaction read-only or read-write transaction? */
    int READ_ONLY = 0x1;
    
    /** This option requires transaction to make all transaction changes durable before commit completes. */
    int DURABLE = 0x2;
    
    /** This option requires transaction to flush all pending changes to disk before transaction start and after commit. */
    int FLUSH = 0x4;
    
    /**
     * Returns operation transaction options.
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
     * Is multistep operation completed?
     *
     * @return true if multistep operation is completed
     */
    boolean isCompleted();
    
    /**
     * Returns batch lock predicates. Lock predicate allows to control isolation between ordinary transactions and batch transaction. 
     * If ordinary transaction and batch transaction have intersecting lock perdicates
     * ordinary transaction is blocked until batch transaction has been completed. Two lock predicates are block each other if they
     * are equal or one lock predicate is prefix of another lock predicate.
     *
     * @return batch lock predicates or null if lock of entire database is requested
     */
    List<String> getBatchLockPredicates();
    
    /**
     * Called when transaction is about to be started.
     * 
     * @param transaction enclosing transaction
     * @exception RawRollbackException (or any other exception) if transaction is rolled back
     */
    void onBeforeStarted(IRawTransaction transaction);
    
    /**
     * Runs operation in transaction.
     *
     * @param transaction enclosing transaction
     * @exception RawRollbackException (or any other exception) if transaction is rolled back
     */
    void run(IRawTransaction transaction);
    
    /**
     * Called when validating write transaction is about to be committed.
     * 
     * @exception RawRollbackException or any other exception if validation is failed and transaction needed to be rolled back
     */
    void validate();
    
    /**
     * Called when pages of write transaction are about to be committed. Can be used for flushing cached data to pages before commit.
     */
    void onBeforeCommitted();
    
    /**
     * Called when read-only or write transaction is successfully committed.
     */
    void onCommitted();
    
    /**
     * Called when write transaction is rolled back.
     * 
     * @return if true all internal caches of operation must be cleared
     */
    boolean onBeforeRolledBack();
    
    /**
     * Called when write transaction is rolled back.
     * 
     * @param clearCache if true all internal caches of operation must be cleared
     */
    void onRolledBack(boolean clearCache);
}
