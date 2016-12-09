/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.rawdb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawPageData;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawFileNotFoundException;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.RawPageNotFoundException;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.rawdb.RawTransactionReadOnlyException;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDataFile;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.rawdb.impl.RawFileCache;
import com.exametrika.common.rawdb.impl.RawHeapReadRegion;
import com.exametrika.common.rawdb.impl.RawPage;
import com.exametrika.common.rawdb.impl.RawPageCache;
import com.exametrika.common.rawdb.impl.RawPageManager;
import com.exametrika.common.rawdb.impl.RawTransaction;
import com.exametrika.common.rawdb.impl.RawTransactionLog.FlushInfo;
import com.exametrika.common.rawdb.impl.RawTransactionManager;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tasks.impl.Daemon;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.tests.common.time.TimeServiceMock;


/**
 * The {@link RawDatabaseUnitTests} are tests for {@link RawDatabase} components.
 * 
 * @see RawDatabase
 * @author Medvedev-A
 */
public class RawDatabaseUnitTests
{
    private RawDatabase database;
    private RawDatabaseConfiguration configuration;
    private RawDatabaseConfigurationBuilder builder;
    
    @Before
    public void setUp() throws Throwable
    {
        File tempDir1 = new File(System.getProperty("java.io.tmpdir"), "db/p1");
        File tempDir2 = new File(System.getProperty("java.io.tmpdir"), "db/p2");
        Files.emptyDir(tempDir1);
        Files.emptyDir(tempDir2);

        builder = new RawDatabaseConfigurationBuilder().addPath(tempDir1.getPath()).addPath(tempDir1.getPath()).setFlushPeriod(1000)
            .addPageType("normal", 0x800)
               .getDefaultPageCategory().setMaxPageIdlePeriod(10000).setMinPageCachePercentage(90).end()
            .end()   
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
                new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 204800).toConfiguration())
                .toConfiguration());
       configuration = builder.toConfiguration();
       database = new RawDatabaseFactory().createDatabase(configuration);
       database.start();
       database.flush();
           
        Timer timer = database.getPageManager().getTimer();
        timer.suspend();
        timer = Tests.get(database.getCompartment().getGroup(), "timer");
        timer.suspend();
        Thread.sleep(200);
    }
    
    @After
    public void tearDown()
    {
        IOs.close(database);
    }
    
    @Test
    public void testFileCache() throws Throwable
    {
        RawFileCache fileCache = database.getFileCache();
        RawBindInfo info = new RawBindInfo();
        info.setName("file1");
        info.setPathIndex(1);
        RawDataFile file1 = fileCache.bindFile(10, false, info);
        assertTrue(file1.getDatabase() == database);
        assertTrue(file1.getIndex() == 10);
        assertThat(file1.getName(),  is("file1"));
        assertThat(file1.getPageSize(), is(0x800));
        assertThat(file1.getPathIndex(), is(1));
        assertThat(file1.getPath(),  is(new File(configuration.getPaths().get(1), "file1").getPath()));
        assertThat(file1.exists(),  is(false));
        assertThat(file1.isReadOnly(),  is(false));
        file1.logWritePage(1, new RawHeapReadRegion(new byte[0x800], 0, 0x800));
        assertThat(Tests.get(file1, "file") != null, is(true));
        
        assertTrue(fileCache.bindFile(10, false, info) == file1);
        assertTrue(fileCache.getFile(10, false) == file1);
        
        info = new RawBindInfo();
        info.setFlags(RawBindInfo.READONLY);
        final RawDataFile file2 = fileCache.bindFile(5, false, info);
        assertTrue(file2.getDatabase() == database);
        assertTrue(file2.getIndex() == 5);
        assertThat(file2.getName(),  is("db-5.dat"));
        assertThat(file2.getPageSize(),  is(2048));
        assertThat(file2.getPathIndex(),  is(0));
        assertThat(file2.getPath(),  is(new File(configuration.getPaths().get(0), "db-5.dat").getPath()));
        assertThat(file2.exists(),  is(false));
        assertThat(file2.isReadOnly(),  is(true));
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                file2.logWritePage(1, new RawHeapReadRegion(new byte[100], 0, 100));
            }
        });
        
        assertThat(Tests.get(file2, "file") == null, is(true));
        
        info = new RawBindInfo();
        info.setFlags(RawBindInfo.READONLY);
        assertThat(fileCache.bindFile(1, true, info), nullValue());
        
        assertThat(((TIntObjectMap)Tests.get(fileCache, "files")).size(), is(3));
        
        fileCache.removeFile(file2); 
        
        assertThat(Tests.get(file2, "file") == null, is(true));
        assertThat(((TIntObjectMap)Tests.get(fileCache, "files")).size(), is(2));
        assertThat(((TIntObjectMap)Tests.get(fileCache, "files")).get(10) == file1, is(true));
        
        fileCache.close();
        
        assertThat(Tests.get(file1, "file") == null, is(true));
        assertThat(((TIntObjectMap)Tests.get(fileCache, "files")).isEmpty(), is(true));
    }
    
    @Test
    public void testDataFile() throws Throwable
    {
        RawFileCache fileCache = database.getFileCache();
        RawBindInfo info = new RawBindInfo();
        info.setName("file1");
        info.setPathIndex(1);
        RawDataFile file1 = fileCache.bindFile(10, false, info);
        assertThat(file1.exists(),  is(false));
        assertThat(file1.getFlushSize(),  is(0l));
        
        ByteArray buffer = createBuffer(10, 0x800);
        file1.logWritePage(10, new RawHeapReadRegion(buffer.getBuffer(), buffer.getOffset(), buffer.getLength()));
        file1.logSync(true);
        assertThat(file1.exists(),  is(true));
        assertThat(file1.getFlushSize(),  is(22528l));
        
        RawPage page = file1.getPage(10, false, true, null);
        assertTrue(page.getFile() == file1);
        assertThat(page.getIndex(), is(10l));
        assertThat(page.getSize(), is(0x800));
        assertThat(page.isReadOnly(), is(true));
        assertThat(page.getReadRegion().readByteArray(0, page.getSize()), is(buffer));
        assertTrue(file1.getPage(10, true, true, null) == page);
        assertThat(file1.getSize(),  is(22528l));
        assertThat((Long)Tests.get(file1, "loadedPageCount"), is(1l));
        
        RawPage page2 = file1.getPage(5, false, true, null);
        assertThat(file1.getSize(),  is(22528l));
        assertThat((Long)Tests.get(file1, "loadedPageCount"), is(2l));
        
        RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getExistingPageCache("");
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).find(page) == page.getElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).find(page2) == page2.getElement(), is(true));
        assertThat(((Long)Tests.get(pageCache, "pageCacheSize")), is(6144l));
        file1.unloadPage(5);
        file1.unloadPage(10);
        page.setStale();
        page2.setStale();
        assertThat(Tests.get(file1, "file"), nullValue());
        assertThat((Long)Tests.get(file1, "loadedPageCount"), is(0l));
        assertThat(((TLongObjectMap)Tests.get(file1, "pages")).isEmpty(), is(true));
        
        page = file1.getPage(10, false, true, null);
        file1.logWritePage(10, new RawHeapReadRegion(buffer.getBuffer(), buffer.getOffset(), buffer.getLength()));
        file1.logTruncate(0x800);
        assertThat(file1.getFlushSize(),  is(0x800l));
        assertThat(new File(file1.getPath()).length(), is(0x800l));
        
        file1.close(false);
        file1.logDelete();
        assertThat(new File(file1.getPath()).exists(), is(false));
        
        file1.logWritePage(10, new RawHeapReadRegion(buffer.getBuffer(), buffer.getOffset(), buffer.getLength()));
        page.setStale();
        page = file1.getPage(10, false, true, null);
        file1.close(true);
        assertThat(Tests.get(file1, "file"), nullValue());
        assertThat((Long)Tests.get(file1, "loadedPageCount"), is(0l));
        assertThat(((TLongObjectMap)Tests.get(file1, "pages")).isEmpty(), is(true));
        assertTrue(file1.isStale());
        
        info = new RawBindInfo();
        info.setName("file2");
        info.setPathIndex(1);
        info.setMaxFileSize(0x800);
        RawDataFile file2 = fileCache.bindFile(11, false, info);
        
        info.setName("file3");
        assertThat(file2.getPage(10, false, true, null), nullValue());
        RawDataFile file3 = fileCache.bindFile(12, false, info);
        assertThat(file3.getPage(0, true, true, null), nullValue());
        
        info.setName("file4");
        info.setFlags(RawBindInfo.READONLY);
        RawDataFile file4 = fileCache.bindFile(13, false, info);
        assertThat(file4.getPage(0, false, true, null), nullValue());
    }
    
    @Test
    public void testPage() throws Throwable
    {
        RawFileCache fileCache = database.getFileCache();
        RawBindInfo info = new RawBindInfo();
        info.setName("file1");
        info.setPathIndex(1);
        RawDataFile file1 = fileCache.bindFile(10, false, info);
        final RawPage page = file1.getPage(0, false, true, null);
        final RawPage page2 = file1.getPage(1, false, true, null);
        assertThat(page.isReadOnly(), is(true));
        
        RawPageManager pageManager = database.getPageManager();
        RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getExistingPageCache("");
        assertTrue(((SimpleList)Tests.get(pageCache, "pages")).getLast() == page2.getElement());
        
        new Expected(RawTransactionReadOnlyException.class, new Runnable()
        {
            @Override
            public void run()
            {
                page.getWriteRegion();
            }
        });
        
        RawTestOperation operation = new RawTestOperation(true);
        final RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        new Expected(RawTransactionReadOnlyException.class, new Runnable()
        {
            @Override
            public void run()
            {
                page.getWriteRegion();
            }
        });
        
        IRawReadRegion region = page.getReadRegion();
        
        operation = new RawTestOperation();
        transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        IRawWriteRegion writeRegion = page.getWriteRegion();
        assertTrue(page.getWriteRegion() == writeRegion);
        assertTrue(page.getReadRegion() == writeRegion);
        assertTrue(((SimpleList)Tests.get(pageManager, "writePages")).getLast() == page.getWriteElement());
        writeRegion.writeLong(0, Long.MAX_VALUE);
        
        assertTrue(page.isModified());
        assertTrue(!page.isReadOnly());
        page.rollback();
        
        assertTrue(!page.isModified());
        IRawReadRegion region2 = page.getReadRegion();
        assertThat(region2.readByteArray(0, region2.getLength()), is(region.readByteArray(0, region.getLength())));
        ((SimpleList)Tests.get(pageManager, "writePages")).clear();
        
        page.getWriteElement().reset();
        page.refresh();
        writeRegion = page.getWriteRegion();
        writeRegion.writeLong(0, Long.MAX_VALUE);
        ByteArray writeBuffer = writeRegion.readByteArray(0, writeRegion.getLength());
        page.commit();
        assertTrue(!page.isModified());
        region2 = page.getReadRegion();
        assertThat(region2.readByteArray(0, region2.getLength()), not(is(region.readByteArray(0, region.getLength()))));
        ((SimpleList)Tests.get(pageManager, "writePages")).clear();
        region2 = Tests.get(page, "savedRegion");
        assertThat(region2.readByteArray(0, region2.getLength()), is(region.readByteArray(0, region.getLength())));
        
        FlushInfo flushInfo = page.flush(false);
        region2 = Tests.get(page, "savedRegion");
        assertThat(region2.readByteArray(0, region2.getLength()), not(is(region.readByteArray(0, region.getLength()))));
        assertTrue((RawDataFile)Tests.get(flushInfo, "file") == page.getFile());
        assertTrue((Long)Tests.get(flushInfo, "pageIndex") == page.getIndex());
        
        region2 = Tests.get(flushInfo, "savedRegion");
        assertThat(region2.readByteArray(0, region2.getLength()), is(region.readByteArray(0, region.getLength())));
        region2 = Tests.get(flushInfo, "region");
        assertThat(region2.readByteArray(0, region2.getLength()), is(writeBuffer));
        
        operation = new RawTestOperation(true);
        transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        Tests.set(database.getCompartment().getGroup(), "currentTime", 123);

        page.refresh();
        assertTrue(((SimpleList)Tests.get(pageCache, "pages")).getLast() != page.getElement());
        Tests.set(pageCache, "refreshIndex", 100);
        page.refresh();
        
        assertTrue(((SimpleList)Tests.get(pageCache, "pages")).getLast() == page.getElement());
        assertTrue(page.getLastAccessTime() != 0);
        
        new Expected(RawTransactionReadOnlyException.class, new Runnable()
        {
            @Override
            public void run()
            {
                page.getWriteRegion();
            }
        });
        
        page.setStale();
        assertThat(Tests.get(page, "region"), nullValue());
        assertThat(Tests.get(page, "savedRegion"), nullValue());
        assertThat(Tests.get(page, "readRegion"), nullValue());
        assertThat(Tests.get(page, "writeRegion"), nullValue());
        assertThat((Boolean)Tests.get(page, "stale"), is(true));
        assertTrue(page.isStale());
    }
    
    @Test
    public void testPageCache() throws Throwable
    {
        RawFileCache fileCache = database.getFileCache();
        RawPageManager pageManager = database.getPageManager();
        RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getExistingPageCache("");
        RawBindInfo info = new RawBindInfo();
        info.setName("file1");
        info.setPathIndex(1);
        RawDataFile file1 = fileCache.bindFile(10, false, info);
        RawPage page1 = file1.getPage(0, false, true, null);
        RawPage page2 = file1.getPage(1, false, true, null);
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).find(page1) == page1.getElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).getLast() == page2.getElement(), is(true));
        assertThat(((Long)Tests.get(pageCache, "pageCacheSize")), is(6144l));
        
        RawTestOperation operation = new RawTestOperation();
        final RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        page1.getWriteRegion();
        page2.getWriteRegion();
        
        assertThat(((SimpleList)Tests.get(pageManager, "writePages")).find(page1) == page1.getWriteElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageManager, "writePages")).find(page2) == page2.getWriteElement(), is(true));
        
        Tests.set(database.getCompartment().getGroup(), "currentTime", 123);
        Tests.set(pageCache, "refreshIndex", 100);
        page1.refresh();
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).getLast() == page1.getElement(), is(true));
        assertTrue(page1.getLastAccessTime() != 0);
        
        pageManager.commit(0);
        
        assertTrue(((SimpleList)Tests.get(pageManager, "writePages")).isEmpty());
        assertThat(((SimpleList)Tests.get(pageManager, "committedPages")).find(page1) == page1.getCommittedElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageManager, "committedPages")).find(page2) == page2.getCommittedElement(), is(true));
        assertTrue(!page1.isModified());
        assertTrue(!page2.isModified());
        
        page1.getWriteRegion().writeLong(0, Long.MAX_VALUE);
        page2.getWriteRegion().writeLong(0, Long.MIN_VALUE);
        
        assertThat(((SimpleList)Tests.get(pageManager, "writePages")).find(page1) == page1.getWriteElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageManager, "writePages")).find(page2) == page2.getWriteElement(), is(true));
        
        pageManager.commit(0);
        
        assertTrue(((SimpleList)Tests.get(pageManager, "writePages")).isEmpty());
        assertThat(((SimpleList)Tests.get(pageManager, "committedPages")).find(page1) == page1.getCommittedElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageManager, "committedPages")).find(page2) == page2.getCommittedElement(), is(true));
        assertTrue(!page1.isModified());
        assertTrue(!page2.isModified());
        assertTrue(page1.getReadRegion().readLong(0) == Long.MAX_VALUE);
        assertTrue(page2.getReadRegion().readLong(0) == Long.MIN_VALUE);
        
        page1.getWriteRegion().writeLong(0, Long.MAX_VALUE / 2);
        page2.getWriteRegion().writeLong(0, Long.MIN_VALUE / 2);
        
        pageManager.rollback(false);
        assertTrue(((SimpleList)Tests.get(pageManager, "writePages")).isEmpty());
        assertThat(((SimpleList)Tests.get(pageManager, "committedPages")).find(page1) == page1.getCommittedElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageManager, "committedPages")).find(page2) == page2.getCommittedElement(), is(true));
        assertTrue(!page1.isModified());
        assertTrue(!page2.isModified());
        assertTrue(page1.getReadRegion().readLong(0) == Long.MAX_VALUE);
        assertTrue(page2.getReadRegion().readLong(0) == Long.MIN_VALUE);
        
        page1.getWriteRegion().writeLong(0, Long.MAX_VALUE);
        page2.getWriteRegion().writeLong(0, Long.MIN_VALUE);
        
        pageCache.removePage(page1);
        pageCache.removePage(page2);
        
        assertTrue(((SimpleList)Tests.get(pageCache, "pages")).toList().size() == 1);
        assertTrue(((SimpleList)Tests.get(pageManager, "writePages")).isEmpty());
        assertTrue(((SimpleList)Tests.get(pageManager, "committedPages")).isEmpty());
        assertThat(((Long)Tests.get(pageCache, "pageCacheSize")), is(2048l));
        assertTrue(page1.isStale());
        assertTrue(page2.isStale());
    }
    
    @Test
    public void testPageCacheFlush() throws Throwable
    {
        RawFileCache fileCache = database.getFileCache();
        RawPageManager pageManager = database.getPageManager();
        RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getExistingPageCache("");
        RawBindInfo info = new RawBindInfo();
        info.setName("file1");
        info.setPathIndex(1);
        RawDataFile file1 = fileCache.bindFile(10, false, info);
        RawPage page1 = file1.getPage(0, false, true, null);
        RawPage page2 = file1.getPage(1, false, true, null);
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).find(page1) == page1.getElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).getLast() == page2.getElement(), is(true));
        assertThat(((Long)Tests.get(pageCache, "pageCacheSize")), is(6144l));
        
        RawTestOperation operation = new RawTestOperation();
        final RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        IRawReadRegion readRegion1 = page1.getReadRegion();
        IRawReadRegion readRegion2 = page2.getReadRegion();
        IRawWriteRegion writeRegion1 = page1.getWriteRegion();
        IRawWriteRegion writeRegion2 = page2.getWriteRegion();
        writeRegion1.writeLong(0, Long.MAX_VALUE);
        writeRegion2.writeLong(0, Long.MIN_VALUE);
        
        ByteArray writeBuffer1 = writeRegion1.readByteArray(0, writeRegion1.getLength());
        ByteArray writeBuffer2 = writeRegion2.readByteArray(0, writeRegion2.getLength());
        pageManager.commit(0);
        
        Tests.set(database.getCompartment().getGroup(), "currentTime", 123);
        
        pageManager.flush(false);
        assertTrue(((SimpleList)Tests.get(pageManager, "committedPages")).isEmpty());
        assertThat((Long)Tests.get(pageManager, "lastFlushToMemoryTime"), is(123l));
        
        List<FlushInfo> flushedPages = Tests.get(pageManager, "flushedPages");
        assertTrue(flushedPages.size() == 2);
        FlushInfo flushInfo = flushedPages.get(0);
        assertTrue((RawDataFile)Tests.get(flushInfo, "file") == page1.getFile());
        assertTrue((Long)Tests.get(flushInfo, "pageIndex") == page1.getIndex());
        
        IRawReadRegion region2 = Tests.get(flushInfo, "savedRegion");
        assertThat(region2.readByteArray(0, region2.getLength()), is(readRegion1.readByteArray(0, readRegion1.getLength())));
        region2 = Tests.get(flushInfo, "region");
        assertThat(region2.readByteArray(0, region2.getLength()), is(writeBuffer1));
        
        flushInfo = flushedPages.get(1);
        assertTrue((RawDataFile)Tests.get(flushInfo, "file") == page2.getFile());
        assertTrue((Long)Tests.get(flushInfo, "pageIndex") == page2.getIndex());
        
        region2 = Tests.get(flushInfo, "savedRegion");
        assertThat(region2.readByteArray(0, region2.getLength()), is(readRegion2.readByteArray(0, readRegion2.getLength())));
        region2 = Tests.get(flushInfo, "region");
        assertThat(region2.readByteArray(0, region2.getLength()), is(writeBuffer2));
        
        pageManager.flushPendingPages(true);
        assertTrue(flushedPages.isEmpty());
    }

    @Test
    public void testPageCacheTimer() throws Throwable
    {
        database.clearCaches();
        TimeServiceMock timeService = new TimeServiceMock();
        timeService.useSystemTime = false;
        Tests.set(database.getCompartment().getGroup(), "timeService", timeService);
        RawFileCache fileCache = database.getFileCache();
        RawPageManager pageManager = database.getPageManager();
        RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getPageCache("", "");
        Tests.set(database.getCompartment().getGroup(), "currentTime", 123);
        timeService.time = 123;
        pageManager.flush(false);
        
        RawBindInfo info = new RawBindInfo();
        info.setName("file1");
        info.setPathIndex(1);
        RawDataFile file1 = fileCache.bindFile(10, false, info);
        RawPage page1 = file1.getPage(0, false, true, null);
        RawPage page2 = file1.getPage(1, false, true, null);
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).find(page1) == page1.getElement(), is(true));
        assertThat(((SimpleList)Tests.get(pageCache, "pages")).getLast() == page2.getElement(), is(true));
        assertThat(((Long)Tests.get(pageCache, "pageCacheSize")), is(4096l));
        
        RawTestOperation operation = new RawTestOperation();
        final RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        IRawWriteRegion writeRegion1 = page1.getWriteRegion();
        IRawWriteRegion writeRegion2 = page2.getWriteRegion();
        writeRegion1.writeLong(0, Long.MAX_VALUE);
        writeRegion2.writeLong(0, Long.MIN_VALUE);
        
        pageManager.commit(0);
        
        pageManager.onTimer(123);
        
        assertTrue(!((SimpleList)Tests.get(pageCache, "pages")).isEmpty());
        assertTrue(!((SimpleList)Tests.get(pageManager, "committedPages")).isEmpty());
        
        Tests.set(database.getCompartment().getGroup(), "currentTime", 2001);
        timeService.time = 2001;
        pageManager.onTimer(2001);
        assertTrue(((SimpleList)Tests.get(pageManager, "committedPages")).isEmpty());
        List<FlushInfo> flushedPages = Tests.get(pageManager, "flushedPages");
        assertTrue(flushedPages.size() == 2);
        
        Tests.set(database.getCompartment().getGroup(), "currentTime", 20001);
        timeService.time = 20001;
        Tests.set(pageCache, "refreshIndex", 100);
        page1.refresh();
        pageManager.onTimer(20001);
        pageCache.onTimer(20001);
        
        assertTrue(((SimpleList)Tests.get(pageCache, "pages")).find(page1) != null);
        assertTrue(((SimpleList)Tests.get(pageCache, "pages")).find(page2) == null);
        assertTrue(!page1.isStale());
        assertTrue(page2.isStale());
        assertThat((Long)Tests.get(file1, "loadedPageCount"), is(1l));
        
        Tests.set(database.getCompartment().getGroup(), "currentTime", 40001);
        timeService.time = 40001;
        pageManager.onTimer(40001);
        pageCache.onTimer(40001);
        
        assertTrue(((SimpleList)Tests.get(pageCache, "pages")).isEmpty());
        assertTrue(page1.isStale());
        assertThat((Long)Tests.get(file1, "loadedPageCount"), is(0l));
        assertTrue(Tests.get(file1, "file") == null);
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testPageCacheFiles() throws Throwable
    {
        database.clearCaches();
        Daemon main = Tests.get(database.getCompartment(), "mainThread");
        Thread thread = Tests.get(main, "daemonThread");
        thread.suspend();
        
        try
        {
            RawFileCache fileCache = database.getFileCache();
            RawPageManager pageManager = database.getPageManager();
            RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getPageCache("", "");
            Tests.set(pageManager, "lastFlushToMemoryTime", 0);
            Tests.set(database.getCompartment().getGroup(), "currentTime", 123);
            
            RawBindInfo info = new RawBindInfo();
            info.setName("file1");
            info.setPathIndex(1);
            final RawDataFile file1 = fileCache.bindFile(10, false, info);
            info.setName("file2");
            final RawDataFile file2 = fileCache.bindFile(11, false, info);
            
            RawTestOperation operation = new RawTestOperation();
            final RawTransactionManager transactionManager = database.getTransactionManager();
            RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
            transactionManager.setTransaction(transaction);
            
            RawPage page1 = file1.getPage(0, false, true, null);
            page1.getWriteRegion().writeLong(0, 123);
            RawPage page2 = file1.getPage(1, false, true, null);
            page2.getWriteRegion().writeLong(0, 123);
            RawPage page3 = file2.getPage(0, false, true, null);
            page3.getWriteRegion().writeLong(0, 123);
            RawPage page4 = file2.getPage(1, false, true, null);
            page4.getWriteRegion().writeLong(0, 123);
            transaction.run();
            Tests.set(transactionManager, "transaction", null);
            
            new Expected(IllegalStateException.class, new Runnable()
            {
                @Override
                public void run()
                {
                    file1.truncate(0x800);
                }
            });
            new Expected(IllegalStateException.class, new Runnable()
            {
                @Override
                public void run()
                {
                    file1.delete();
                }
            });
            
            operation = new RawTestOperation(true);
            transaction = new RawTransaction(operation, transactionManager, new Object());
            transactionManager.setTransaction(transaction);
            new Expected(IllegalStateException.class, new Runnable()
            {
                @Override
                public void run()
                {
                    file1.truncate(0x800);
                }
            });
            new Expected(IllegalStateException.class, new Runnable()
            {
                @Override
                public void run()
                {
                    file1.delete();
                }
            });
            
            operation = new RawTestOperation();
            transaction = new RawTransaction(operation, transactionManager, new Object());
            transactionManager.setTransaction(transaction);
            
            assertThat((Long)Tests.get(pageManager, "lastFlushToMemoryTime"), is(0l));
            file1.truncate(0x800);
            assertThat((Long)Tests.get(pageManager, "lastFlushToMemoryTime"), is(123l));
            assertThat((Boolean)Tests.get(file1, "truncated"), is(true));
            assertThat(file1.getSize(), is(0x800l));
            assertThat((List<RawDataFile>)Tests.get(pageManager, "writeFiles"), is(Arrays.asList(file1)));
            assertThat((Long)Tests.get(file1, "loadedPageCount"), is(1l));
            assertThat(((TLongObjectMap)Tests.get(file1, "pages")).size(), is(1));
            assertThat(((TLongObjectMap)Tests.get(file1, "pages")).containsKey(0l), is(true));
            assertThat(((SimpleList<RawPage>)Tests.get(pageCache, "pages")).toList(), is(Arrays.asList(page1, page3, page4)));
            assertTrue(!page1.isStale());
            assertTrue(page2.isStale());
            
            Tests.set(database.getCompartment().getGroup(), "currentTime", 124);
            file1.truncate(0);
            assertThat((Long)Tests.get(pageManager, "lastFlushToMemoryTime"), is(123l));
            assertThat((Boolean)Tests.get(file1, "truncated"), is(true));
            assertThat(file1.getSize(), is(0l));
            assertThat((List<RawDataFile>)Tests.get(pageManager, "writeFiles"), is(Arrays.asList(file1)));
            assertThat((Long)Tests.get(file1, "loadedPageCount"), is(0l));
            assertTrue(((TLongObjectMap)Tests.get(file1, "pages")).isEmpty());
            assertThat(((SimpleList<RawPage>)Tests.get(pageCache, "pages")).toList(), is(Arrays.asList(page3, page4)));
            assertTrue(page1.isStale());
            
            file2.delete();
            
            assertThat(file2.getPage(0, false, true, null), nullValue());
            
            new Expected(IllegalStateException.class, new Runnable()
            {
                @Override
                public void run()
                {
                    file2.truncate(0);
                }
            });
            assertThat((Long)Tests.get(pageManager, "lastFlushToMemoryTime"), is(124l));
            assertThat((Boolean)Tests.get(file2, "deleted"), is(true));
            assertThat((List<RawDataFile>)Tests.get(pageManager, "writeFiles"), is(Arrays.asList(file1, file2)));
            assertThat((Long)Tests.get(file2, "loadedPageCount"), is(0l));
            assertTrue(((TLongObjectMap)Tests.get(file2, "pages")).isEmpty());
            assertThat(((SimpleList<Object>)Tests.get(pageCache, "pages")).toList(), is(Arrays.asList()));
            assertTrue(page3.isStale());
            assertTrue(page4.isStale());
            
            pageManager.rollback(false);
            assertThat((Boolean)Tests.get(file1, "truncated"), is(false));
            assertTrue(file1.getSize() == 4096l);
            assertThat((Boolean)Tests.get(file2, "deleted"), is(false));
            assertTrue(file2.getSize() == 4096l);
            assertTrue(((List)Tests.get(pageManager, "writeFiles")).isEmpty());
            
            page1 = file1.getPage(0, false, true, null);
            page2 = file1.getPage(1, false, true, null);
            page3 = file2.getPage(0, false, true, null);
            page4 = file2.getPage(1, false, true, null);
            
            file1.truncate(0);
            file2.delete();
            
            Tests.set(database.getCompartment().getGroup(), "currentTime", 125);
            pageManager.commit(0);
            
            assertThat((Boolean)Tests.get(file1, "truncated"), is(false));
            assertThat((Long)Tests.get(file1, "committedSize"), is(0l));
            
            assertThat(((TIntObjectMap)Tests.get(fileCache, "files")).size(), is(1));
            assertTrue(((TIntObjectMap)Tests.get(fileCache, "files")).containsKey(10));
            
            assertThat((Long)Tests.get(pageManager, "lastFlushToMemoryTime"), is(125l));
            assertTrue(((List)Tests.get(pageManager, "writeFiles")).isEmpty());
            assertTrue(((List)Tests.get(pageManager, "committedFiles")).isEmpty());
            assertTrue(((List)Tests.get(pageManager, "flushedFiles")).isEmpty());
        }
        finally
        {
            thread.resume();
        }
    }
    
   @Test
    public void testTransaction() throws Throwable
    {
        RawPageManager pageManager = database.getPageManager();
        RawTestOperation operation = new RawTestOperation();
        RawTestOperation operation2 = new RawTestOperation(true);
        RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
        final RawTransaction transaction2 = new RawTransaction(operation2, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        RawBindInfo info = new RawBindInfo();
        info.setPathIndex(1);
        info.setName("file1");
        info.setFlags(0);
        info.setMaxFileSize(1000000);
        RawDataFile file1 = transaction.bindFile(10, info);
        assertTrue(file1.getDatabase() == database);
        assertTrue(file1.getIndex() == 10);
        assertThat(file1.getName(),  is("file1"));
        assertThat(file1.getPageSize(),  is(0x800));
        assertThat(file1.getPathIndex(),  is(1));
        assertThat(file1.getPath(),  is(new File(configuration.getPaths().get(1), "file1").getPath()));
        assertThat(file1.isReadOnly(),  is(false));
        
        assertTrue(transaction.getFile(10) == file1);
        
        new Expected(RawFileNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                transaction2.getFile(0);
                
            }
        });
        
        new Expected(RawPageNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                transaction2.getPage(10, 0);
            }
        });
        
        RawPage page = transaction.getPage(10, 11).getPage();
        assertTrue(page.getFile() == file1);
        assertTrue(page.getIndex() == 11);
        assertTrue(!page.isReadOnly());
        assertTrue(page.getSize() == 0x800);
        assertTrue(page.getWriteRegion() != null);
        
        Tests.set(transactionManager, "transaction", transaction2);
        transactionManager.setTransaction(transaction2);
        page = transaction.getPage(10, 11).getPage();
        assertTrue(page.isReadOnly());
        transaction2.run();
        
        assertTrue(operation2.committed);
        assertTrue(!operation2.validated);
        assertTrue(!operation2.beforeCommitted);
        assertTrue(!operation2.rolledBack);
        transaction2.waitCompleted();
        operation2.committed = false;
        operation2.validated = false;
        
        transactionManager.setTransaction(transaction);
        page.refresh();
        assertTrue(!page.isReadOnly());
        
        page.getWriteRegion().writeLong(0, 123);
        transaction.run();
        transaction.waitCompleted();
        
        assertTrue(operation.validated);
        assertTrue(operation.beforeCommitted);
        assertTrue(operation.committed);
        
        assertTrue(page.isReadOnly());
        assertTrue(page.getReadRegion().readLong(0) == 123);
        
        assertTrue(((SimpleList)Tests.get(pageManager, "writePages")).isEmpty());
        assertThat(((SimpleList)Tests.get(pageManager, "committedPages")).find(page) == page.getCommittedElement(), is(true));
        
        operation = new RawTestOperation(IRawOperation.FLUSH);
        RawTransaction transaction3 = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction3);
        transaction3.run();
        transaction3.waitCompleted();
        
        assertTrue(((SimpleList)Tests.get(pageManager, "committedPages")).isEmpty());
        assertTrue(((List)Tests.get(pageManager, "flushedPages")).isEmpty());
        
        operation.validateException = new RuntimeException("test");
        final RawTransaction transaction4 = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction4);
        
        page.refresh();
        assertTrue(!page.isReadOnly());
        
        page.getWriteRegion().writeLong(0, 1234);
        
        transaction4.run();
        
        assertTrue(operation.rolledBack);
        assertTrue(page.getReadRegion().readLong(0) == 123);
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                transaction4.waitCompleted();
            }
        });
        
        operation.rolledBack = false;
        operation.validateException = null;
        operation.exception = new RuntimeException("test");
        final RawTransaction transaction5 = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction5);
        
        page.refresh();
        assertTrue(!page.isReadOnly());
        
        page.getWriteRegion().writeLong(0, 567);
        
        transaction5.run();
        
        assertTrue(operation.rolledBack);
        assertTrue(page.getReadRegion().readLong(0) == 123);
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                transaction5.waitCompleted();
            }
        });
    }
    
    @Test
    public void testDurableCommit()
    {
        IOs.close(database);
        
        File tempDir1 = new File(System.getProperty("java.io.tmpdir"), "db/p1");
        File tempDir2 = new File(System.getProperty("java.io.tmpdir"), "db/p2");
        Files.emptyDir(tempDir1);
        Files.emptyDir(tempDir2);
        builder = new RawDatabaseConfigurationBuilder().addPath(tempDir1.getPath()).addPath(tempDir1.getPath()).setFlushPeriod(1000000)
            .addPageType("normal", 0x2000)
               .getDefaultPageCategory().setMaxPageIdlePeriod(10000).setMinPageCachePercentage(90).end()
            .end()   
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
                new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 100000000).toConfiguration())
                .toConfiguration());
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        for (int i = 0; i < 10; i++)
        {
            final int a = i;
            database.transactionSync(new RawOperation(IRawOperation.DURABLE)
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int i = 0; i < 10; i++)
                    {
                        for (int k = 0; k < 10; k++)
                        {
                            IRawPage page = transaction.getPage(i, k);
                            page.getWriteRegion().writeByteArray(0, createBuffer(i * k + a, 0x2000));
                        }
                    }
                }
            });
        }
        IOs.close(database);
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        
        for (int i = 0; i < 10; i++)
        {
            database.transactionSync(new RawOperation(true)
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int i = 0; i < 10; i++)
                    {
                        for (int k = 0; k < 10; k++)
                        {
                            IRawPage page = transaction.getPage(i, k);
                            Assert.checkState(page.getReadRegion().readByteArray(0, 0x2000).equals(createBuffer(i * k + 9, 0x2000)));
                        }
                    }
                }
            });
        }
    }
    
    @Test
    public void testDurableRecover() throws Throwable
    {
        IOs.close(database);
        File tempDir1 = new File(System.getProperty("java.io.tmpdir"), "db/p1");
        File tempDir2 = new File(System.getProperty("java.io.tmpdir"), "db/p2");
        Files.emptyDir(tempDir1);
        Files.emptyDir(tempDir2);
        builder = new RawDatabaseConfigurationBuilder().addPath(tempDir1.getPath()).addPath(tempDir1.getPath()).setFlushPeriod(1000000)
            .addPageType("normal", 0x2000)
               .getDefaultPageCategory().setMaxPageIdlePeriod(10000).setMinPageCachePercentage(90).end()
            .end()   
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
                new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 100000000).toConfiguration())
                .toConfiguration());
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        for (int i = 0; i < 10; i++)
        {
            final int a = i;
            database.transactionSync(new RawOperation(IRawOperation.DURABLE)
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int i = 0; i < 10; i++)
                    {
                        for (int k = 0; k < 10; k++)
                        {
                            IRawPage page = transaction.getPage(i, k);
                            page.getWriteRegion().writeByteArray(0, createBuffer(i * k + a, 0x2000));
                        }
                    }
                }
            });
        }
        ((SimpleList)Tests.get(database.getPageManager(), "committedPages")).clear();
        IOs.close(database);
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        
        for (int i = 0; i < 10; i++)
        {
            database.transactionSync(new RawOperation(true)
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int i = 0; i < 10; i++)
                    {
                        for (int k = 0; k < 10; k++)
                        {
                            IRawPage page = transaction.getPage(i, k);
                            Assert.checkState(page.getReadRegion().readByteArray(0, 0x2000).equals(createBuffer(i * k + 9, 0x2000)));
                        }
                    }
                }
            });
        }
    }
    
    @Test
    public void testPageData() throws Throwable
    {
        final TestPageData data = new TestPageData();
        
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(final IRawTransaction transaction)
                    {
                        IRawPage page = transaction.getPage(0, 0);
                        data.page = page;
                        page.setData(data);
                        
                        page.getWriteRegion();
                        
                        throw new RawRollbackException(new NonLocalizedMessage("test"));
                    }
                });
            }
        });
        
        assertThat(data.unloaded, is(true));
        data.unloaded = false;
        assertThat(data.rolledBack, is(false));
        assertThat(data.beforeCommitted, is(false));
        assertThat(data.committed, is(false));
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(final IRawTransaction transaction)
            {
                transaction.getPage(0, 0);
            }
        });
        
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(final IRawTransaction transaction)
                    {
                        IRawPage page = transaction.getPage(0, 0);
                        data.page = page;
                        page.setData(data);
                        
                        page.getWriteRegion();
                        
                        throw new RawRollbackException(new NonLocalizedMessage("test"));
                    }
                });
            }
        });
        
        assertThat(data.rolledBack, is(true));
        data.rolledBack = false;
        assertThat(data.beforeCommitted, is(false));
        assertThat(data.committed, is(false));
        assertThat(data.unloaded, is(false));
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(final IRawTransaction transaction)
            {
                IRawPage page = transaction.getPage(0, 0);
                page.getWriteRegion();
            }
        });
        
        assertThat(data.rolledBack, is(false));
        assertThat(data.beforeCommitted, is(true));
        assertThat(data.committed, is(true));
        assertThat(data.unloaded, is(false));
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(final IRawTransaction transaction)
            {
                IRawPage page = transaction.getPage(0, 0);
                assertThat(page.getReadRegion().readLong(0), is(987654321l));
            }
        });

        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(final IRawTransaction transaction)
            {
                transaction.getFile(0).delete();
            }
        });
        
        assertThat(data.unloaded, is(true));
    }
    
    private ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
    
    public static class TransactionLogInterceptor
    {
        private static int failureIndex = -1;
        public static void onLine(int index, int version, Object instance)
        {
            if (index == failureIndex)
                throw new RuntimeException("test");
        }
    }
    
    public static class TestPageData implements IRawPageData
    {
        private boolean beforeCommitted;
        private boolean committed;
        private boolean rolledBack;
        private boolean unloaded;
        private IRawPage page;
        
        @Override
        public void onBeforeCommitted()
        {
            beforeCommitted = true;
            page.getWriteRegion().writeLong(0, 987654321);
        }

        @Override
        public void onCommitted()
        {
            committed = true;
        }

        @Override
        public void onRolledBack()
        {
            rolledBack = true;
        }

        @Override
        public void onUnloaded()
        {
            unloaded = true;
        }
    }
}