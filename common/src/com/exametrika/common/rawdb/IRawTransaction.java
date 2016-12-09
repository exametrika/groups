/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.util.Map;



/**
 * The {@link IRawTransaction} represents a transaction.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawTransaction
{
    /**
     * Is transaction read-only?
     *
     * @return true if transaction read-only
     */
    boolean isReadOnly();
    
    /**
     * Is transaction completed?
     *
     * @return true if transaction is completed
     */
    boolean isCompleted();
    
    /**
     * Returns database.
     *
     * @return database
     */
    IRawDatabase getDatabase();
    
    /**
     * Returns transaction operation.
     *
     * @return transaction operation
     */
    IRawOperation getOperation();
    
    /**
     * Returns transaction start time.
     *
     * @return transaction start time
     */
    long getTime();
    
    /**
     * Returns modifiable arbitrary user-defined transaction bound parameters.
     *
     * @return modifiable arbitrary user-defined transaction bound parameters
     */
    Map<String, Object> getParameters();
    
    /**
     * Is file with specified index bound?
     *
     * @param fileIndex file index
     * @return true if file is bound
     */
    boolean isFileBound(int fileIndex);
    
    /**
     * Binds data file to a given file index. Allows to set up custom parameters of data files. Binding must be the first access
     * to specified data file in application.
     *
     * @param fileIndex index of file
     * @param bindInfo file bind information
     * @return data file
     * @exception RawFileNotFoundException when file does not exist and transaction is read-only
     */
    IRawDataFile bindFile(int fileIndex, RawBindInfo bindInfo);
    
    /**
     * Unbinds data file. Unbinding is not allowed if file is changed or file's pages are loaded.
     *
     * @param fileIndex index of file
     */
    void unbindFile(int fileIndex);
    
    /**
     * Returns data file by index.
     *
     * @param fileIndex data file index
     * @return data file
     * @exception RawFileNotFoundException when file does not exist and transaction is read-only
     */
    IRawDataFile getFile(int fileIndex);
    
    /**
     * Returns page by index.
     *
     * @param fileIndex file index
     * @param pageIndex page index
     * @return page
     * @exception RawFileNotFoundException when file does not exist and transaction is read-only
     * @exception RawPageNotFoundException when page does not exist and transaction is read-only
     */
    IRawPage getPage(int fileIndex, long pageIndex);
}
