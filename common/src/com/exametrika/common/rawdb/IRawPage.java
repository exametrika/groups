/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;




/**
 * The {@link IRawPage} represents a page.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawPage
{
    /**
     * Is page stale? Stale page can not be used and must be replaced by the new loaded page from database.
     * Page becomes stale only when all database caches are cleared.
     *
     * @return true if node is unloaded
     */
    boolean isStale();
    
    /**
     * Returns page size.
     *
     * @return page size
     */
    int getSize();
    
    /**
     * Returns page index in data file.
     *
     * @return page index in data file
     */
    long getIndex();
    
    /**
     * Returns page's data file.
     *
     * @return page's data file
     */
    IRawDataFile getFile();
    
    /**
     * Is page read-only? Page is read-only if page's transaction is read-only.
     *
     * @return true if page is read-only
     */
    boolean isReadOnly();
    
    /**
     * Returns custom user defined page data.
     *
     * @return custom user defined page data or null if page data are not set
     */
    IRawPageData getData();
    
    /**
     * Sets custom user defined page data.
     *
     * @param data custom user defined page data or null if page data are not set
     */
    void setData(IRawPageData data);
    
    /**
     * Returns page read region.
     *
     * @return page read region
     */
    IRawReadRegion getReadRegion();
    
    /**
     * Returns page write region.
     *
     * @return page write region
     * @exception RawTransactionReadOnlyException if transaction is read-only 
     */
    IRawWriteRegion getWriteRegion();
}
