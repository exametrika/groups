/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration.Flag;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;



/**
 * The {@link RawTransactionLog} is an undo-based transaction log.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawTransactionLog
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(RawTransactionLog.class);
    private final RawDatabase database;
    private final File transactionLogPath;
    private RandomAccessFile transactionLogFile;
    private final File redoLogPath;
    private final boolean disableSync;
    private RandomAccessFile redoLogFile;
    private long startPos;
    private byte[] compressedBuffer;
    private byte[] buffer;
    private boolean opened;
    private boolean closed;
    
    public static class FlushInfo
    {
        private final RawDataFile file;
        private final long pageIndex;
        private final RawRegion savedRegion;
        private final RawRegion region;
        private final boolean flushed;

        public FlushInfo(RawDataFile file, long pageIndex, RawRegion savedRegion, RawRegion region, boolean flushed)
        {
            Assert.notNull(file);
            Assert.notNull(savedRegion);
            Assert.notNull(region);
            Assert.isTrue(savedRegion.getLength() == region.getLength());
            
            this.file = file;
            this.pageIndex = pageIndex;
            this.savedRegion = savedRegion;
            this.region = region;
            this.flushed = flushed;
        }
    }
    
    public RawTransactionLog(File path, RawDatabase database)
    {
        Assert.notNull(path);
        Assert.notNull(database);
        
        this.database = database;
        transactionLogPath = new File(path, "tx.log");
        redoLogPath = new File(path, "txr.log");
        disableSync = database.getConfiguration().getFlags().contains(Flag.DISABLE_SYNC);
    }
    
    public void open()
    {
        Assert.checkState(!closed && !opened);
        
        try
        {
            opened = true;
            transactionLogFile = new RandomAccessFile(transactionLogPath, "rw");
            redoLogFile = new RandomAccessFile(redoLogPath, "rw");
            recover();
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
    }
    
    public synchronized void close()
    {
        if (closed || !opened)
            return;
        
        IOs.close(transactionLogFile);
        IOs.close(redoLogFile);
        if (transactionLogPath.length() == 0)
            transactionLogPath.delete();
        if (redoLogPath.length() == 0)
            redoLogPath.delete();
        
        closed = true;
    }
    
    public synchronized void flush(List<FlushInfo> flushedPages, List<RawDataFile> flushedFiles, boolean full)
    {
        if (closed)
            return;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.flushStarted(flushedPages.size()));
        
        boolean interceptResult = RawDatabaseInterceptor.INSTANCE.onBeforeLogFlushed(database.getInterceptId());
        long flushSize = 0;
        Collections.sort(flushedPages, new PageComparator());
        
        try
        {
            long startFlushSize = transactionLogFile.getFilePointer();
            if (transactionLogFile.getFilePointer() == 0)
            {
                transactionLogFile.writeBoolean(false);
                transactionLogFile.writeLong(0);
                transactionLogFile.writeLong(0);
                transactionLogFile.writeLong(0);
            }
            
            long pos = transactionLogFile.getFilePointer();
            
            Set<RawDataFile> files = new HashSet<RawDataFile>();
            for (FlushInfo info : flushedPages)
                files.add(info.file);
                
            transactionLogFile.writeInt(files.size());
            for (RawDataFile file : files)
            {
                transactionLogFile.writeInt(file.getIndex());
                transactionLogFile.writeInt(file.getPathIndex());
                transactionLogFile.writeLong(file.getFlushSize());
                transactionLogFile.writeBoolean(file.exists());
                transactionLogFile.writeUTF(file.getName());
                transactionLogFile.writeInt(file.getPageCache().getPageType().getIndex());
                transactionLogFile.writeUTF(file.getPageCache().getName());
                transactionLogFile.writeUTF(file.getPageCache().getConfiguration().getName());
            }
            
            transactionLogFile.writeInt(flushedPages.size());
            for (FlushInfo info : flushedPages)
            {
                if (!info.flushed && info.file.exists() && (info.pageIndex + 1) * info.savedRegion.getLength() <= info.file.getFlushSize())
                {
                    transactionLogFile.writeBoolean(true);
                    transactionLogFile.writeInt(info.file.getIndex());
                    transactionLogFile.writeLong(info.pageIndex);

                    Assert.checkState(info.savedRegion.getLength() == info.file.getPageSize());
                    int maxCompressedLength = LZ4.maxCompressedLength(info.savedRegion.getLength());
                    if (compressedBuffer == null || compressedBuffer.length < maxCompressedLength)
                        compressedBuffer = new byte[maxCompressedLength];
                    
                    ByteArray buffer = info.savedRegion.readByteArray(0, info.savedRegion.getLength());
                    int compressedLength = LZ4.compress(true, buffer.getBuffer(), buffer.getOffset(), buffer.getLength(), 
                        compressedBuffer, 0, compressedBuffer.length);
                    
                    info.savedRegion.setFlushing(true, false);

                    transactionLogFile.writeInt(compressedLength);
                    transactionLogFile.write(compressedBuffer, 0, compressedLength);
                }
                else
                {
                    info.savedRegion.setFlushing(true, false);
                    transactionLogFile.writeBoolean(false);
                }
            }
            
            transactionLogFile.writeLong(startPos);
           
            long oldPos = transactionLogFile.getFilePointer();
            transactionLogFile.seek(1);
            transactionLogFile.writeLong(pos);
            transactionLogFile.writeLong(pos);
            transactionLogFile.writeLong(pos);
            transactionLogFile.seek(oldPos);
            startPos = pos;
            
            if (full)
            {
                pos = transactionLogFile.getFilePointer();
                transactionLogFile.writeInt(flushedFiles.size());
                for (RawDataFile file : flushedFiles)
                {
                    transactionLogFile.writeBoolean(file.isDeleted());
                    transactionLogFile.writeBoolean(file.isDirectoryOwner());
                    transactionLogFile.writeLong(file.getSize());
                    transactionLogFile.writeInt(file.getIndex());
                    transactionLogFile.writeInt(file.getPathIndex());
                    transactionLogFile.writeUTF(file.getName());
                    transactionLogFile.writeInt(file.getPageCache().getPageType().getIndex());
                    transactionLogFile.writeUTF(file.getPageCache().getName());
                    transactionLogFile.writeUTF(file.getPageCache().getConfiguration().getName());
                }
            }
            else
                Assert.isTrue(flushedFiles.isEmpty());
            
            flushSize = transactionLogFile.getFilePointer() - startFlushSize;
            
            if (!disableSync)
                transactionLogFile.getFD().sync();
            
            for (FlushInfo info : flushedPages)
            {
                info.file.logWritePage(info.pageIndex, info.region);
                info.region.setFlushing(false, false);
            }
            
            for (RawDataFile file : files)
                file.logSync(!disableSync);
            
            if (full)
            {
                if (!flushedFiles.isEmpty())
                {
                    transactionLogFile.seek(1);
                    transactionLogFile.writeLong(pos);
                    transactionLogFile.seek(0);
                    transactionLogFile.writeBoolean(true);
                    
                    if (!disableSync)
                        transactionLogFile.getFD().sync();
                    
                    for (RawDataFile file : flushedFiles)
                    {
                        if (file.isDeleted())
                            file.logDelete();
                        else
                            file.logTruncate(file.getSize());
                    }
                }
                
                redoLogFile.setLength(0);
                transactionLogFile.setLength(0);
                startPos = 0;
            }
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
        finally
        {
            if(interceptResult)
                RawDatabaseInterceptor.INSTANCE.onAfterLogFlushed(database.getInterceptId(), flushSize);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.flushCompleted());
    }

    public synchronized void flushRedo(List<FlushInfo> flushedPages)
    {
        if (closed)
            return;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.redoFlushStarted(flushedPages.size()));
        
        boolean interceptResult = RawDatabaseInterceptor.INSTANCE.onBeforeLogFlushed(database.getInterceptId());
        long flushSize = 0;
        
        Collections.sort(flushedPages, new PageComparator());
        
        try
        {
            long startFlushSize = redoLogFile.getFilePointer();
            if (redoLogFile.getFilePointer() == 0)
            {
                redoLogFile.writeLong(0);
                redoLogFile.writeLong(0);
                redoLogFile.writeLong(0);
            }
            
            long pos = redoLogFile.getFilePointer();
            
            Set<RawDataFile> files = new HashSet<RawDataFile>();
            for (FlushInfo info : flushedPages)
                files.add(info.file);
                
            redoLogFile.writeInt(files.size());
            for (RawDataFile file : files)
            {
                redoLogFile.writeInt(file.getIndex());
                redoLogFile.writeInt(file.getPathIndex());
                redoLogFile.writeUTF(file.getName());
                transactionLogFile.writeInt(file.getPageCache().getPageType().getIndex());
                transactionLogFile.writeUTF(file.getPageCache().getName());
                transactionLogFile.writeUTF(file.getPageCache().getConfiguration().getName());
            }
            
            redoLogFile.writeInt(flushedPages.size());
            for (FlushInfo info : flushedPages)
            {
                redoLogFile.writeInt(info.file.getIndex());
                redoLogFile.writeLong(info.pageIndex);

                Assert.checkState(info.region.getLength() == info.file.getPageSize());
                int maxCompressedLength = LZ4.maxCompressedLength(info.region.getLength());
                if (compressedBuffer == null || compressedBuffer.length < maxCompressedLength)
                    compressedBuffer = new byte[maxCompressedLength];
                
                ByteArray buffer = info.region.readByteArray(0, info.region.getLength());
                int compressedLength = LZ4.compress(true, buffer.getBuffer(), buffer.getOffset(), buffer.getLength(), 
                    compressedBuffer, 0, compressedBuffer.length);
                
                redoLogFile.writeInt(compressedLength);
                redoLogFile.write(compressedBuffer, 0, compressedLength);
            }
            
            long oldPos = redoLogFile.getFilePointer();
            flushSize = oldPos - startFlushSize;
            redoLogFile.seek(0);
            redoLogFile.writeLong(pos);
            redoLogFile.writeLong(pos);
            redoLogFile.writeLong(pos);
            redoLogFile.seek(oldPos);
            
            if (!disableSync)
                redoLogFile.getFD().sync();
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
        finally
        {
            if (interceptResult)
                RawDatabaseInterceptor.INSTANCE.onAfterLogFlushed(database.getInterceptId(), flushSize);
        }
        
        for (FlushInfo info : flushedPages)
        {
            Assert.checkState(!info.flushed);
            
            info.savedRegion.setFlushing(true, false);
            info.region.setFlushing(false, false);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.redoFlushCompleted());
    }

    public synchronized void recover()
    {
        if (closed)
            return;
        
        recoverRedo();
        
        try
        {
            if (transactionLogFile.length() == 0)
                return;
            
            transactionLogFile.seek(0);
            
            boolean redoPhase = transactionLogFile.readBoolean();
            if (!redoPhase)
            {
                long pos1 = transactionLogFile.readLong();
                long pos2 = transactionLogFile.readLong();
                long pos3 = transactionLogFile.readLong();
                long pos;
                if (pos1 == pos2)
                    pos = pos1;
                else if (pos2 == pos3)
                    pos = pos2;
                else
                    pos = pos3;
                
                while (pos != 0)
                {
                    Set<RawDataFile> files = new HashSet<RawDataFile>();
                    
                    transactionLogFile.seek(pos);
                    
                    int count = transactionLogFile.readInt();
                    for (int i = 0; i < count; i++)
                    {
                        int fileIndex = transactionLogFile.readInt();
                        
                        RawBindInfo info = new RawBindInfo();
                        info.setPathIndex(transactionLogFile.readInt());
                        long fileSize = transactionLogFile.readLong();
                        boolean exists = transactionLogFile.readBoolean();
                        info.setName(transactionLogFile.readUTF());
                        info.setPageTypeIndex(transactionLogFile.readInt());
                        info.setCategory(transactionLogFile.readUTF());
                        info.setCategoryType(transactionLogFile.readUTF());
                            
                        if (exists)
                        {
                            RawDataFile file = database.getFileCache().bindFile(fileIndex, false, info);
                            files.add(file);
                            
                            if (file.getSize() != fileSize)
                                file.logTruncate(fileSize);
                        }
                        else
                            new File(database.getConfiguration().getPaths().get(info.getPathIndex()), info.getName()).delete();
                    }
                    
                    count = transactionLogFile.readInt();
                    for (int i = 0; i < count; i++)
                    {
                        if (!transactionLogFile.readBoolean())
                            continue;
                        
                        int fileIndex = transactionLogFile.readInt();
                        RawDataFile file = database.getFileCache().getFile(fileIndex, false);
                        
                        long pageIndex = transactionLogFile.readLong();
                        
                        int maxCompressedLength = LZ4.maxCompressedLength(file.getPageSize());
                        if (this.compressedBuffer == null || this.compressedBuffer.length < maxCompressedLength)
                            this.compressedBuffer = new byte[maxCompressedLength];
                        if (buffer == null || buffer.length < file.getPageSize())
                            buffer = new byte[file.getPageSize()];

                        int compressedLength = transactionLogFile.readInt();
                        transactionLogFile.readFully(compressedBuffer, 0, compressedLength);
                        
                        LZ4.decompress(compressedBuffer, 0, buffer, 0, file.getPageSize());
                        
                        file.logWritePage(pageIndex, new RawHeapReadRegion(buffer, 0, file.getPageSize()));
                    }
                    
                    for (RawDataFile file : files)
                        file.logSync(true);

                    pos = transactionLogFile.readLong();
                }
            }
            else
            {
                long pos = transactionLogFile.readLong();
                transactionLogFile.seek(pos);
                
                int count = transactionLogFile.readInt();
                for (int i = 0; i < count; i++)
                {
                    boolean deleted = transactionLogFile.readBoolean();
                    boolean directoryOwner  = transactionLogFile.readBoolean();
                    long size = transactionLogFile.readLong();
                    int fileIndex = transactionLogFile.readInt();
                    
                    RawBindInfo info = new RawBindInfo();
                    info.setPathIndex(transactionLogFile.readInt());
                    info.setName(transactionLogFile.readUTF());
                    info.setPageTypeIndex(transactionLogFile.readInt());
                    info.setCategory(transactionLogFile.readUTF());
                    info.setCategoryType(transactionLogFile.readUTF());
                    
                    if (deleted)
                    {
                        File path = new File(database.getConfiguration().getPaths().get(info.getPathIndex()), info.getName());
                        path.delete();
                        
                        if (directoryOwner)
                            Files.delete(path.getParentFile());
                    }
                    else
                    {
                        RawDataFile file = database.getFileCache().bindFile(fileIndex, false, info);
                        file.logTruncate(size);
                    }
                }
            }
        }
        catch (IOException e)
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, database.getMarker(), e);
        }

        try
        {
            transactionLogFile.setLength(0);
            startPos = 0;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, database.getMarker(), messages.databaseRecovered(
                    database.getConfiguration().getPaths().toString()));
        }
        catch (IOException e)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, database.getMarker(), e);
        }
        
        database.getFileCache().close();
    }

    @Override
    public String toString()
    {
        return transactionLogPath.toString();
    }
    
    private void recoverRedo()
    {
        try
        {
            if (redoLogFile.length() == 0)
                return;
            
            redoLogFile.seek(0);
            
            long pos1 = redoLogFile.readLong();
            long pos2 = redoLogFile.readLong();
            long pos3 = redoLogFile.readLong();
            long lastCommittedPos;
            if (pos1 == pos2)
                lastCommittedPos = pos1;
            else if (pos2 == pos3)
                lastCommittedPos = pos2;
            else
                lastCommittedPos = pos3;
            
            while (true)
            {
                Set<RawDataFile> files = new HashSet<RawDataFile>();
                
                if (lastCommittedPos < redoLogFile.getFilePointer())
                    break;
                
                int count = redoLogFile.readInt();
                for (int i = 0; i < count; i++)
                {
                    int fileIndex = redoLogFile.readInt();
                    
                    RawBindInfo info = new RawBindInfo();
                    info.setPathIndex(redoLogFile.readInt());
                    info.setName(redoLogFile.readUTF());
                    info.setPageTypeIndex(transactionLogFile.readInt());
                    info.setCategory(transactionLogFile.readUTF());
                    info.setCategoryType(transactionLogFile.readUTF());
                        
                    RawDataFile file = database.getFileCache().bindFile(fileIndex, false, info);
                    files.add(file);
                }
                
                count = redoLogFile.readInt();
                for (int i = 0; i < count; i++)
                {
                    int fileIndex = redoLogFile.readInt();
                    RawDataFile file = database.getFileCache().getFile(fileIndex, false);
                    
                    long pageIndex = redoLogFile.readLong();
                    
                    int maxCompressedLength = LZ4.maxCompressedLength(file.getPageSize());
                    if (this.compressedBuffer == null || this.compressedBuffer.length < maxCompressedLength)
                        this.compressedBuffer = new byte[maxCompressedLength];
                    if (buffer == null || buffer.length < file.getPageSize())
                        buffer = new byte[file.getPageSize()];

                    int compressedLength = redoLogFile.readInt();
                    redoLogFile.readFully(compressedBuffer, 0, compressedLength);
                    
                    LZ4.decompress(compressedBuffer, 0, buffer, 0, file.getPageSize());
                    
                    file.logWritePage(pageIndex, new RawHeapReadRegion(buffer, 0, file.getPageSize()));
                }
                
                for (RawDataFile file : files)
                    file.logSync(true);
            }
        }
        catch (IOException e)
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, database.getMarker(), e);
        }

        try
        {
            redoLogFile.setLength(0);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, database.getMarker(), messages.databaseRedoRecovered(
                    database.getConfiguration().getPaths().toString()));
        }
        catch (IOException e)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, database.getMarker(), e);
        }
        
        database.getFileCache().close();
    }
    
    private static class PageComparator implements Comparator<FlushInfo>
    {
        @Override
        public int compare(FlushInfo o1, FlushInfo o2)
        {
            int fileIndex1 = o1.file.getIndex();
            int fileIndex2 = o2.file.getIndex();
            
            if (fileIndex1 < fileIndex2)
                return -1;
            else if (fileIndex1 > fileIndex2)
                return 1;
            
            long pageIndex1 = o1.pageIndex;
            long pageIndex2 = o2.pageIndex;
            
            if (pageIndex1 < pageIndex2)
                return -1;
            else if (pageIndex1 > pageIndex2)
                return 1;
            else
                return 0;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Database ''{0}'' is recovered.")
        ILocalizedMessage databaseRecovered(String path);
        
        @DefaultMessage("Database ''{0}'' is recovered with redo log.")
        ILocalizedMessage databaseRedoRecovered(String path);
        
        @DefaultMessage("Flush has been started. Committed pages count - ''{0}''.")
        ILocalizedMessage flushStarted(int committedPagesCount);
        
        @DefaultMessage("Flush has been completed.")
        ILocalizedMessage flushCompleted();
        
        @DefaultMessage("Redo flush has been started. Committed pages count - ''{0}''.")
        ILocalizedMessage redoFlushStarted(int committedPagesCount);
        
        @DefaultMessage("Redo flush has been completed.")
        ILocalizedMessage redoFlushCompleted();
    }
}