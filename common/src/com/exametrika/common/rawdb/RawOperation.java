/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.util.List;




/**
 * The {@link RawOperation} is an implementation of {@link IRawOperation}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class RawOperation implements IRawOperation
{
    private final int options;
    
    public RawOperation()
    {
        options = 0;
    }
    
    public RawOperation(boolean readOnly)
    {
        options = readOnly ? READ_ONLY : 0;
    }
    
    public RawOperation(int options)
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
    public List<String> getBatchLockPredicates()
    {
        return null;
    }
    
    @Override
    public boolean isCompleted()
    {
        return true;
    }
    
    @Override
    public void onBeforeStarted(IRawTransaction transaction)
    {
    }
    
    @Override
    public void validate()
    {
    }
    
    @Override
    public void onBeforeCommitted()
    {
    }
    
    @Override
    public void onCommitted()
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
