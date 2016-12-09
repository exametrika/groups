/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.json.JsonObject;




/**
 * The {@link RawDatabaseInterceptor} is a database interceptor.
 * 
 * @threadsafety Implementations of this class and its methods are thread safe.
 * @author AndreyM
 */
public class RawDatabaseInterceptor
{
    public static RawDatabaseInterceptor INSTANCE = new RawDatabaseInterceptor();
    
    public int onStarted(String databaseName)
    {
        return 0;
    }
    
    public void onStopped(int id)
    {
    }

    public void onDatabase(int id, JsonObject resourceAllocatorInfo, int currentFileCount, int pagePoolSize, 
        int transactionQueueSize)
    {
    }
    
    public boolean onBeforeFileRead(int id)
    {
        return false;
    }
    
    public void onAfterFileRead(int id, int size)
    {
    }
    
    public boolean onBeforeFileWritten(int id)
    {
        return false;
    }
    
    public void onAfterFileWritten(int id, int size)
    {
    }
    
    public void onFileLoaded(int id)
    {
    }
    
    public void onFileUnloaded(int id)
    {
    }
    
    public boolean onBeforeLogFlushed(int id)
    {
        return false;
    }
    
    public void onAfterLogFlushed(int id, long size)
    {
    }
    
    public boolean onTransactionStarted(int id)
    {
        return false;
    }
    
    public void onTransactionCommitted(int id)
    {
    }
    
    public void onTransactionRolledBack(int id, Throwable exception)
    {
    }
    
    public int onPageCacheCreated(int id, String pageCacheName, int pageSize)
    {
        return 0;
    }
    
    public void onPageCacheClosed(int id)
    {
    }
    
    public void onPageCache(int id, long pageCacheSize, long maxPageCacheSize, long quota)
    {
    }
    
    public void onPageLoaded(int id)
    {
    }
    
    public void onPageUnloaded(int id, boolean byTimer)
    {
    }
    
    public int onPageTypeCreated(int id, String pageTypeName, int pageSize)
    {
        return 0;
    }
    
    public void onPageTypeClosed(int id)
    {
    }
    
    public void onPageType(int id, long currentRegionsCount, long currentRegionsSize)
    {
    }
    
    public void onRegionAllocated(int id, int size)
    {
    }
    
    public void onRegionFreed(int id, int size)
    {
    }
}
