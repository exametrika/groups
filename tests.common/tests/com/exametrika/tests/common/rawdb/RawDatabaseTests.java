/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.rawdb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawFileNotFoundException;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.RawPageNotFoundException;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration.Flag;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDataFile;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.rawdb.impl.RawPage;
import com.exametrika.common.rawdb.impl.RawPageCache;
import com.exametrika.common.rawdb.impl.RawPageManager;
import com.exametrika.common.rawdb.impl.RawPageProxy;
import com.exametrika.common.rawdb.impl.RawTransaction;
import com.exametrika.common.rawdb.impl.RawTransactionLog.FlushInfo;
import com.exametrika.common.rawdb.impl.RawTransactionManager;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Times;


/**
 * The {@link RawDatabaseTests} are tests for {@link RawDatabase}.
 * 
 * @see RawDatabase
 * @author Medvedev-A
 */
public class RawDatabaseTests
{
    private RawDatabase database;
    private RawDatabaseConfiguration configuration;
    private RawDatabaseConfigurationBuilder builder;
    
    @Before
    public void setUp()
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        builder = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath()).setFlushPeriod(0x8000)
             .addPageType("normal", 0x800)
                .getDefaultPageCategory().setMaxPageIdlePeriod(1000000).setMinPageCachePercentage(90).end()
             .end()   
             .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
                 new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 204800).toConfiguration())
                 .toConfiguration());
        configuration = builder.toConfiguration();
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        database.flush();
    }
    
    @After
    public void tearDown()
    {
        IOs.close(database);
    }
    
    @Test
    public void testTransactionIsolation() throws Throwable
    {
        final RawTransactionManager transactionManager = database.getTransactionManager();
        RawPageManager pageManager = database.getPageManager();
        RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getExistingPageCache("");
        SimpleList<RawPage> pages = Tests.get(pageCache, "pages");
        SimpleList<RawPage> committedPages = Tests.get(pageManager, "committedPages");
        List<FlushInfo> flushedPages = Tests.get(pageManager, "flushedPages");
        
        RawTestOperation operation = new RawTestOperation();
        RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction.bindFile(0, bindInfo);
        bindInfo.setName("custom1");
        bindInfo.setFlags(0);
        transaction.bindFile(1, bindInfo);
        
        RawPage page1 = transaction.getPage(0, 0).getPage();
        writeRegion(0, page1.getWriteRegion());
        assertThat(page1.isModified(), is(true));
        assertThat(page1.isReadOnly(), is(false));
        assertThat(page1.getSize(), is(0x800));
        assertThat(page1.getIndex(), is(0L));
        assertThat(page1.getFile().getIndex(), is(0));
        
        RawPage page2 = transaction.getPage(0, 1).getPage();
        assertThat(page2.isModified(), is(false));
        
        final RawPage page3 = transaction.getPage(1, 10).getPage();
        assertThat(page3.isReadOnly(), is(false));
        assertThat(page3.getFile().getIndex(), is(1));
        assertThat(page3.getFile().isReadOnly(), is(false));
//        new Expected(TransactionReadOnlyException.class, new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                page3.getWriteRegion();
//            }
//        });
        RawPage page4 = transaction.getPage(2, 1).getPage();
        writeRegion(2, page4.getWriteRegion());
        RawPage page5 = transaction.getPage(2, 2).getPage();
        writeRegion(3, page5.getWriteRegion());
        
        assertThat(pages.find(page3) != null, is(true));
        assertThat(pages.find(page1) != null, is(true));
        assertThat(pages.find(page2) != null, is(true));
        assertThat(pages.find(page4) != null, is(true));
        assertThat(pages.find(page5) != null, is(true));
        assertThat(committedPages.isEmpty(), is(true));
        assertThat(flushedPages.isEmpty(), is(true));
        
        transaction.run();
        assertThat(operation.beforeCommitted, is(true));
        assertThat(operation.committed, is(true));
        assertThat(operation.validated, is(true));
        assertThat(pages.find(page2) != null, is(true));
        assertThat(pages.find(page3) != null, is(true));
        
        assertThat(committedPages.find(page1) != null, is(true)); 
        assertThat(committedPages.find(page4) != null, is(true));
        assertThat(committedPages.find(page5) != null, is(true));
        
        assertThat(page1.isReadOnly(), is(true));
        assertThat(page1.getReadRegion().isReadOnly(), is(true));
        assertThat(Tests.get(page1, "writeRegion"), nullValue());
        assertThat(((IRawReadRegion)Tests.get(page1, "region")) == page1.getReadRegion(), is(true));
        
        assertThat(page2.isReadOnly(), is(true));
        assertThat(page3.isReadOnly(), is(true));
        assertThat(page4.isReadOnly(), is(true));
        assertThat(page5.isReadOnly(), is(true));
        
        operation = new RawTestOperation();
        operation.exception = new RuntimeException("test");
        transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction.bindFile(0, bindInfo);
        bindInfo.setName("custom1");
        bindInfo.setFlags(RawBindInfo.READONLY);
        transaction.bindFile(1, bindInfo);
        
        page1 = transaction.getPage(0, 0).getPage();
        checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
        writeRegion(10, page1.getWriteRegion());
        
        page2 = transaction.getPage(0, 1).getPage();
        checkRegion(page2.getReadRegion(), new ByteArray(new byte[0x800]));
        
        page4 = transaction.getPage(2, 1).getPage();
        checkRegion(page4.getReadRegion(), createBuffer(2, 0x800));
        writeRegion(11, page4.getWriteRegion());
        page5 = transaction.getPage(2, 2).getPage();
        checkRegion(page5.getReadRegion(), createBuffer(3, 0x800));
        
        transaction.run();
        assertThat(operation.committed, is(false));
        assertThat(operation.rolledBack, is(true));
        
        assertThat(pages.find(page2) != null, is(true));
        assertThat(pages.find(page3) != null, is(true));
        
        assertThat(committedPages.find(page1) != null, is(true)); 
        assertThat(committedPages.find(page4) != null, is(true));
        assertThat(committedPages.find(page5) != null, is(true));
        
        operation = new RawTestOperation();
        operation.exception = new RuntimeException("test");
        transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction.bindFile(0, bindInfo);
        bindInfo.setName("custom1");
        bindInfo.setFlags(RawBindInfo.READONLY);
        transaction.bindFile(1, bindInfo);
        
        assertThat(transaction.getPage(0, 0).getPage() == page1, is(true));
        assertThat(transaction.getPage(0, 1).getPage() == page2, is(true));
        assertThat(transaction.getPage(1, 10).getPage() == page3, is(true));
        
        page1 = transaction.getPage(0, 0).getPage();
        checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
        writeRegion(0x800, page1.getWriteRegion());
        
        page2 = transaction.getPage(0, 1).getPage();
        checkRegion(page2.getReadRegion(), new ByteArray(new byte[0x800]));
        
        page4 = transaction.getPage(2, 1).getPage();
        checkRegion(page4.getReadRegion(), createBuffer(2, 0x800));
        writeRegion(0x810, page4.getWriteRegion());
        page5 = transaction.getPage(2, 2).getPage();
        checkRegion(page5.getReadRegion(), createBuffer(3, 0x800));
        RawPage page6 = transaction.getPage(2, 3).getPage();
        
        transaction.run();
        assertThat(operation.validated, is(false));
        assertThat(operation.rolledBack, is(true));
        
        assertThat(pages.find(page2) != null, is(true));
        assertThat(pages.find(page6) != null, is(true));
        
        assertThat(committedPages.find(page1) != null, is(true)); 
        assertThat(committedPages.find(page4) != null, is(true));
        assertThat(committedPages.find(page5) != null, is(true));
        
        operation = new RawTestOperation();
        operation.validateException = new RuntimeException("test");
        transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction.bindFile(0, bindInfo);
        bindInfo.setName("custom1");
        bindInfo.setFlags(RawBindInfo.READONLY);
        transaction.bindFile(1, bindInfo);
        
        assertThat(transaction.getPage(0, 0).getPage() == page1, is(true));
        assertThat(transaction.getPage(0, 1).getPage() == page2, is(true));
        assertThat(transaction.getPage(1, 10).getPage() == page3, is(true));
        
        page1 = transaction.getPage(0, 0).getPage();
        checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
        writeRegion(0x800, page1.getWriteRegion());
        
        page2 = transaction.getPage(0, 1).getPage();
        checkRegion(page2.getReadRegion(), new ByteArray(new byte[0x800]));
        
        page4 = transaction.getPage(2, 1).getPage();
        checkRegion(page4.getReadRegion(), createBuffer(2, 0x800));
        writeRegion(110, page4.getWriteRegion());
        page5 = transaction.getPage(2, 2).getPage();
        checkRegion(page5.getReadRegion(), createBuffer(3, 0x800));
        page6 = transaction.getPage(2, 3).getPage();
        
        transaction.run();
        assertThat(operation.validated, is(true));
        assertThat(operation.rolledBack, is(true));
        
        assertThat(pages.find(page2) != null, is(true));
        assertThat(pages.find(page6) != null, is(true));
        
        assertThat(committedPages.find(page1) != null, is(true)); 
        assertThat(committedPages.find(page4) != null, is(true));
        assertThat(committedPages.find(page5) != null, is(true));
        
        operation = new RawTestOperation(true);
        operation.exception = new RuntimeException();
        transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction.bindFile(0, bindInfo);
        bindInfo.setName("custom1");
        bindInfo.setFlags(RawBindInfo.READONLY);
        transaction.bindFile(1, bindInfo);
        
        assertThat(transaction.getPage(0, 0).getPage() == page1, is(true));
        page1 = transaction.getPage(0, 0).getPage();
        assertThat(page1.isReadOnly(), is(true));
        checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
        
        page2 = transaction.getPage(0, 1).getPage();
        checkRegion(page2.getReadRegion(), new ByteArray(new byte[0x800]));
        
        page4 = transaction.getPage(2, 1).getPage();
        checkRegion(page4.getReadRegion(), createBuffer(2, 0x800));
        page5 = transaction.getPage(2, 2).getPage();
        checkRegion(page5.getReadRegion(), createBuffer(3, 0x800));
        checkRegion(page6.getReadRegion(), new ByteArray(new byte[0x800]));

        database.flush();
        
        database.stop();
        
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(final IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("custom");
                
                transaction.bindFile(0, bindInfo);
                
                try
                {
                    new Expected(RawFileNotFoundException.class, new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            RawBindInfo bindInfo = new RawBindInfo();
                            bindInfo.setName("custom1");
                            transaction.bindFile(1, bindInfo);
                        }
                    });
                }
                catch (Throwable e)
                {
                    throw new RuntimeException(e);
                }
                
                IRawPage page1 = transaction.getPage(0, 0);
                
                try
                {
                    new Expected(RawPageNotFoundException.class, new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            transaction.getPage(0, 1);
                        }
                    });
                }
                catch (Throwable e)
                {
                    throw new RuntimeException(e);
                }
                
                IRawPage page4 = transaction.getPage(2, 1);
                IRawPage page5 = transaction.getPage(2, 2);
                
                checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page4.getReadRegion(), createBuffer(2, 0x800));
                checkRegion(page5.getReadRegion(), createBuffer(3, 0x800));
            }
        });
    }
    
    @Test
    public void testFlush() throws Throwable
    {
        database.transactionSync(new RawOperation(IRawOperation.FLUSH)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                writeRegion(0, page1.getWriteRegion());
                writeRegion(0, page2.getWriteRegion());
            }
        });

        assertThat(((SimpleList)Tests.get(database.getPageManager(), "committedPages")).isEmpty(), is(true));
        
        database.stop();
        
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        database.flush();
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page2.getReadRegion(), createBuffer(0, 0x800));
            }
        });
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                for (int i = 0; i < 20; i++)
                {
                    IRawPage page = transaction.getPage(1, i);
                    writeRegion(0, page.getWriteRegion());
                }
            }
        });
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                for (int i = 0; i < 20; i++)
                {
                    IRawPage page = transaction.getPage(1, i);
                    checkRegion(page.getReadRegion(), createBuffer(0, 0x800));
                }
            }
        });
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(1, 0);
                IRawPage page2 = transaction.getPage(1, 1);
                IRawPage page3 = transaction.getPage(1, 2);
                IRawPage page4 = transaction.getPage(1, 3);
                IRawPage page5 = transaction.getPage(1, 4);
                
                writeRegion(0, page1.getWriteRegion());
                writeRegion(0, page2.getWriteRegion());
                writeRegion(0, page3.getWriteRegion());
                writeRegion(0, page4.getWriteRegion());
                writeRegion(0, page5.getWriteRegion());
            }
        });
        
        database.start();
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(1, 0);
                IRawPage page2 = transaction.getPage(1, 1);
                IRawPage page3 = transaction.getPage(1, 2);
                IRawPage page4 = transaction.getPage(1, 3);
                IRawPage page5 = transaction.getPage(1, 4);
                
                checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page2.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page3.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page4.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page5.getReadRegion(), createBuffer(0, 0x800));
            }
        });
        
        database.stop();
        
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(1, 0);
                IRawPage page2 = transaction.getPage(1, 1);
                IRawPage page3 = transaction.getPage(1, 2);
                IRawPage page4 = transaction.getPage(1, 3);
                IRawPage page5 = transaction.getPage(1, 4);
                
                checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page2.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page3.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page4.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page5.getReadRegion(), createBuffer(0, 0x800));
            }
        });
    }
    
    @Test
    public void testUnload() throws Throwable
    {
        RawTransactionManager transactionManager = database.getTransactionManager();
        
        RawTestOperation operation = new RawTestOperation(IRawOperation.FLUSH);
        RawTransaction transaction = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction.bindFile(0, bindInfo);
        for (int i = 0; i < 23; i++)
            transaction.getPage(0, i).getWriteRegion();
        
        transaction.run();
        
        database.stop();
        
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
            new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 20480).toConfiguration()).toConfiguration());

        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();

        transactionManager = database.getTransactionManager();
        RawPageCache pageCache = database.getPageTypeManager().getPageType(0).getExistingPageCache("");
        SimpleList<RawPage> pages = Tests.get(pageCache, "pages");
        
        operation = new RawTestOperation(true);
        RawTransaction transaction1 = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction1);
        bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction1.bindFile(0, bindInfo);
        
        RawPage page11 = transaction1.getPage(0, 0).getPage();
        RawPage page12 = transaction1.getPage(0, 1).getPage();
        
        assertThat(pages.find(page11) != null, is(true));
        assertThat(pages.find(page12) != null, is(true));
        
        operation = new RawTestOperation();
        RawTransaction transaction2 = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction2);
        bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction2.bindFile(0, bindInfo);
        
        IRawPage page21 = transaction2.getPage(0, 0);
        IRawPage page22 = transaction2.getPage(0, 1);
        
        assertThat(pages.isEmpty(), is(false));

        writeRegion(0, page21.getWriteRegion());
        writeRegion(0, page22.getWriteRegion());
        
        transaction2.run();
        transaction1.run();
        
        database.flush();
        
        operation = new RawTestOperation(true);
        RawTransaction transaction3 = new RawTransaction(operation, transactionManager, new Object());
        transactionManager.setTransaction(transaction3);
        bindInfo = new RawBindInfo();
        bindInfo.setName("custom");
        transaction3.bindFile(0, bindInfo);
        RawPageProxy page33 = transaction3.getPage(0, 2);
        
        RawBindInfo info = new RawBindInfo();
        info.setName("custom");
        RawDataFile file = database.getFileCache().bindFile(0, true, info);
        assertTrue(pages.toList().containsAll(Arrays.asList(file.getPage(0, true, true, null), file.getPage(1, true, true, null), 
            file.getPage(2, true, true, null))));

        file.getPage(0, true, true, null).setLastAccessTime(1);
        file.getPage(1, true, true, null).setLastAccessTime(1);
        file.getPage(2, true, true, null).setLastAccessTime(Times.getCurrentTime());

        database.start();
        Thread.sleep(2000);

        assertTrue(page33.isLoaded());
        assertThat(pages.find(page33.getPage()) != null, is(true));

        for (int i = 0; i < 20; i++)
            transaction3.getPage(0, 3 + i);
        
        Thread.sleep(3000);
        
        assertTrue(!page33.isLoaded());
        
        for (int i = 0; i < 20; i++)
            transaction3.getPage(0, 3 + i).getPage().setLastAccessTime(1);
        
        Thread.sleep(2000);
        
        file = ((TIntObjectMap<RawDataFile>)Tests.get(database.getFileCache(), "files")).valueCollection().iterator().next();
        assertThat(Tests.get(file, "file"), nullValue());
    }
    
    @Test
    public void testHeapRegions() throws Throwable
    {
        database.stop();
        
        builder.removeFlag(Flag.NATIVE_MEMORY);
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        
        testRegions();
    }
    
    @Test
    public void testNativeRegions() throws Throwable
    {
        database.stop();
        
        builder.addFlag(Flag.NATIVE_MEMORY);
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        
        testRegions();
    }
    
    @Test
    public void testRecovery() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.suspend();
        Timer timer2 = Tests.get(database.getCompartment().getGroup(), "timer");
        timer2.suspend();
        Thread.sleep(100);
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("custom");
                transaction.bindFile(0, bindInfo);
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                writeRegion(0, page1.getWriteRegion());
                writeRegion(1, page2.getWriteRegion());
            }
        });
        
        database.flush();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("custom");
                transaction.bindFile(0, bindInfo);
                
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                IRawPage page3 = transaction.getPage(1, 1);
                
                writeRegion(2, page1.getWriteRegion());
                writeRegion(3, page2.getWriteRegion());
                writeRegion(4, page3.getWriteRegion());
            }
        });
        
        RawBindInfo info = new RawBindInfo();
        info.setName("custom");
        RawDataFile dataFile = database.getFileCache().bindFile(0, true, info);
        
        dataFile.close(true);
        Tests.set(dataFile, "path", null);
        
        System.out.println("Making NullPointerException for testing recovery...");
        try
        {
            database.flush();
        }
        catch (RawDatabaseException e)
        {
        }
        
        RandomAccessFile file = Tests.get(dataFile, "file");
        IOs.close(file);
        file = Tests.get(database.getFileCache().getFile(1, true), "file");
        IOs.close(file);
        file = Tests.get(database.getFileCache().getFile(-1, true), "file");
        IOs.close(file);
        file = Tests.get(database.getTransactionLog(), "transactionLogFile");
        IOs.close(file);
        file = Tests.get(database.getTransactionLog(), "redoLogFile");
        IOs.close(file);
        
        String newPath = database.getFileCache().getFile(1, false).getPath();
        assertThat(new File(newPath).exists(), is(false));
        
        database.getTransactionManager().close();
        timer.stop();
        timer2.stop();
        Tests.set(database, "stopped", true);
        
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        timer = database.getPageManager().getTimer();
        timer.suspend();
        timer = Tests.get(database.getCompartment().getGroup(), "timer");
        timer.suspend();
        Thread.sleep(0x800);
        
        assertThat(new File(newPath).exists(), is(false));
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("custom");
                transaction.bindFile(0, bindInfo);
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page2.getReadRegion(), createBuffer(1, 0x800));
            }
        });
    }
    
    @Test
    public void testAsyncTransactions() throws Throwable
    {
        database.transaction(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                writeRegion(0, page1.getWriteRegion());
                writeRegion(1, page2.getWriteRegion());
            }
        });
        
        database.transaction(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                writeRegion(2, page1.getWriteRegion());
                writeRegion(3, page2.getWriteRegion());
            }
        });
        
        Thread.sleep(0x800);
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                checkRegion(page1.getReadRegion(), createBuffer(2, 0x800));
                checkRegion(page2.getReadRegion(), createBuffer(3, 0x800));
            }
        });
    }
    
    @Test
    public void testSyncTransactions() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                writeRegion(2, page1.getWriteRegion());
                writeRegion(3, page2.getWriteRegion());
            }
        });
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                checkRegion(page1.getReadRegion(), createBuffer(2, 0x800));
                checkRegion(page2.getReadRegion(), createBuffer(3, 0x800));
            }
        });
        
        new Expected(new ICondition<Throwable>()
        {
            @Override
            public boolean evaluate(Throwable value)
            {
                return value.getCause() instanceof RawRollbackException;
            }
        }, RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        throw new RawRollbackException();
                    }
                });
            }
        });
    }
    
    @Test
    public void testReadOnlyFiles() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("custom");
                transaction.bindFile(0, bindInfo);
                bindInfo.setName("custom1");
                transaction.bindFile(1, bindInfo);
                
                IRawPage page1 = transaction.getPage(0, 0);
                assertThat(page1.isReadOnly(), is(false));
                writeRegion(0, page1.getWriteRegion());

                IRawPage page2 = transaction.getPage(1, 0);
                assertThat(page2.isReadOnly(), is(false));
                writeRegion(0, page2.getWriteRegion());
            }
        });
        
        database.stop();
        
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("custom");
                bindInfo.setFlags(RawBindInfo.READONLY);
                transaction.bindFile(0, bindInfo);
                bindInfo.setName("custom1");
                bindInfo.setFlags(0);
                transaction.bindFile(1, bindInfo);
                
                IRawPage page1 = transaction.getPage(0, 0);
                assertThat(page1.isReadOnly(), is(true));
                
                IRawPage page2 = transaction.getPage(1, 0);
                assertThat(page2.isReadOnly(), is(false));
                writeRegion(0, page2.getWriteRegion());
            }
        }); 
    }
    
    @Test
    public void testDirectoryStructure() throws Throwable
    {
        database.stop();
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        
        builder.clearPaths()
            .addPath(new File(tempDir, "dir1").getPath())
            .addPath(new File(tempDir, "dir2").getPath());
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setPathIndex(0);
                bindInfo.setName("d1/custom");
                transaction.bindFile(0, bindInfo);
                bindInfo.setPathIndex(0);
                bindInfo.setName("d2/custom");
                transaction.bindFile(1, bindInfo);
                bindInfo.setPathIndex(1);
                bindInfo.setName("d3/custom");
                transaction.bindFile(2, bindInfo);
                bindInfo.setPathIndex(1);
                bindInfo.setName("d4/custom");
                transaction.bindFile(3, bindInfo);
                
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(1, 0);
                IRawPage page3 = transaction.getPage(2, 0);
                IRawPage page4 = transaction.getPage(3, 0);
                
                writeRegion(0, page1.getWriteRegion());
                writeRegion(1, page2.getWriteRegion());
                writeRegion(2, page3.getWriteRegion());
                writeRegion(3, page4.getWriteRegion());
            }
        });
        
        database.flush();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setPathIndex(0);
                bindInfo.setName("d1/custom");
                transaction.bindFile(0, bindInfo);
                bindInfo.setPathIndex(0);
                bindInfo.setName("d2/custom");
                transaction.bindFile(1, bindInfo);
                bindInfo.setPathIndex(1);
                bindInfo.setName("d3/custom");
                transaction.bindFile(2, bindInfo);
                bindInfo.setPathIndex(1);
                bindInfo.setName("d4/custom");
                transaction.bindFile(3, bindInfo);
                bindInfo.setPathIndex(1);
                bindInfo.setName("d4/custom2");
                transaction.bindFile(4, bindInfo);
                
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(1, 1);
                IRawPage page3 = transaction.getPage(2, 1);
                IRawPage page4 = transaction.getPage(3, 1);
                IRawPage page5 = transaction.getPage(4, 1);
                
                writeRegion(2, page1.getWriteRegion());
                writeRegion(3, page2.getWriteRegion());
                writeRegion(4, page3.getWriteRegion());
                writeRegion(5, page4.getWriteRegion());
                writeRegion(6, page5.getWriteRegion());
            }
        });
        
        RawBindInfo info = new RawBindInfo();
        info.setName("custom");
        RawDataFile dataFile = database.getFileCache().bindFile(0, true, info);
        dataFile.close(true);
        Tests.set(dataFile, "path", null);
        
        try
        {
            database.flush();
        }
        catch (RawDatabaseException e)
        {
        }
        
        info.setName("d1/custom");
        RandomAccessFile file = Tests.get(database.getFileCache().bindFile(0, true, info), "file");
        IOs.close(file);
        info.setName("d2/custom");
        file = Tests.get(database.getFileCache().bindFile(1, true, info), "file");
        IOs.close(file);
        info.setName("d3/custom");
        info.setPathIndex(1);
        file = Tests.get(database.getFileCache().bindFile(2, true, info), "file");
        IOs.close(file);
        info.setName("d4/custom");
        file = Tests.get(database.getFileCache().bindFile(3, true, info), "file");
        IOs.close(file);
        info.setName("d4/custom2");
        file = Tests.get(database.getFileCache().bindFile(4, true, info), "file");
        IOs.close(file);
        
        file = Tests.get(database.getTransactionLog(), "transactionLogFile");
        IOs.close(file);
        
        info.setName(null);
        info.setPathIndex(1);
        String newPath = database.getFileCache().bindFile(4, false, info).getPath();
        assertThat(new File(newPath).exists(), is(false));
        
        database.getPageManager().getTimer().stop();
        ((Timer)Tests.get(database.getCompartment().getGroup(), "timer")).stop();
        database.getTransactionManager().close();
        Tests.set(database, "stopped", true);
        
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        
        assertThat(new File(newPath).exists(), is(false));
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setPathIndex(0);
                bindInfo.setName("d1/custom");
                transaction.bindFile(0, bindInfo);
                bindInfo.setPathIndex(0);
                bindInfo.setName("d2/custom");
                transaction.bindFile(1, bindInfo);
                bindInfo.setPathIndex(1);
                bindInfo.setName("d3/custom");
                transaction.bindFile(2, bindInfo);
                bindInfo.setPathIndex(1);
                bindInfo.setName("d4/custom");
                transaction.bindFile(3, bindInfo);
                
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(1, 0);
                IRawPage page3 = transaction.getPage(2, 0);
                IRawPage page4 = transaction.getPage(3, 0);
                
                checkRegion(page1.getReadRegion(), createBuffer(0, 0x800));
                checkRegion(page2.getReadRegion(), createBuffer(1, 0x800));
                checkRegion(page3.getReadRegion(), createBuffer(2, 0x800));
                checkRegion(page4.getReadRegion(), createBuffer(3, 0x800));
            }
        });
    }
    
    @Test
    public void testNewFileRollback() throws Throwable
    {
        new Expected(new ICondition<Throwable>()
        {
            @Override
            public boolean evaluate(Throwable value)
            {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        transaction.getPage(0, 0).getWriteRegion();
                        throw new RuntimeException("test");
                    }
                });
            }
        });
        
        assertTrue(((TIntObjectMap)Tests.get(database.getFileCache(), "files")).size() == 1);
        
        new Expected(new ICondition<Throwable>()
        {
            @Override
            public boolean evaluate(Throwable value)
            {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        transaction.getFile(0);
                        throw new RuntimeException("test");
                    }
                });
            }
        });

        assertTrue(((TIntObjectMap)Tests.get(database.getFileCache(), "files")).size() == 1);
        
        new Expected(new ICondition<Throwable>()
        {
            @Override
            public boolean evaluate(Throwable value)
            {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        RawBindInfo info = new RawBindInfo();
                        info.setName("test");
                        transaction.bindFile(0, info);
                        throw new RuntimeException("test");
                    }
                });
            }
        });
        
        assertTrue(((TIntObjectMap)Tests.get(database.getFileCache(), "files")).size() == 1);
    }
    
    @Test
    public void testClearCaches() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setCategory("test");
                transaction.bindFile(1, bindInfo);
                
                transaction.getPage(0, 0);
                transaction.getPage(1, 0).getWriteRegion();
                
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("") != null);
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("test") != null);
            }
        });
        
        database.clearCaches();

        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("") == null);
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("test") == null);
            }
        });
    }
    
    @Test
    public void testConfigurationChange() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setCategory("test");
                transaction.bindFile(1, bindInfo);
                
                transaction.getPage(0, 0);
                transaction.getPage(1, 0).getWriteRegion();
                
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("") != null);
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("test") != null);
            }
        });
        
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
            new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 2048000).toConfiguration())
            .toConfiguration());
        database.setConfiguration(builder.toConfiguration());
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("") == null);
                assertTrue(database.getPageTypeManager().getPageType(0).getExistingPageCache("test") == null);
                
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setCategory("test");
                transaction.bindFile(1, bindInfo);
                
                transaction.getPage(0, 0);
                transaction.getPage(1, 0).getWriteRegion();
            }
        });
    }
    
    @Test
    public void testPreloading() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setCategory("test");
                transaction.bindFile(1, bindInfo);
                
                for (int i = 0; i < 100; i++)
                {
                    transaction.getPage(0, i).getWriteRegion();
                    transaction.getPage(1, i).getWriteRegion();
                }
            }
        });
        database.stop();
        
        builder.addFlag(Flag.PRELOAD_DATA);
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                try
                {
                    RawBindInfo bindInfo = new RawBindInfo();
                    bindInfo.setFlags(RawBindInfo.NOPRELOAD);
                    bindInfo.setCategory("test");
                    RawDataFile file1 = (RawDataFile)transaction.getFile(0);
                    transaction.getPage(0, 0);
                    
                    assertThat(((TLongObjectMap)Tests.get(file1, "pages")).size(), is(100));
                    RawDataFile file2 = (RawDataFile)transaction.bindFile(1, bindInfo);
                    transaction.getPage(1, 0);
                    assertThat(((TLongObjectMap)Tests.get(file2, "pages")).size(), is(1));
                }
                catch (Exception e)
                {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
        
        database.stop();
        
        builder.removeFlag(Flag.PRELOAD_DATA);
        database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                try
                {
                    RawBindInfo bindInfo = new RawBindInfo();
                    bindInfo.setFlags(RawBindInfo.PRELOAD);
                    bindInfo.setCategory("test");
                    RawDataFile file1 = (RawDataFile)transaction.getFile(0);
                    transaction.getPage(0, 0);
                    assertThat(((TLongObjectMap)Tests.get(file1, "pages")).size(), is(1));
                    RawDataFile file2 = (RawDataFile)transaction.bindFile(1, bindInfo);
                    transaction.getPage(1, 0);
                    assertThat(((TLongObjectMap)Tests.get(file2, "pages")).size(), is(100));
                }
                catch (Exception e)
                {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }
    
    @Test
    public void testPageCacheCategories() throws Throwable
    {
        database.stop();
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        
        configuration = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath()).setFlushPeriod(0x8000)
            .addPageType("type1", 0x4000)
               .getDefaultPageCategory().setMaxPageIdlePeriod(1000000).setMinPageCachePercentage(90).end()
               .addPageCategory("categoryType1").setMaxPageIdlePeriod(1000000).setInitialPageCacheSize(40000).setMinPageCachePercentage(90).end()
            .end()   
            .addPageType("type2", 0x4000)
               .getDefaultPageCategory().setMaxPageIdlePeriod(1000000).setMinPageCachePercentage(90).end()
               .addPageCategory("categoryType2").setMaxPageIdlePeriod(1000000).setInitialPageCacheSize(40000).setMinPageCachePercentage(90).end()
            .end()
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder()
                .addPolicy("native.pages.type1", new FixedAllocationPolicyConfigurationBuilder().addQuota("category1", 40000).toConfiguration())
                .addPolicy("native.pages.type2", new FixedAllocationPolicyConfigurationBuilder().addQuota("category2", 40000).toConfiguration())
                .toConfiguration()).toConfiguration();
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo info = new RawBindInfo();
                info.setPageTypeIndex(0);
                info.setCategoryType(null);
                info.setCategory(null);
                transaction.bindFile(0, info);

                info.setCategoryType("categoryType1");
                info.setCategory("category1");
                transaction.bindFile(1, info);
                
                info.setPageTypeIndex(1);
                info.setCategoryType(null);
                info.setCategory(null);
                transaction.bindFile(2, info);

                info.setCategoryType("categoryType2");
                info.setCategory("category2");
                transaction.bindFile(3, info);

                IRawPage page1 = null;
                for (int i = 0; i < 100; i++)
                {
                    IRawPage p = transaction.getPage(0, i);
                    if (i == 0)
                        page1 = p;
                }
                
                IRawPage page2 = null;
                for (int i = 0; i < 100; i++)
                {
                    IRawPage p = transaction.getPage(2, i);
                    if (i == 0)
                        page2 = p;
                }

                RawPageProxy page3 = (RawPageProxy)transaction.getPage(1, 0);
                transaction.getPage(1, 1);
                transaction.getPage(1, 2);
                assertTrue(!page3.isLoaded());
                
                RawPageProxy page4 = (RawPageProxy)transaction.getPage(3, 0);
                transaction.getPage(3, 1);
                transaction.getPage(3, 2);
                assertTrue(!page4.isLoaded());
                
                assertTrue(((RawPageProxy)page1).isLoaded());
                assertTrue(((RawPageProxy)page2).isLoaded());
            }
        });
    }
    
    @Test
    public void testDynamicPageCacheCategories() throws Throwable
    {
        database.stop();
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        
        configuration = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath()).setFlushPeriod(0x8000)
            .addPageType("type1", 0x4000)
               .getDefaultPageCategory().setMaxPageIdlePeriod(1000000).setMinPageCachePercentage(90).end()
               .addPageCategory("categoryType1").setMaxPageIdlePeriod(1000000).setInitialPageCacheSize(40000).setMinPageCachePercentage(90).end()
            .end()   
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder()
                .addPolicy("native.pages.type1", new FixedAllocationPolicyConfigurationBuilder().addQuota("category1", 40000).toConfiguration())
                .toConfiguration()).toConfiguration();
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo info = new RawBindInfo();
                info.setPageTypeIndex(0);
                info.setCategoryType(null);
                info.setCategory(null);
                transaction.bindFile(0, info);

                info.setCategoryType("categoryType1");
                info.setCategory("category1");
                transaction.bindFile(1, info);

                IRawPage page1 = null;
                for (int i = 0; i < 100; i++)
                {
                    IRawPage p = transaction.getPage(0, i);
                    if (i == 0)
                        page1 = p;
                    
                    writeRegion(i, p.getWriteRegion());
                }
                
                assertTrue(((RawPageProxy)page1).isLoaded());
                
                transaction.getFile(0).setCategory("categoryType1", "category1");
                RawPageProxy page4 = (RawPageProxy)transaction.getPage(0, 0);
                transaction.getPage(0, 1);
                transaction.getPage(0, 2);
                assertTrue(!page4.isLoaded());
            }
        });
        
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                transaction.getFile(0).setCategory(null, null);

                IRawPage page1 = null;
                for (int i = 0; i < 100; i++)
                {
                    IRawPage p = transaction.getPage(0, i);
                    if (i == 0)
                        page1 = p;
                    
                    checkRegion(p.getReadRegion(), createBuffer(i, p.getSize()));
                }
                
                assertTrue(((RawPageProxy)page1).isLoaded());
            }
        });
    }
    
    @Test
    public void testDirectoryOwner() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("dir/custom");
                bindInfo.setFlags(RawBindInfo.DIRECTORY_OWNER);
                transaction.bindFile(0, bindInfo);
                IRawPage page1 = transaction.getPage(0, 0);
                IRawPage page2 = transaction.getPage(0, 1);
                
                writeRegion(0, page1.getWriteRegion());
                writeRegion(1, page2.getWriteRegion());
            }
        });
        
        database.transactionSync(new RawOperation(IRawOperation.FLUSH)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                assertThat(new File(configuration.getPaths().get(0), "dir/custom").exists(), is(true));
                RawBindInfo bindInfo = new RawBindInfo();
                bindInfo.setName("dir/custom");
                bindInfo.setFlags(RawBindInfo.DIRECTORY_OWNER);
                IRawDataFile file = transaction.bindFile(0, bindInfo);
                file.delete();
            }
        });
        
        assertThat(new File(configuration.getPaths().get(0), "dir").exists(), is(false));
    }
    
    private void testRegions()
    {
        RawTestOperation operation = new RawTestOperation();
        RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction1 = new RawTransaction(operation, database.getTransactionManager(), new Object());
        transactionManager.setTransaction(transaction1);
        
        IRawWriteRegion region = transaction1.getPage(0, 0).getWriteRegion().getRegion(2, 98);
        region.writeByte(0, (byte)1);
        region.writeByteArray(1, new ByteArray(new byte[]{(byte)1, (byte)2, (byte)3}, 1, 2));
        region.writeChar(3, (char)4);
        region.writeCharArray(5, new char[]{(char)4, (char)5, (char)6}, 1, 2);
        region.writeString(9, "hw");
        region.writeShort(13, (short)7);
        region.writeShortArray(15, new short[]{(short)7, (short)8, (short)9}, 1, 2);
        region.writeInt(19, 10);
        region.writeIntArray(23, new int[]{10, 11, 12}, 1, 2);
        region.writeLong(31, 13);
        region.writeLongArray(39, new long[]{13, 14, 15}, 1, 2);
        region.writeDouble(55, 16);
        region.writeDoubleArray(63, new double[]{16, 17, 18}, 1, 2);
        
        assertThat(region.getLength(), is(98));
        assertThat(transaction1.getPage(0, 0).getReadRegion().readByte(0), is((byte)0));
        assertThat(transaction1.getPage(0, 0).getReadRegion().readByte(1), is((byte)0));
        assertThat(transaction1.getPage(0, 0).getReadRegion().readByte(2), is((byte)1));
        
        assertThat(region.readByte(0), is((byte)1));
        assertThat(region.readByteArray(1, 2), is(new ByteArray(new byte[]{(byte)2, (byte)3})));
        assertThat(region.readChar(3), is((char)4));
        char[] ch = new char[2];
        region.readCharArray(5, ch, 0, 2);
        assertThat(Arrays.equals(ch, new char[]{(char)5, (char)6}), is(true));
        assertThat(region.readString(9, 2), is("hw"));
        assertThat(region.readShort(13), is((short)7));
        short[] sh = new short[2];
        region.readShortArray(15, sh, 0, 2);
        assertThat(Arrays.equals(sh, new short[]{(short)8, (short)9}), is(true));
        assertThat(region.readInt(19), is(10));
        int[] in = new int[2];
        region.readIntArray(23, in, 0, 2);
        assertThat(Arrays.equals(in, new int[]{11, 12}), is(true));
        assertThat(region.readLong(31), is(13L));
        long[] lo = new long[2];
        region.readLongArray(39, lo, 0, 2);
        assertThat(Arrays.equals(lo, new long[]{14, 15}), is(true));
        assertThat(region.readDouble(55), is(16.0));
        double[] dob = new double[2];
        region.readDoubleArray(63, dob, 0, 2);
        assertThat(Arrays.equals(dob, new double[]{17, 18}), is(true));
    }

    private void writeRegion(int base, IRawWriteRegion region)
    {
        for (int i = 0; i < region.getLength(); i++)
            region.writeByte(i, (byte)(base + i));
    }
    
    private void checkRegion(IRawReadRegion region, ByteArray array)
    {
        assertThat(region.readByteArray(0, array.getLength()), is(array));
    }
    
    private ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
}