/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawFileCorruptedException;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration.Flag;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.Files.AdviceType;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Out;



/**
 * The {@link RawDataFile} is an implementation of {@link IRawDataFile}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawDataFile implements IRawDataFile
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(RawDataFile.class);
    private final RawFileCache fileCache;
    private final int pathIndex;
    private final String name;
    private final File path;
    private final int pageSize;
    private final int index;
    private final long maxFileSize;
    private final RawDatabase database;
    private final boolean readOnly;
    private final boolean temporary;
    private final boolean directoryOwner;
    private RawPageCache pageCache;
    private final boolean disableIOPageCacheEviction;
    private boolean preload;
    private ReadMode readMode = ReadMode.NORMAL;
    private RandomAccessFile file;
    private TLongObjectMap<RawPage> pages = new TLongObjectHashMap<RawPage>();
    private long loadedPageCount;
    private boolean isNew;
    private boolean deleted;
    private boolean truncated;
    private long size;
    private volatile long committedSize;
    private volatile long flushSize;
    private volatile boolean exists;
    private volatile boolean stale;
    
    public RawDataFile(int pathIndex, String name, File path, int index, long maxFileSize, int flags, 
        RawDatabase database, RawFileCache fileCache, RawPageCache pageCache)
    {
        Assert.notNull(name);
        Assert.notNull(path);
        Assert.notNull(database);
        Assert.notNull(fileCache);
        Assert.notNull(pageCache);
        Assert.isTrue(maxFileSize > 0);

        this.pathIndex = pathIndex;
        this.name = name;
        this.path = path;
        this.pageSize = pageCache.getPageSize();
        this.index = index;
        this.maxFileSize = maxFileSize;
        this.database = database;
        this.readOnly = (flags & RawBindInfo.READONLY) != 0;
        this.temporary = (flags & RawBindInfo.TEMPORARY) != 0;
        this.fileCache = fileCache;
        this.pageCache = pageCache;
        pageCache.addRef();
        
        if ((flags & RawBindInfo.PRELOAD) != 0)
            this.preload = true;
        else if ((flags & RawBindInfo.NOPRELOAD) != 0)
            this.preload = false;
        else
            this.preload = database.getConfiguration().getFlags().contains(Flag.PRELOAD_DATA);
        
        if ((flags & RawBindInfo.DIRECTORY_OWNER) != 0)
            directoryOwner = true;
        else
            directoryOwner = false;
        
        disableIOPageCacheEviction = database.getConfiguration().getFlags().contains(Flag.DISABLE_IO_CACHE_PAGE_EVICTION);
        exists = path.exists();
        isNew = !exists;
        size = path.length();
        committedSize = size;
        flushSize = size;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileCreated(path.getPath(), exists));
        
        RawDatabaseInterceptor.INSTANCE.onFileLoaded(database.getInterceptId());
    }

    public boolean exists()
    {
        return exists;
    }
    
    public boolean isNew()
    {
        return isNew;
    }
    
    public void setOld()
    {
        isNew = false;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileSetOld(path.getPath()));
    }
    
    public boolean isDirectoryOwner()
    {
        return directoryOwner;
    }
    
    public long getFlushSize()
    {
        return flushSize;
    }
    
    public RawDatabase getDatabase()
    {
        return database;
    }
    
    public RawPageCache getPageCache()
    {
        return pageCache;
    }
    
    public int getPathIndex()
    {
        return pathIndex;
    }
    
    public boolean allowUnbind()
    {
        return loadedPageCount == 0 && !deleted && !truncated && !stale;
    }
    
    public String getName()
    {
        return name;
    }
    
    public RawPage getPage(long pageIndex, boolean readOnly, boolean cachingEnabled, RawPageProxy proxy)
    {
        if (preload)
            preloadPages();
        
        RawPage page = pages.get(pageIndex);
        if (page != null)
            return page;
        
        return createPage(pageIndex, readOnly, cachingEnabled, proxy);
    }
    
    public void unloadPage(long pageIndex)
    {
        RawPage page = pages.remove(pageIndex);
        
        if (page != null)
        {
            loadedPageCount--;
            if (loadedPageCount == 0)
                close(false);
        }
    }
    
    public synchronized void logWritePage(long pageIndex, RawRegion region)
    {
        if (file == null)
            openFile();
        
        Assert.checkState(!readOnly);
        Assert.isTrue(region.getLength() == pageSize);
        
        ByteBuffer buffer = region.getBuffer();
        
        boolean interceptResult = RawDatabaseInterceptor.INSTANCE.onBeforeFileWritten(database.getInterceptId());
        try
        {
            long pos = pageIndex * pageSize;
            
            while (buffer.hasRemaining())
                pos += file.getChannel().write(buffer, pos);
            
            fileCache.incrementWrite(pageSize);
            
            if (flushSize < pos)
                flushSize = pos;
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
        finally
        {
            if (interceptResult)
                RawDatabaseInterceptor.INSTANCE.onAfterFileWritten(database.getInterceptId(), pageSize);
        }
    }
    
    public synchronized void logSync(boolean sync)
    {
        if (file == null)
            openFile();
        
        try
        {
            if (sync)
                file.getFD().sync();
            exists = true;
            fileCache.incrementSync();
            
            if (!disableIOPageCacheEviction)
                Files.advise(file.getFD(), 0, flushSize, AdviceType.POSIX_FADV_DONTNEED);
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileSynced(path.getPath()));
    }
    
    public void logDelete()
    {
        path.delete();
        
        if (directoryOwner)
            Files.delete(path.getParentFile());
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileDeleted(path.getPath()));
    }
    
    public synchronized void logTruncate(long size)
    {
        if (!exists)
            return;
        
        if (file == null)
            openFile();
        
        Assert.checkState(!readOnly);
        
        try
        {
            file.setLength(size);
            flushSize = size;
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileTrucated(path.getPath()));
    }
    
    public void close(boolean stale)
    {
        synchronized (this)
        {
            if (this.stale)
                return;
            
            IOs.close(file);
            file = null;
            
            pages = new TLongObjectHashMap<RawPage>();
            loadedPageCount = 0;
            this.stale = stale;
        }
        
        if (stale)
            pageCache.release();
        
        RawDatabaseInterceptor.INSTANCE.onFileUnloaded(database.getInterceptId());
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileClosed(path.getPath()));
    }
    
    @Override
    public boolean isReadOnly()
    {
        return readOnly;
    }
    
    @Override
    public boolean isStale()
    {
        return stale;
    }
    
    @Override
    public boolean isDeleted()
    {
        return deleted;
    }

    @Override
    public int getPageSize()
    {
        return pageSize;
    }
    
    @Override
    public long getSize()
    {
        return size;
    }
    
    @Override
    public int getIndex()
    {
        return index;
    }
    
    @Override
    public String getPath()
    {
        return path.getPath();
    }
    
    @Override
    public ReadMode getReadMode()
    {
        return readMode;
    }

    @Override
    public void setReadMode(ReadMode readMode)
    {
        Assert.notNull(readMode);
        
        this.readMode = readMode;
        
        if (file == null)
            return;
        
        Files.AdviceType adviceType;
        switch (readMode)
        {
        case NORMAL:
            adviceType = AdviceType.POSIX_FADV_NORMAL;
            break;
        case SEQUENTIAL:
            adviceType = AdviceType.POSIX_FADV_SEQUENTIAL;
            break;
        case RANDOM:
            adviceType = AdviceType.POSIX_FADV_RANDOM;
            break;
        default:
            Assert.error();
            return;
        }
        
        try
        {
            Files.advise(file.getFD(), 0, Long.MAX_VALUE, adviceType);
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
    }

    @Override
    public String getCategoryType()
    {
        return pageCache.getConfiguration().getName();
    }

    @Override
    public String getCategory()
    {
        return pageCache.getName();
    }

    @Override
    public void setCategory(String categoryType, String category)
    {
        final RawPageCache newPageCache = pageCache.getPageType().getPageCache(categoryType, category);
        if (newPageCache == pageCache)
            return;
        
        newPageCache.addRef();
        pages.forEachValue(new TObjectProcedure<RawPage>()
        {
            @Override
            public boolean execute(RawPage page)
            {
                newPageCache.migratePage(page);
                return true;
            }
        });
        
        pageCache.release();
        pageCache = newPageCache;
        
        newPageCache.unloadExcessive();
    }

    @Override
    public void prefetch(long startPageIndex, long endPageIndex)
    {
        try
        {
            Files.advise(file.getFD(), startPageIndex * pageSize, (endPageIndex - startPageIndex) * pageSize, 
                AdviceType.POSIX_FADV_WILLNEED);
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
    }

    @Override
    public void truncate(long newSize)
    {
        Assert.checkState(!deleted);
        
        if (!truncated)
        {
            RawTransaction transaction = database.getTransactionManager().getTransaction();
            Assert.checkState(!readOnly && transaction != null && !transaction.isReadOnly());
    
            database.getPageManager().flush(true);
            
            truncated = true;
            database.getPageManager().addWriteFile(this);
        }

        size = newSize;
        
        for (TLongObjectIterator<RawPage> it = pages.iterator(); it.hasNext(); )
        {
            it.advance();
            RawPage page = it.value(); 
            if (page.getIndex() * page.getSize() >= size)
            {
                it.remove();
                pageCache.removePage(page);
                loadedPageCount--;
            }
        }
    }

    @Override
    public void delete()
    {
        if (deleted)
            return;
        
        RawTransaction transaction = database.getTransactionManager().getTransaction();
        Assert.checkState(!readOnly && transaction != null && !transaction.isReadOnly());
        
        database.getPageManager().flush(true);
        
        deleted = true;
        
        pages.forEachValue(new TObjectProcedure<RawPage>()
        {
            @Override
            public boolean execute(RawPage page)
            {
                pageCache.removePage(page);
                return true;
            }
        });
        
        pages.clear();
        loadedPageCount = 0;
        
        database.getPageManager().addWriteFile(this);
    }

    @Override
    public String toString()
    {
        return path.toString();
    }
    
    public void commit()
    {
        if (deleted || temporary)
            database.getFileCache().removeFile(this);
        
        committedSize = size;
        truncated = false;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileCommitted(path.getPath()));
    }
    
    public void rollback()
    {
        if (temporary)
            database.getFileCache().removeFile(this);
        
        size = committedSize;
        deleted = false;
        truncated = false;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileRolledBack(path.getPath()));
    }
    
    public void rollbackNew()
    {
        pages.forEachValue(new TObjectProcedure<RawPage>()
        {
            @Override
            public boolean execute(RawPage page)
            {
                pageCache.removePage(page);
                return true;
            }
        });
        
        pages.clear();
        loadedPageCount = 0;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileRolledBackNew(path.getPath()));
    }
    
    private void openFile()
    {
        if (file != null)
            return;
        
        try
        {
            path.getParentFile().mkdirs();
            file = new RandomAccessFile(path, readOnly ? "r" : "rw");
            
            if (readMode != ReadMode.NORMAL)
                setReadMode(readMode);
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, database.getMarker(), messages.fileOpened(path.getPath()));
    }
    
    private void preloadPages()
    {
        if (exists && !deleted)
        {
            long pageCount = size / pageSize;
            for (long i = 0; i < pageCount; i++)
                createPage(i, false, true, null);
        }
        
        preload = false;
    }
    
    private RawPage createPage(long pageIndex, boolean readOnly, boolean cachingEnabled, RawPageProxy proxy)
    {
        Assert.isTrue(pageIndex >= 0);
        
        readOnly |= this.readOnly;
        long newFileSize = (pageIndex + 1) * pageSize;
        if (deleted || ((readOnly || !cachingEnabled) && newFileSize > size))
            return null;
        
        if (newFileSize > maxFileSize)
            return null;
        
        RawRegion region = loadPage(pageIndex);
        RawPage page = database.getPagePool().remove();
        if (page != null)
            page.init(pageSize, pageIndex, this, region, cachingEnabled, proxy);
        else
            page = new RawPage(pageSize, pageIndex, this, region, cachingEnabled, proxy);
        
        if (cachingEnabled)
        {
            pages.put(pageIndex, page);
            loadedPageCount++;
            
            if (newFileSize > size)
            {
                size = newFileSize;
                committedSize = size;
            }
            
            pageCache.addLoadedPage(page);
        }
        
        return page;
    }
    
    private RawRegion loadPage(long pageIndex)
    {
        Out<Boolean> fromPool = new Out<Boolean>(false);
        RawRegion region = pageCache.acquireRegion(index, pageIndex, true, !exists, fromPool);

        synchronized (this)
        {
            if (file == null && exists)
                openFile();
            
            boolean interceptResult = false;
            if (exists)
            {
                interceptResult = RawDatabaseInterceptor.INSTANCE.onBeforeFileRead(database.getInterceptId());
                int size = 0;
                try
                {
                    long pos = pageIndex * pageSize;
            
                    ByteBuffer buffer = region.getBuffer();
                    
                    boolean loaded = true;
                    while (buffer.hasRemaining())
                    {
                        int n = file.getChannel().read(buffer, pos);
                        if (n == -1)
                        {
                            if (buffer.remaining() == pageSize)
                            {
                                if (fromPool.value)
                                    region.init();
                                loaded = false;
                                break;
                            }
                            else
                                throw new RawFileCorruptedException();
                        }
                        else
                        {
                            fileCache.incrementRead(n);
                            size += n;
                        }
                        
                        pos += n;
                    }
    
                    if (loaded && !disableIOPageCacheEviction)
                        Files.advise(file.getFD(), pageIndex * pageSize, pageSize, AdviceType.POSIX_FADV_DONTNEED);
                        
                }
                catch (IOException e)
                {
                    throw new RawDatabaseException(e);
                }
                finally
                {
                    if (interceptResult)
                        RawDatabaseInterceptor.INSTANCE.onAfterFileRead(database.getInterceptId(), size);
                }
            }
        }
        
        return region;
    }
    
    private interface IMessages
    {
        @DefaultMessage("File ''{0}'' is created. File exists: {1}.")
        ILocalizedMessage fileCreated(String path, boolean exists);
        
        @DefaultMessage("New file ''{0}'' is rolled back.")
        ILocalizedMessage fileRolledBackNew(String path);

        @DefaultMessage("Existing file ''{0}'' is rolled back.")
        ILocalizedMessage fileRolledBack(String path);

        @DefaultMessage("File ''{0}'' is committed.")
        ILocalizedMessage fileCommitted(String path);

        @DefaultMessage("File ''{0}'' is closed.")
        ILocalizedMessage fileClosed(String path);

        @DefaultMessage("File ''{0}'' is truncated.")
        ILocalizedMessage fileTrucated(String path);

        @DefaultMessage("File ''{0}'' is deleted.")
        ILocalizedMessage fileDeleted(String path);

        @DefaultMessage("File ''{0}'' is synced.")
        ILocalizedMessage fileSynced(String path);

        @DefaultMessage("File ''{0}'' is set old.")
        ILocalizedMessage fileSetOld(String path);

        @DefaultMessage("File ''{0}'' is opened.")
        ILocalizedMessage fileOpened(String path);
    }
}
