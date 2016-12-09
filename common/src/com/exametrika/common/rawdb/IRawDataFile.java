/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;



/**
 * The {@link IRawDataFile} represents a single datafile of database.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawDataFile
{
    /** Read mode of file. */
    enum ReadMode
    {
        /** Normal read mode (default). */
        NORMAL,
        
        /** Sequential read mode. */
        SEQUENTIAL,
        
        /** Random read mode. */
        RANDOM
    }
    
    /**
     * Is file read-only?
     *
     * @return true if file is read-only
     */
    boolean isReadOnly();

    /**
     * Is file stale? 
     *
     * @return true if file is stale
     */
    boolean isStale();
    
    /**
     * Returns true if file is deleted in current transaction.
     *
     * @return true if file is deleted in current transaction
     */
    boolean isDeleted();
    
    /**
     * Returns page size of data file.
     *
     * @return page size of data file
     */
    int getPageSize();
    
    /** Returns file size.
     *
     * @return file size
     */
    long getSize();
    
    /**
     * Returns file index in database.
     *
     * @return file index in database
     */
    int getIndex();
    
    /**
     * Returns path to data file.
     *
     * @return path to data file
     */
    String getPath();

    /**
     * Returns file read mode.
     *
     * @return file read mode
     */
    ReadMode getReadMode();
    
    /**
     * Sets file read mode.
     *
     * @param readMode read mode
     */
    void setReadMode(ReadMode readMode);
    
    /**
     * Returns category type.
     *
     * @return category type
     */
    String getCategoryType();
    
    /**
     * Returns category.
     *
     * @return category
     */
    String getCategory();
    
    /**
     * Sets category.
     *
     * @param categoryType category type or null if default category type is used
     * @param category category or null if default category is used
     */
    void setCategory(String categoryType, String category);
    
    /**
     * Initiates asynchronous prefetching of specified file region into IO cache.
     *
     * @param startPageIndex index of first page of prefetching file region
     * @param endPageIndex index of last + 1 page of prefetching file region
     */
    void prefetch(long startPageIndex, long endPageIndex);
    
    /**
     * Truncates data file to specified size.
     *
     * @param newSize new data file size
     */
    void truncate(long newSize);
    
    /**
     * Deletes data file.
     */
    void delete();
}
