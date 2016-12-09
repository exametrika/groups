/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;





/**
 * The {@link RawBatchOperation} is a default implementation of batch operation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class RawBatchOperation implements IRawBatchOperation, Serializable 
{
    private final int options;
    
    public RawBatchOperation()
    {
        options = 0;
    }
    
    public RawBatchOperation(boolean readOnly)
    {
        options = readOnly ? IRawOperation.READ_ONLY : 0;
    }
    
    public RawBatchOperation(int options)
    {
        this.options = options;
    }
    
    @Override
    public int getOptions()
    {
        return options;
    }

    @Override
    public int getSize()
    {
        return 1;
    }
    
    @Override
    public List<RawBatchLock> getLocks()
    {
        return Collections.emptyList();
    }
    
    @Override
    public void setContext(IRawBatchContext context)
    {
    }
    
    @Override
    public void onBeforeStarted(IRawTransaction transaction)
    {
    }
    
    @Override
    public void validate(IRawTransaction transaction)
    {
    }
    
    @Override
    public void onBeforeCommitted(boolean completed)
    {
    }
    
    @Override
    public void onCommitted(boolean completed)
    {
    }

    @Override
    public boolean onBeforeRolledBack()
    {
        return false;
    }
    
    @Override
    public void onRolledBack(boolean clearCache)
    {
    }
}
