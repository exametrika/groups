/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;

import java.io.File;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.utils.Assert;



/**
 * The {@link RawFileCache} is a file cache.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawFileCache
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final RawDatabase database;
    private final TIntObjectMap<RawDataFile> newFiles = new TIntObjectHashMap<RawDataFile>();
    private final TIntObjectMap<RawDataFile> files = new TIntObjectHashMap<RawDataFile>();
    private volatile long readCount;
    private volatile long readSize;
    private volatile long writeCount;
    private volatile long writeSize;
    private volatile long syncCount;
        
    public RawFileCache(RawDatabase database)
    {
        Assert.notNull(database);
    
        this.database = database;
    }
    
    public int getFileCount()
    {
        return files.size();
    }
    
    public void incrementRead(int size)
    {
        readCount++;
        readSize += size;
    }
    
    public void incrementWrite(int size)
    {
        writeCount++;
        writeSize += size;
    }
    
    public void incrementSync()
    {
        syncCount++;
    }
    
    public boolean isFileBound(int fileIndex)
    {
        RawDataFile file = files.get(fileIndex);
        return file != null;
    }
    
    public RawDataFile bindFile(int fileIndex, boolean transactionReadOnly, RawBindInfo bindInfo)
    {
        Assert.notNull(bindInfo);
        RawDataFile file = files.get(fileIndex);
        if (file != null)
            return file;
        
        return createFile(bindInfo.getPathIndex(), bindInfo.getName(), fileIndex, transactionReadOnly, 
            bindInfo.getMaxFileSize(), bindInfo.getFlags(), bindInfo.getPageTypeIndex(), bindInfo.getCategoryType(),
            bindInfo.getCategory());
    }
    
    public void unbindFile(int fileIndex)
    {
        RawDataFile file = files.get(fileIndex);
        if (file != null)
        {
            Assert.checkState(file.allowUnbind());
            removeFile(file);
        }
    }
    
    public RawDataFile getFile(int fileIndex, boolean transactionReadOnly)
    {
        RawDataFile file = files.get(fileIndex);
        if (file != null)
            return file;
        
        return createFile(0, null, fileIndex, transactionReadOnly, 0, 0, 0, null, null);
    }

    public void removeFile(RawDataFile file)
    {
        files.remove(file.getIndex());
        newFiles.remove(file.getIndex());
        file.close(true);
    }
    
    public void commit()
    {
        newFiles.forEachValue(new TObjectProcedure<RawDataFile>()
        {
            @Override
            public boolean execute(RawDataFile file)
            {
                file.setOld();
                return true;
            }
        });
        
        newFiles.clear();
    }
    
    public void rollback()
    {
        newFiles.forEachValue(new TObjectProcedure<RawDataFile>()
        {
            @Override
            public boolean execute(RawDataFile file)
            {
                file.rollbackNew();
                files.remove(file.getIndex());
                file.close(true);
                return true;
            }
        });
        
        newFiles.clear();
    }
    
    public void close()
    {
        files.forEachValue(new TObjectProcedure<RawDataFile>()
        {
            @Override
            public boolean execute(RawDataFile file)
            {
                file.close(true);
                return true;
            }
        });
        
        newFiles.clear();
        files.clear();
    }

    public String printStatistics()
    {
        return messages.statistics(files.size(), readCount, readSize, writeCount, writeSize, syncCount).toString();
    }
    
    private RawDataFile createFile(int pathIndex, String name, int fileIndex, boolean transactionReadOnly, 
        long maxFileSize, int flags, int pageTypeIndex, String categoryType, String category)
    {
        if (name == null)
            name = "db-" + fileIndex + ".dat";
        if (fileIndex == RawBatchOperationSpace.BATCH_OPERATION_SPACE_FILE_INDEX)
            name = RawBatchOperationSpace.BATCH_OPERATION_SPACE_FILE_NAME;
        
        File dataFilePath = new File(database.getConfiguration().getPaths().get(pathIndex), name);
        
        if (transactionReadOnly && !dataFilePath.exists())
            return null;
        
        RawPageType pageType = database.getPageTypeManager().getPageType(pageTypeIndex);
        RawPageCache pageCache = pageType.getPageCache(categoryType, category);
        
        RawDataFile dataFile = new RawDataFile(pathIndex, name, dataFilePath, fileIndex, 
            maxFileSize != 0 ? maxFileSize : database.getConfiguration().getMaxFileSize(), flags, database, this, pageCache);
        
        files.put(fileIndex, dataFile);  
        
        if (dataFile.isNew())
            newFiles.put(fileIndex, dataFile);
        
        return dataFile;
    }
    
    private interface IMessages
    {
        @DefaultMessage("file cache - loaded file count: {0}, read count: {1}, read size: {2}, write count: {3}, write size: {4}, sync count: {5}")
        ILocalizedMessage statistics(int fileCount, long readCount, long readSize, long writeCount, long writeSize, long syncCount);
    }
}
