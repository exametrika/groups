/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.rawdb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.compartment.ICompartmentQueue.Event;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBatchLock;
import com.exametrika.common.rawdb.RawBatchLock.Type;
import com.exametrika.common.rawdb.RawBatchOperation;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.rawdb.impl.RawDbBatchOperation;
import com.exametrika.common.rawdb.impl.RawDbBatchOperationState;
import com.exametrika.common.rawdb.impl.RawPage;
import com.exametrika.common.rawdb.impl.RawPageProxy;
import com.exametrika.common.rawdb.impl.RawTransaction;
import com.exametrika.common.rawdb.impl.RawTransactionQueue;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Times;


/**
 * The {@link RawBatchTests} are tests for batch transactions.
 * 
 * @author Medvedev-A
 */
public class RawBatchTests
{
    private RawDatabaseConfiguration configuration;
    private RawDatabase database;
    
    @Before
    public void setUp()
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        
        configuration = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath()).toConfiguration();
        database = new RawDatabaseFactory().createDatabase(configuration, null, false, new TestBatchContext(), null);
        database.start();
    }
    
    @After
    public void tearDown()
    {
        IOs.close(database);
    }
    
    @Test
    public void testBatch() throws Throwable
    {
        final TestBatchOperation operation = new TestBatchOperation();
        operation.invalid = true;
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(operation);
            }
        });
        
        assertThat(operation.rolledBackCount, is(1));
        
        final TestBatchOperation operation2 = new TestBatchOperation();
        database.transactionSync(operation2);
        
        assertThat(operation2.committedCount, is(1));
        assertThat(operation2.runCount, is(1000));
        assertThat(((RawDbBatchOperation)operation2.batchControl).isCompleted(), is(true));
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawWriteRegion region = transaction.getPage(0, 0).getWriteRegion();
                assertThat(region.readInt(0), is(1000));
                region.writeInt(0, 0);
            }
        });
        
        final TestBatchOperation operation3 = new TestBatchOperation();
        operation3.exception = new RuntimeException("test");
        
        database.transactionSync(operation3);
        
        assertThat(operation3.committedCount, is(1));
        assertThat(operation3.runCount, is(5));
        assertThat(((RawDbBatchOperation)operation3.batchControl).isCompleted(), is(true));
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawWriteRegion region = transaction.getPage(0, 0).getWriteRegion();
                assertThat(region.readInt(0), is(5));
                region.writeInt(0, 0);
            }
        });
        
        System.out.println("Generating NullPointerException for testing batch failures...");
        final TestBatchOperation operation4 = new TestBatchOperation();
        operation4.locks = Arrays.asList(new RawBatchLock(Type.EXCLUSIVE, "test"));
        operation4.failure = true;
        try
        {
            database.transactionSync(operation4);
        }
        catch (RawDatabaseException e)
        {
        }
         
        try
        {
            database.stop();
        }
        catch (Exception e)
        {
        }
        database = new RawDatabaseFactory().createDatabase(configuration, null, false, new TestBatchContext(), null);
        database.start();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public List<String> getBatchLockPredicates()
            {
                return Arrays.asList("test");
            }
            
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawWriteRegion region = transaction.getPage(0, 0).getWriteRegion();
                assertThat(region.readInt(0), is(1000));
            }
        });
    }
    
    @Test
    public void testSequentialBatchExecution() throws Throwable
    {
        for (int i = 0; i < 10; i++)
        {
            TestBatchOperation2 operation = new TestBatchOperation2(i);
            operation.locks = Arrays.asList(new RawBatchLock(Type.EXCLUSIVE, "test")); 
            database.transaction(operation);
        }
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public List<String> getBatchLockPredicates()
            {
                return Arrays.asList("test");
            }
            
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawReadRegion region = transaction.getPage(0, 0).getReadRegion();
                assertThat(region.readInt(0), is(10000));
            }
        });
    }
    
    @Test
    public void testCooperativeTransactionExecution() throws Throwable
    {
        final TestBatchOperation3 operation = new TestBatchOperation3();
        operation.locks = Arrays.asList(new RawBatchLock(Type.EXCLUSIVE, "a")); 
        database.transaction(operation);
        
        final int[] count = new int[1];
        for (int i = 0; i < 100; i++)
        {
            final int n = i;
            database.transaction(new RawOperation()
            {
                List<String> locks = Arrays.asList("b");
                
                @Override
                public void run(IRawTransaction transaction)
                {
                    if (operation.runCount >= 7 && operation.runCount <= 12)
                        count[0]++;
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch (Exception e)
                    {
                        Exceptions.wrapAndThrow(e);
                    }
                    
                    System.out.println(n + " - " + Times.getCurrentTime());
                }
                
                @Override
                public List<String> getBatchLockPredicates()
                {
                    return locks;
                }
            });
        }
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
            }
        });
        
        assertThat(count[0] > 10, is(true));
        assertThat(operation.exits > 0, is(true));
        System.out.println("final - " + Times.getCurrentTime() + ", exits - " + operation.exits);
    }
    
    @Test
    public void testLocks() throws Throwable
    {
        RawDbBatchOperation batch1 = new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(new TestBatchOperation3()), true, 100, 900);
        RawDbBatchOperation batch2 = new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(new TestBatchOperation3(Arrays.asList(
                new RawBatchLock(Type.EXCLUSIVE, "a.b")))), true, 100, 900);
        RawDbBatchOperation batch3 = new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(new TestBatchOperation3(Arrays.asList(
                new RawBatchLock(Type.SHARED, "a.b")))), true, 100, 900);
        RawDbBatchOperation batch4 = new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(new TestBatchOperation3(Arrays.asList(
                new RawBatchLock(Type.EXCLUSIVE, "a.b")))), true, 100, 900);
        IRawOperation op1 = new TestOperation();
        IRawOperation op2 = new TestOperation(Arrays.asList("a"));
        IRawOperation op3 = new TestOperation(Arrays.asList("a.b.c"));
        IRawOperation op4 = new TestOperation(Arrays.asList("a.c"));
        IRawOperation op5 = new TestOperation(Arrays.asList("a.b.c"));
        
        assertThat(batch1.allow(false, op1), is(true));
        assertThat(batch1.allow(false, op2), is(true));
        
        assertThat(batch2.allow(false, op1), is(true));
        assertThat(batch2.allow(false, op2), is(false));
        assertThat(batch2.allow(true, op2), is(false));
        assertThat(batch2.allow(false, op3), is(false));
        assertThat(batch2.allow(true, op3), is(false));
        assertThat(batch2.allow(false, op4), is(true));
        assertThat(batch2.allow(true, op4), is(true));
        
        assertThat(batch3.allow(false, op1), is(true));
        assertThat(batch3.allow(false, op2), is(false));
        assertThat(batch3.allow(true, op2), is(true));
        assertThat(batch3.allow(false, op3), is(false));
        assertThat(batch3.allow(true, op3), is(true));
        assertThat(batch3.allow(false, op4), is(true));
        assertThat(batch3.allow(true, op4), is(true));
        
        assertThat(batch4.allow(false, op2), is(false));
        assertThat(batch4.allow(false, op4), is(true));
        assertThat(batch4.allow(false, op5), is(false));
    }
    
    @Test
    public void testTransactionQueue() throws Throwable
    {
        RawTransactionQueue queue = new RawTransactionQueue();
        assertThat(queue.poll(false), nullValue());
        Event transaction1 = new Event(new RawTransaction(new TestOperation(), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction2 = new Event(new RawTransaction(new TestOperation(), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction3 = new Event(new RawTransaction(new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(new TestBatchOperation3(Arrays.asList(
                new RawBatchLock(Type.EXCLUSIVE, "a.b")))), true, 100, 900), database.getTransactionManager(), null), 
                database.getCompartment(), 1);
        Event transaction4 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.c")), database.getTransactionManager(), null),
            database.getCompartment(), 1); 
        Event transaction5 = new Event(new RawTransaction(new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(new TestBatchOperation3(Arrays.asList(
                new RawBatchLock(Type.EXCLUSIVE, "a.c")))), true, 100, 900), database.getTransactionManager(), null), 
                database.getCompartment(), 1);
        Event transaction6 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.c")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction7 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.c")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction8 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction9 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction10 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        
        queue.offer(transaction1);
        queue.offer(transaction2);
        queue.offer(transaction3);
        queue.offer(transaction4);
        queue.offer(transaction5);
        queue.offer(transaction6);
        queue.offer(transaction7);
        queue.offer(transaction8);
        queue.offer(transaction9);
        assertThat(queue.getCapacity(), is(9));
        
        assertThat(queue.poll(false) == transaction1, is(true));
        assertThat(queue.getCapacity(), is(8));
        assertThat(queue.poll(false) == transaction2, is(true));
        assertThat(queue.getCapacity(), is(7));
        assertThat(queue.poll(false) == transaction3, is(true));
        assertThat(Tests.get(queue, "currentBatchTransaction") == transaction3, is(true));
        Tests.set(((IRawTransaction)transaction3.task).getOperation(), "startTime", 0);
        assertThat(queue.poll(false) == transaction3, is(true));
        assertThat(queue.getCapacity(), is(7));
        Tests.set(((IRawTransaction)transaction3.task).getOperation(), "startTime", Times.getCurrentTime());
        assertThat(queue.poll(false) == transaction4, is(true));
        assertThat(queue.getCapacity(), is(6));
        assertThat(queue.poll(false), nullValue());
        assertThat(queue.poll(true) == transaction3, is(true));
        assertThat(queue.poll(true) == transaction3, is(true));
        assertThat(queue.poll(true) == transaction3, is(true));
        assertThat(queue.getCapacity(), is(6));
        assertThat((List<Event>)Tests.get(queue, "pendingTransactions"), is(Arrays.asList(transaction5)));
        assertThat(((SimpleList<Event>)Tests.get(((IRawTransaction)transaction3.task).getOperation(), "pendingTransactions")).toList(), 
            is(Arrays.asList(transaction9, transaction8)));
        assertThat(((SimpleList<Event>)Tests.get(((IRawTransaction)transaction5.task).getOperation(), "pendingTransactions")).toList(), 
            is(Arrays.asList(transaction7, transaction6)));
        
        Tests.set(((IRawTransaction)transaction3.task).getOperation(), "completed", true);
        assertThat(queue.poll(false) == transaction8, is(true));
        assertThat(queue.getCapacity(), is(4));
        assertThat(Tests.get(queue, "currentBatchTransaction"), nullValue());
        assertThat(queue.poll(false) == transaction9, is(true));
        assertThat(queue.getCapacity(), is(3));
        
        queue.offer(transaction10);
        assertThat(queue.getCapacity(), is(4));
        
        assertThat(queue.poll(false) == transaction5, is(true));
        assertThat(Tests.get(queue, "currentBatchTransaction") == transaction5, is(true));
        Tests.set(((IRawTransaction)transaction5.task).getOperation(), "completed", true);
        assertThat(queue.getCapacity(), is(4));
        
        assertThat(queue.poll(false) == transaction6, is(true));
        assertThat(queue.getCapacity(), is(2));
        assertThat(queue.poll(false) == transaction7, is(true));
        assertThat(queue.getCapacity(), is(1));
        assertThat(queue.poll(false) == transaction10, is(true));
        assertThat(queue.poll(false), nullValue());
        assertThat(queue.getCapacity(), is(0));
    }
    
    @Test
    public void testDynamicLocks() throws Throwable
    {
        RawTransactionQueue queue = new RawTransactionQueue();
        assertThat(queue.poll(false), nullValue());
        Event transaction1 = new Event(new RawTransaction(new RawDbBatchOperation(database.getBatchManager(), database.getTransactionManager(),
            database.getPageTypeManager(), new RawDbBatchOperationState(new TestBatchOperation3(Arrays.asList(
                new RawBatchLock(Type.EXCLUSIVE, "a.b")))), true, 100, 900), database.getTransactionManager(), null), 
                database.getCompartment(), 1);
        Event transaction2 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b.c")), database.getTransactionManager(), null),
            database.getCompartment(), 1); 
        Event transaction3 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b.c")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction4 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b.d")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction5 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b.d")), database.getTransactionManager(), null),
            database.getCompartment(), 1);
        Event transaction6 = new Event(new RawTransaction(new TestOperation(Arrays.asList("a.b.d")), database.getTransactionManager(), null),
            database.getCompartment(), 1);

        Tests.set(((IRawTransaction)transaction1.task).getOperation(), "startTime", Times.getCurrentTime());
        
        queue.offer(transaction1);
        queue.offer(transaction2);
        queue.offer(transaction3);
        queue.offer(transaction4);
        queue.offer(transaction5);
        assertThat(queue.getCapacity(), is(5));
        
        assertThat(queue.poll(false) == transaction1, is(true));
        assertThat(queue.poll(false), nullValue());
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.getCapacity(), is(5));
        
        assertThat(((SimpleList<Event>)Tests.get(((IRawTransaction)transaction1.task).getOperation(), "pendingTransactions")).toList(), 
            is(Arrays.asList(transaction5, transaction4, transaction3, transaction2)));
        ((TestBatchOperation3)((RawDbBatchOperation)((IRawTransaction)transaction1.task).getOperation()).getOperation()).locks = Arrays.asList(new RawBatchLock(Type.EXCLUSIVE, "a.b.d"));
        
        assertThat(queue.poll(false) == transaction2, is(true));
        assertThat(queue.getCapacity(), is(4));
        assertThat(queue.poll(false) == transaction3, is(true));
        assertThat(queue.getCapacity(), is(3));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.getCapacity(), is(3));
        
        queue.offer(transaction6);
        assertThat(queue.getCapacity(), is(4));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.getCapacity(), is(4));
        
        assertThat(((SimpleList<Event>)Tests.get(((IRawTransaction)transaction1.task).getOperation(), "pendingTransactions")).toList(), 
            is(Arrays.asList(transaction6, transaction5, transaction4)));
        
        ((TestBatchOperation3)((RawDbBatchOperation)((IRawTransaction)transaction1.task).getOperation()).getOperation()).locks = Collections.emptyList();
        
        assertThat(queue.poll(false) == transaction4, is(true));
        assertThat(queue.getCapacity(), is(3));
        assertThat(queue.poll(false) == transaction5, is(true));
        assertThat(queue.getCapacity(), is(2));
        assertThat(queue.poll(false) == transaction6, is(true));
        assertThat(queue.getCapacity(), is(1));
        assertThat(queue.poll(true) == transaction1, is(true));
        assertThat(queue.getCapacity(), is(1));
        Tests.set(((IRawTransaction)transaction1.task).getOperation(), "completed", true);
        assertThat(queue.poll(false), nullValue());
        assertThat(queue.getCapacity(), is(0));
    }
    
    @Test
    public void testNonCachedPages() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                transaction.getPage(0, 0).getWriteRegion();
            }
        });
        
        database.clearCaches();
        
        database.transactionSync(new TestBatchOperation4());
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawPage page = ((RawPageProxy)transaction.getPage(0, 0)).getPage();
                assertTrue(page.isCached());
            }
        });
        
        Thread.sleep(1000);
    }
    
    @Test
    public void testReadOnlyBatchTransaction() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                transaction.getPage(0, 0).getWriteRegion();
            }
        });
        
        database.clearCaches();
        
        database.transactionSync(new TestBatchOperation4(true));
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawPage page = ((RawPageProxy)transaction.getPage(0, 0)).getPage();
                assertTrue(page.isCached());
            }
        });
        
        Thread.sleep(1000);
    }
    
    @Test
    public void testPageCacheConstraints() throws Throwable
    {
        database.stop();
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        
        configuration = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath()).setFlushPeriod(0x8000)
            .addPageType("normal", 0x4000)
               .getDefaultPageCategory().setMaxPageIdlePeriod(1000000).setMinPageCachePercentage(90).end()
            .end()   
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
                new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 20000000).toConfiguration())
                .toConfiguration()).toConfiguration();
        database = new RawDatabaseFactory().createDatabase(configuration, null, false, new TestBatchContext(), null);
        database.start();
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                for (int i = 0; i < 100; i++)
                    transaction.getPage(0, i).getWriteRegion();
            }
        });
        
        database.clearCaches();
        
        database.transactionSync(new TestBatchOperation5());
        
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawPage page = null;
                for (int i = 0; i < 100; i++)
                {
                    IRawPage p = transaction.getPage(0, i);
                    if (i == 0)
                        page = p;
                }
                
                assertTrue(((RawPageProxy)page).isLoaded());
            }
        });
        
        Thread.sleep(1000);
    }
    
    private static class TestBatchOperation extends RawBatchOperation
    {
        private int runCount;
        private boolean invalid;
        private RuntimeException exception;
        private transient boolean failure;
        private int committedCount;
        private int rolledBackCount;
        private transient IRawBatchControl batchControl;
        private List<RawBatchLock> locks = Collections.emptyList();
        
        @Override
        public void validate(IRawTransaction transaction)
        {
            if (invalid)
                throw new RawRollbackException(new NonLocalizedMessage("test"));
        }

        @Override
        public boolean run(IRawTransaction transaction, IRawBatchControl batchControl)
        {
            this.batchControl = batchControl;
            
            if (runCount == 5)
            {
                if (exception != null)
                    throw exception;
            }
            
            IRawWriteRegion region = transaction.getPage(0, 0).getWriteRegion();
            assertThat(region.readInt(0), is(runCount));
            region.writeInt(0, region.readInt(0) + 1);
            runCount++;
            
            if (runCount == 6)
            {
                if (failure)
                {
                    RawDbBatchOperation batch = (RawDbBatchOperation)batchControl;
                    try
                    {
                        Tests.set(batch, "state", null);
                    }
                    catch (Exception e)
                    {
                        Exceptions.wrapAndThrow(e);
                    }
                }
            }

            return runCount >= 1000;
        }

        @Override
        public int getOptions()
        {
            return 0;
        }

        @Override
        public int getSize()
        {
            return 1;
        }

        @Override
        public List<RawBatchLock> getLocks()
        {
            return locks;
        }

        @Override
        public void setContext(IRawBatchContext context)
        {
            TestBatchContext batchContext = (TestBatchContext)context;
            Assert.isTrue(batchContext.opened);
        }

        @Override
        public void onBeforeCommitted(boolean completed)
        {
        }

        @Override
        public void onCommitted(boolean completed)
        {
            if (completed)
                committedCount++;
        }

        @Override
        public void onRolledBack(boolean clearCache)
        {
            rolledBackCount++;
        }
    }
    
    private static class TestBatchContext implements IRawBatchContext
    {
        private boolean opened;
        
        @Override
        public <T> T getContext()
        {
            return null;
        }

        @Override
        public void open()
        {
            opened = true;
        }

        @Override
        public UUID getExtensionId()
        {
            return UUID.randomUUID();
        }
    }
    
    private static class TestBatchOperation2 extends RawBatchOperation
    {
        private final int n;
        int runCount;
        private List<RawBatchLock> locks = Collections.emptyList();
        
        public TestBatchOperation2(int n)
        {
            this.n = n;
        }
        
        @Override
        public boolean run(IRawTransaction transaction, IRawBatchControl batchControl)
        {
            IRawWriteRegion region = transaction.getPage(0, 0).getWriteRegion();
            assertThat(region.readInt(0), is(n * 1000 + runCount));
            region.writeInt(0, region.readInt(0) + 1);
            runCount++;
            
            return runCount >= 1000;
        }
        
        @Override
        public List<RawBatchLock> getLocks()
        {
            return locks;
        }
    };
    
    private static class TestBatchOperation3 extends RawBatchOperation
    {
        int runCount;
        int exits;
        private List<RawBatchLock> locks = Collections.emptyList();
        
        public TestBatchOperation3()
        {
        }
        
        public TestBatchOperation3(List<RawBatchLock> locks)
        {
            this.locks = locks;
        }
        
        @Override
        public boolean run(IRawTransaction transaction, IRawBatchControl batchControl)
        {
            while (batchControl.canContinue())
            {
                System.out.println("batch run count - " + runCount + ", time - " + Times.getCurrentTime());
                runCount++;
                try
                {
                    Thread.sleep(10);
                }
                catch (Exception e)
                {
                    Exceptions.wrapAndThrow(e);
                }
            }
            
            exits++;
            return runCount >= 100;
        }

        @Override
        public List<RawBatchLock> getLocks()
        {
            return locks;
        }
    };
    
    private class TestOperation extends RawOperation
    {
        private List<String> locks;
        
        public TestOperation()
        {
        }
        
        public TestOperation(List<String> locks)
        {
            this.locks = locks;
        }
        
        @Override
        public void run(IRawTransaction transaction)
        {
        }
        
        @Override
        public List<String> getBatchLockPredicates()
        {
            return locks;
        }
    };
    
    private static class TestBatchOperation4 extends RawBatchOperation
    {
        int i;
        
        public TestBatchOperation4()
        {
        }
        
        public TestBatchOperation4(boolean readOnly)
        {
            super(readOnly);
        }
        
        @Override
        public boolean run(IRawTransaction transaction, IRawBatchControl batchControl)
        {
            if (i == 0)
            {
                batchControl.setPageCachingEnabled(false);
                batchControl.setNonCachedPagesInvalidationQueueSize(2);
            }
            
            IRawPage page1 = transaction.getPage(0, 0);
            assertTrue(!(((RawPageProxy)page1).getPage().isCached()));
            assertTrue(((RawPageProxy)page1).isLoaded());
            
            IRawPage page2 = transaction.getPage(0, 0);
            assertTrue(!(((RawPageProxy)page2).getPage().isCached()));
            assertTrue(((RawPageProxy)page2).isLoaded());
            assertTrue(page1 != page2);
            
            IRawPage page3 = transaction.getPage(0, 0);
            assertTrue(!(((RawPageProxy)page2).getPage().isCached()));
            assertTrue(((RawPageProxy)page3).isLoaded());
            assertTrue(!((RawPageProxy)page1).isLoaded());
            
            if (i == 1)
            {
                batchControl.setNonCachedPagesInvalidationQueueSize(1);
                assertTrue(!((RawPageProxy)page2).isLoaded());
                
                batchControl.setPageCachingEnabled(true);
                
                RawPage page = ((RawPageProxy)transaction.getPage(0, 0)).getPage();
                assertTrue(page.isCached());
            }
            
            i++;
            
            try
            {
                Thread.sleep(300);
            }
            catch (Exception e)
            {
                Exceptions.wrapAndThrow(e);
            }
            return i == 2;
        }
    };
    
    private static class TestBatchOperation5 extends RawBatchOperation
    {
        int i;
        
        public TestBatchOperation5()
        {
        }
        
        @Override
        public boolean run(IRawTransaction transaction, IRawBatchControl batchControl)
        {
            if (i == 0)
            {
                transaction.getPage(0, 0);
                batchControl.setMaxPageCacheSize(0, "", 40000);
            }
            
            RawPageProxy page1 = (RawPageProxy)transaction.getPage(0, 0);
            transaction.getPage(0, 1);
            transaction.getPage(0, 2);
            assertTrue(!page1.isLoaded());
            i++;
            
            try
            {
                Thread.sleep(300);
            }
            catch (Exception e)
            {
                Exceptions.wrapAndThrow(e);
            }
            return i == 2;
        }
    };
}
