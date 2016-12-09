/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.rawdb;

import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawOperation;

public class RawTestOperation extends RawOperation
{
    public boolean validated;
    public boolean beforeCommitted;
    public boolean committed;
    public boolean rolledBack;
    public RuntimeException exception;
    public RuntimeException validateException;

    public RawTestOperation()
    {
    }
    
    public RawTestOperation(boolean readOnly)
    {
        super(readOnly);
    }
    
    public RawTestOperation(int options)
    {
        super(options);
    }
    
    @Override
    public void run(IRawTransaction transaction)
    {
        if (exception != null)
            throw exception;
    }

    @Override
    public void validate()
    {
        validated = true;
        
        if (validateException != null)
            throw validateException;
    }
    
    @Override
    public void onBeforeCommitted()
    {
        beforeCommitted = true;
    }
    
    @Override
    public void onCommitted()
    {
        committed = true;
    }

    @Override
    public void onRolledBack(boolean clearCache)
    {
        rolledBack = true;
    }
}