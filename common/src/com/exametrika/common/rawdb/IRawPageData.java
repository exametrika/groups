/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;




/**
 * The {@link IRawPageData} represents a user-defined custom page data.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawPageData
{
    /**
     * Called before modified page has been committed.
     */
    void onBeforeCommitted();
    
    /**
     * Called when modified page has been committed.
     */
    void onCommitted();
    
    /**
     * Called when modified page has been rolled back.
     */
    void onRolledBack();
    
    /**
     * Called when page has been unloaded from page cache.
     */
    void onUnloaded();
}
