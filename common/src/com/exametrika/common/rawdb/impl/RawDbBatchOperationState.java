/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.Set;

import com.exametrika.common.rawdb.IRawBatchOperation;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;

/**
 * The {@link RawDbBatchOperationState} is a serializable batch operation state.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawDbBatchOperationState
{
    public static final class PageCacheConstraint
    {
        public final int pageTypeIndex;
        public final String category;
        public long maxPageCacheSize;

        public PageCacheConstraint(int pageTypeIndex, String category, long maxPageCacheSize)
        {
            Assert.notNull(category);
            
            this.pageTypeIndex = pageTypeIndex;
            this.category = category;
            this.maxPageCacheSize = maxPageCacheSize;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof PageCacheConstraint))
                return false;
            
            PageCacheConstraint constraint = (PageCacheConstraint)o;
            return pageTypeIndex == constraint.pageTypeIndex && category.equals(constraint.category);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(pageTypeIndex, category);
        }
        
        @Override
        public String toString()
        {
            return pageTypeIndex + ":" + category;
        }
    }
    
    public final IRawBatchOperation operation;
    public boolean cachingEnabled = true;
    public int nonCachedPagesInvalidationQueueSize = 10;
    public Set<PageCacheConstraint> constraints;
    
    public RawDbBatchOperationState(IRawBatchOperation operation)
    {
        Assert.notNull(operation);
        
        this.operation = operation;
    }
}