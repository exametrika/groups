/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.io.File;

import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.utils.Assert;

/**
 * The {@link RawBatchManager} is used to manage batch transactions.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawBatchManager
{
    private RawBatchOperationSpace batchOperationSpace;
    private final RawTransactionManager transactionManager;
    private final IRawBatchContext batchContext;
    
    public RawBatchManager(RawTransactionManager transactionManager, IRawBatchContext batchContext)
    {
        Assert.notNull(transactionManager);
        
        this.transactionManager = transactionManager;
        this.batchContext = batchContext;
    }

    public IRawBatchContext getBatchContext()
    {
        return batchContext;
    }
    
    public void open(String path)
    {
        File batchOperationSpaceFile = new File(path, RawBatchOperationSpace.BATCH_OPERATION_SPACE_FILE_NAME);
        if (!batchOperationSpaceFile.exists() || batchOperationSpaceFile.length() == 0)
        {
            transactionManager.transactionSync(new RawOperation()
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    batchOperationSpace = RawBatchOperationSpace.create(transactionManager);
                }
            });
        }
        else
        {
            transactionManager.transactionSync(new RawOperation()
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    getBatchOperationSpace();
                }
            });
        }
    }
    
    public RawBatchOperationSpace getBatchOperationSpace()
    {
        if (batchOperationSpace != null)
            return batchOperationSpace; 
        else
        {
            batchOperationSpace = RawBatchOperationSpace.open(transactionManager);
            
            RawDbBatchOperation operation = batchOperationSpace.getOperation(batchContext);
            if (operation != null)
                transactionManager.transaction(operation);

            return batchOperationSpace;
        }
    }
    
    public void clearCache()
    {
        batchOperationSpace = null;
    }
}
