/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;





/**
 * The {@link IRawBatchControl} represents a batch control interface.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author AndreyM
 */
public interface IRawBatchControl
{
    /**
     * Returns true if page caching is enabled. If caching is disabled all subsequently loaded pages will not be cached
     * and be returned as readonly.
     *
     * @return true if page caching is enabled
     */
    boolean isPageCachingEnabled();
    
    /**
     * Enabled or disabled page caching.
     *
     * @param value true if page caching is enabled
     */
    void setPageCachingEnabled(boolean value);
    
    /**
     * Returns size of invalidation queue for non-cached pages. Non cached page added to invalidation queue after loading.
     * Last queue page is removed and invalidated when queue size reaches specified value. Higher values allow to 
     * simultaneously access more non-cached pages.
     *
     * @return size of invalidation queue for non-cached pages
     */
    int getNonCachedPagesInvalidationQueueSize();
    
    /**
    * Sets size of invalidation queue for non-cached pages.
    *
    * @param value size of invalidation queue for non-cached pages
    */
    void setNonCachedPagesInvalidationQueueSize(int value);
    
    /**
     * Adds additional constraint on maximum page cache size of specified page cache category. Constraint is applied only when
     * batch step is running and disabled when normal transaction is executed. Page cache category must be bound before
     * using this method.
     *
     * @param pageTypeIndex index of page type of category
     * @param category cache category
     * @param value maximum page cache size of specified page cache category
     */
    void setMaxPageCacheSize(int pageTypeIndex, String category, long value);
    
    /**
     * Can batch operation continue execution.
     *
     * @return true if batch operation can continue execution, false if batch operation have to save internal state in 
     * serializable fields and exit
     */
    boolean canContinue();
}
