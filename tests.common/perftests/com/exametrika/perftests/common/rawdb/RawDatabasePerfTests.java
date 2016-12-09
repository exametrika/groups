/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.rawdb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.IRawDataFile.ReadMode;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.rawdb.impl.RawTransaction;
import com.exametrika.common.rawdb.impl.RawTransactionManager;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.resource.config.UniformAllocationPolicyConfiguration;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.tests.common.rawdb.RawTestOperation;


/**
 * The {@link RawDatabasePerfTests} are performance tests for database framework.
 * 
 * @author Medvedev-A
 */
public class RawDatabasePerfTests
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(RawDatabasePerfTests.class);
    private RawDatabase database;
    private RawDatabaseConfigurationBuilder parameters;
    
    @Before
    public void setUp() throws Throwable
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        
        parameters = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath())
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder(500000000)
                .setDefaultPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration())    
            .addPageType("normal", 0x4000)
                .getDefaultPageCategory()
                    .setMinPageCachePercentage(90)
                .end()
            .end();
        database = new RawDatabaseFactory().createDatabase(parameters.toConfiguration());
        database.start();
        
        Timer timer = database.getPageManager().getTimer();
        timer.suspend();
        Thread.sleep(100);
    }
    
    @After
    public void tearDown()
    {
        IOs.close(database);
    }
    
    @Test
    public void testRegions() throws Throwable
    {
        final RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction3 = new RawTransaction(new RawTestOperation(), database.getTransactionManager(), new Object());
        transactionManager.setTransaction(transaction3);
        
        final IRawWriteRegion region = transaction3.getPage(0, 0).getWriteRegion();
        final int[] r1 = new int[1];
        final long[] r2 = new long[1];
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.regionCount(1000000000));
        logger.log(LogLevel.INFO, messages.readRegionInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                {
                    for (int i = 0; i < 10000 - 4; i++)
                        r1[0] += region.readInt(i);
                }
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.writeRegionInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                    for (int i = 0; i < 10000 - 4; i++)
                        region.writeInt(i, k);
            }
        }, 1)));
        
        logger.log(LogLevel.INFO, messages.readRegionLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                {
                    for (int i = 0; i < 10000 - 8; i++)
                        r2[0] += region.readLong(i);
                }
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.writeRegionLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                    for (int i = 0; i < 10000 - 8; i++)
                        region.writeLong(i, k);
            }
        }, 1)));
        
        System.out.println(r1[0] + r2[0]);
        
        final ByteBuffer buffer1 = ByteBuffer.allocate(10000);
        buffer1.order(ByteOrder.LITTLE_ENDIAN);
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.readBufferInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                {
                    for (int i = 0; i < 10000 - 4; i++)
                        r1[0] += buffer1.getInt(i);
                }
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.writeBufferInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                    for (int i = 0; i < 10000 - 4; i++)
                        buffer1.putInt(i, k);
            }
        }, 1)));
        
        logger.log(LogLevel.INFO, messages.readBufferLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                {
                    for (int i = 0; i < 10000 - 8; i++)
                        r2[0] += buffer1.getLong(i);
                }
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.writeBufferLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                    for (int i = 0; i < 10000 - 8; i++)
                        buffer1.putLong(i, k);
            }
        }, 1)));
        
        System.out.println(r1[0] + r2[0]);
        
        final ByteBuffer buffer2 = ByteBuffer.allocateDirect(10000);
        buffer2.order(ByteOrder.LITTLE_ENDIAN);
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.readDirectBufferInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                {
                    for (int i = 0; i < 10000 - 4; i++)
                        r1[0] += buffer2.getInt(i);
                }
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.writeDirectBufferInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                    for (int i = 0; i < 10000 - 4; i++)
                        buffer2.putInt(i, k);
            }
        }, 1)));
        
        logger.log(LogLevel.INFO, messages.readDirectBufferLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                {
                    for (int i = 0; i < 10000 - 8; i++)
                        r2[0] += buffer2.getLong(i);
                }
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.writeDirectBufferLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                for (int k = 0; k < 100000; k++)
                    for (int i = 0; i < 10000 - 8; i++)
                        buffer2.putLong(i, k);
            }
        }, 1)));
        
        System.out.println(r1[0] + r2[0]);
    }
    
    @Test
    public void testPages() throws Throwable
    {
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                for (int k = 0; k < 128; k++)
                {
                    for (int i = 0; i < 100; i++)
                        transaction.getPage(k, i);
                }
            }
        });
        
        final int COUNT = 1000000;
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.getReadTransactionPage(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        for (int k = 0; k < COUNT; k++)
                        {
                            int n = k & 127;
                            for (int i = 0; i < 100; i++)
                                transaction.getPage(n, i);
                        }
                    }
                });     
            }
        }, 1, 0)));

        logger.log(LogLevel.INFO, messages.getSingleReadTransactionPage(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        for (int k = 0; k < 100 * COUNT; k++)
                            transaction.getPage(0, 0);
                    }
                });
            }
        }, 1, 0)));

        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.getWriteTransactionPage(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        for (int k = 0; k < COUNT; k++)
                        {
                            int n = k & 127;
                            for (int i = 0; i < 100; i++)
                                transaction.getPage(n, i).getWriteRegion();
                        }
                    }
                });
            }
        }, 1, 0)));
    }
    
    @Test
    public void testTransaction() throws Throwable
    {
        final int COUNT = 1000000;
        final int BATCH_SIZE = 40;
        
        final int[] count = new int[1];
        
        logger.log(LogLevel.INFO, messages.separator());

        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                transaction.getPage(0, 0);
            }
        });
        
        logger.log(LogLevel.INFO, messages.readTransaction(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                count[0] = 0;
                for (int k = 0; k < COUNT; k++)
                {
                    database.transaction(new RawOperation(true)
                    {
                        @Override
                        public void run(IRawTransaction transaction)
                        {
                            transaction.getPage(0, 0).getReadRegion();
                            count[0]++;
                        }
                    });
                }
                
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        transaction.getPage(0, 0);
                    }
                });
            }
        }, 1, 0)));
        
        assertThat(count[0], is(COUNT));
        
        logger.log(LogLevel.INFO, messages.readTransactionList(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                count[0] = 0;
                List<IRawOperation> operations = new ArrayList<IRawOperation>(BATCH_SIZE);
                for (int i = 0; i < BATCH_SIZE; i++)
                {
                    operations.add(new RawOperation(true)
                    {
                        @Override
                        public void run(IRawTransaction transaction)
                        {
                            transaction.getPage(0, 0).getReadRegion();
                            count[0]++;
                        }
                    });
                }
                for (int k = 0; k < COUNT / BATCH_SIZE; k++)
                {
                    database.transaction(operations);
                }
                
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        transaction.getPage(0, 0);
                    }
                });
            }
        }, 1, 0)));
        
        assertThat(count[0], is(COUNT));
        
        logger.log(LogLevel.INFO, messages.writeTransaction(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                count[0] = 0;
                
                for (int k = 0; k < COUNT; k++)
                {
                    database.transaction(new RawOperation()
                    {
                        @Override
                        public void run(IRawTransaction transaction)
                        {
                            transaction.getPage(0, 0).getWriteRegion();
                            count[0]++;
                        }
                    });
                }
                
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        transaction.getPage(0, 0);
                    }
                });
            }
        }, 1, 0)));
        
        assertThat(count[0], is(COUNT));
        
        logger.log(LogLevel.INFO, messages.writeTransactionList(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                count[0] = 0;
                List<IRawOperation> operations = new ArrayList<IRawOperation>(BATCH_SIZE);
                for (int i = 0; i < BATCH_SIZE; i++)
                {
                    operations.add(new RawOperation()
                    {
                        @Override
                        public void run(IRawTransaction transaction)
                        {
                            transaction.getPage(0, 0).getWriteRegion();
                            count[0]++;
                        }
                    });
                }
                for (int k = 0; k < COUNT / BATCH_SIZE; k++)
                {
                    database.transaction(operations);
                }
                
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        transaction.getPage(0, 0);
                    }
                });
            }
        }, 1, 0)));
        
        assertThat(count[0], is(COUNT));
    }
    
    @Test
    public void testSmallScalability() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();
        
        System.out.println("============== Test small scalability...");
        System.out.println("Creating DB...");
        long l = Times.getCurrentTime();
        long t = Times.getCurrentTime();
        for (int m = 0; m < 1000; m++)
        {
            boolean sync = false;
            if (m > 0 && (m % 10) == 0)
                sync = true;
            
            final int k = m;
            IRawOperation operation = new RawOperation()
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int m = 0; m < 1000; m++)
                        writeRegion(k, transaction.getPage(k, m).getWriteRegion());
                }
            };
            
            if (sync)
                database.transactionSync(operation);
            else
                database.transaction(operation);
            
            if (m > 0 && (m % 10) == 0)
            {
                System.out.println(Times.getCurrentTime() - t + " " + m);
                t = Times.getCurrentTime();
            }
        }
        
        System.out.println(Times.getCurrentTime() - l);
        
        System.out.println("Checking DB...");
        l = Times.getCurrentTime();
        t = Times.getCurrentTime();
        for (int m = 0; m < 1000; m++)
        {
            boolean sync = false;
            if (m > 0 && (m % 10) == 0)
                sync = true;
            
            final int k = m;
            IRawOperation operation = new RawOperation()
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int m = 0; m < 1000; m++)
                        checkRegion(k, transaction.getPage(k, m).getReadRegion());
                }
            };
            
            if (sync)
                database.transactionSync(operation);
            else
                database.transaction(operation);
            
            if (m > 0 && (m % 10) == 0)
            {
                System.out.println(Times.getCurrentTime() - t + " " + m);
                t = Times.getCurrentTime();
            }
        }
        
        System.out.println(Times.getCurrentTime() - l);
    }
    
    @Test
    public void testSmallScalabilitySmallTransactions() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();
        
        System.out.println("============== Test small scalability small transactions...");
        System.out.println("Creating DB...");
        long l = Times.getCurrentTime();
        long t = Times.getCurrentTime();
        for (int s = 0; s < 100; s++)
        {
            for (int m = 0; m < 100; m++)
            {
                boolean sync = false;
                int p = s * 100 + m;
                if (p > 0 && (p % 100) == 0)
                    sync = true;
                
                final int k = m;
                final int shift = s * 10;
                IRawOperation operation = new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        for (int m = 0; m < 100; m++)
                        {
                            if (shift > 0 && m < 90)
                                checkRegion(k + m + shift, transaction.getPage(k, m + shift).getReadRegion());
                            writeRegion(k + m + shift, transaction.getPage(k, m + shift).getWriteRegion());
                        }
                    }
                };
                
                if (sync)
                    database.transactionSync(operation);
                else
                    database.transaction(operation);
                
                if (p > 0 && (p % 100) == 0)
                {
                    System.out.println(Times.getCurrentTime() - t + " " + p);
                    t = Times.getCurrentTime();
                }
            }
        }
        
        System.out.println(Times.getCurrentTime() - l);
        
        System.out.println("Checking DB...");
        l = Times.getCurrentTime();
        t = Times.getCurrentTime();
        for (int s = 0; s < 100; s++)
        {
            for (int m = 0; m < 100; m++)
            {
                boolean sync = false;
                int p = s * 100 + m;
                if (p > 0 && (p % 100) == 0)
                    sync = true;
                
                final int k = m;
                final int shift = s * 10;
                IRawOperation operation = new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        for (int m = 0; m < 100; m++)
                            checkRegion(k + m + shift, transaction.getPage(k, m + shift).getReadRegion());
                    }
                };
                
                if (sync)
                    database.transactionSync(operation);
                else
                    database.transaction(operation);
                
                if (p > 0 && (p % 100) == 0)
                {
                    System.out.println(Times.getCurrentTime() - t + " " + p);
                    t = Times.getCurrentTime();
                }
            }
        }
        
        System.out.println(Times.getCurrentTime() - l);
    }
    
    @Test
    public void testDurableScalability() throws Throwable
    {
        IOs.close(database);
        parameters.setFlushPeriod(20000);
        database = new RawDatabaseFactory().createDatabase(parameters.toConfiguration());
        database.start();
        
        System.out.println("============== Test durable scalability...");
        System.out.println("Creating DB...");
        long l = Times.getCurrentTime();
        long t = Times.getCurrentTime();
        for (int m = 0; m < 1000; m++)
        {
            boolean sync = false;
            if (m > 0 && (m % 10) == 0)
                sync = true;
            
            final int k = 0;
            IRawOperation operation = new RawOperation(IRawOperation.DURABLE)
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int m = 0; m < 100; m++)
                        writeRegion(k, transaction.getPage(k, m).getWriteRegion());
                }
            };
            if (sync)
                database.transactionSync(operation);
            else
                database.transaction(operation);
            
            if (m > 0 && (m % 10) == 0)
            {
                System.out.println(Times.getCurrentTime() - t + " " + m);
                t = Times.getCurrentTime();
            }
        }
        
        System.out.println(Times.getCurrentTime() - l);
        
        System.out.println("Checking DB...");
        l = Times.getCurrentTime();
        t = Times.getCurrentTime();
        for (int m = 0; m < 1000; m++)
        {
            boolean sync = false;
            if (m > 0 && (m % 10) == 0)
                sync = true;
            
            final int k = 0;
            IRawOperation operation = new RawOperation(IRawOperation.DURABLE)
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    for (int m = 0; m < 100; m++)
                        checkRegion(k, transaction.getPage(k, m).getReadRegion());
                }
            };
            if (sync)
                database.transactionSync(operation);
            else
                database.transaction(operation);
            
            if (m > 0 && (m % 10) == 0)
            {
                System.out.println(Times.getCurrentTime() - t + " " + m);
                t = Times.getCurrentTime();
            }
        }
        
        System.out.println(Times.getCurrentTime() - l);
    }
    
    @Test
    public void testNewLargeScalability() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();
        
        long l = Times.getCurrentTime();
        System.out.println("============== New large scalability...");
        System.out.println("Creating DB...");
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    writeRegion(m, transaction.getPage(m / 1000, m % 1000).getWriteRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
        
        System.out.println("Checking DB...");
        l = Times.getCurrentTime();
        database.transaction(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    checkRegion(m, transaction.getPage(m / 1000, m % 1000).getReadRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
    }
    
    @Test
    public void testNewLargeRollbackScalability() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();
        
        long l = Times.getCurrentTime();

        System.out.println("============== Test new large rollback scalability...");
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        long t = Times.getCurrentTime();        
                        
                        for (int m = 0; m < 100000; m++)
                        {
                            writeRegion(m, transaction.getPage(m / 1000, m % 1000).getWriteRegion());
                            
                            if (m > 0 && (m % 10000) == 0)
                            {
                                System.out.println(Times.getCurrentTime() - t + " " + m);
                                t = Times.getCurrentTime();
                            }
                        }
                        
                        throw new RuntimeException("test");
                    }
                });
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
    }

    @Test
    public void testExistingLargeScalability() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();
        
        System.out.println("============== Test existing large scalability...");
        System.out.println("Creating DB...");
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    writeRegion(m, transaction.getPage(m / 1000, m % 1000).getWriteRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        
        long l = Times.getCurrentTime();
        
        System.out.println("Modifying DB...");
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    writeRegion(m + 0x1717, transaction.getPage(m / 1000, m % 1000).getWriteRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
        
        l = Times.getCurrentTime();
        
        System.out.println("Checking DB...");
        database.transaction(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    checkRegion(m + 0x1717, transaction.getPage(m / 1000, m % 1000).getReadRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
    }
    
    @Test
    public void testExistingLargeRollbackScalability() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();
        
        System.out.println("============== Test existing large rollback scalability...");
        System.out.println("Creating DB...");
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    writeRegion(m, transaction.getPage(m / 1000, m % 1000).getWriteRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        
        long l = Times.getCurrentTime();
        
        System.out.println("Modifying DB...");
        new Expected(RawDatabaseException.class, new Runnable()
        {
            @Override
            public void run()
            {
                database.transactionSync(new RawOperation()
                {
                    @Override
                    public void run(IRawTransaction transaction)
                    {
                        long t = Times.getCurrentTime();        
                        
                        for (int m = 0; m < 100000; m++)
                        {
                            writeRegion(m + 0x1717, transaction.getPage(m / 1000, m % 1000).getWriteRegion());
                            
                            if (m > 0 && (m % 10000) == 0)
                            {
                                System.out.println(Times.getCurrentTime() - t + " " + m);
                                t = Times.getCurrentTime();
                            }
                        }
                        
                        throw new RuntimeException("test");
                    }
                });
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
        
        System.out.println("Checking DB...");
        l = Times.getCurrentTime();
        database.transaction(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    checkRegion(m, transaction.getPage(m / 1000, m % 1000).getReadRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        System.out.println(Times.getCurrentTime() - l);
    }
    
    @Test
    public void testFlushScalability() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();

        System.out.println("============== Test flush scalability...");
        long l = Times.getCurrentTime();

        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long t = Times.getCurrentTime();        
                
                for (int m = 0; m < 100000; m++)
                {
                    writeRegion(m, transaction.getPage(m % 1000, m / 1000).getWriteRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - t + " " + m);
                        t = Times.getCurrentTime();
                    }
                }
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
    }
    
    @Test
    public void testReadMode() throws Throwable
    {
        Timer timer = database.getPageManager().getTimer();
        timer.resume();
        
        System.out.println("============== Test read mode...");
        System.out.println("Creating DB...");
        long l = Times.getCurrentTime();
        database.transactionSync(new RawOperation()
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                long l = Times.getCurrentTime();
                for (int m = 0; m < 200000; m++)
                {
                    writeRegion(m, transaction.getPage(0, m).getWriteRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - l + " " + m);
                        l = Times.getCurrentTime();
                    }
                }
            }
        });
        System.out.println(Times.getCurrentTime() - l);
        
        System.out.println("Checking DB...");
        l = Times.getCurrentTime();
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                IRawDataFile dataFile = transaction.getFile(0);
                dataFile.setReadMode(ReadMode.SEQUENTIAL);
                long l = Times.getCurrentTime();
                for (int m = 0; m < 200000; m++)
                {
                    checkRegion(m, transaction.getPage(0, m).getReadRegion());
                    
                    if (m > 0 && (m % 10000) == 0)
                    {
                        System.out.println(Times.getCurrentTime() - l + " " + m);
                        l = Times.getCurrentTime();
                    }
                }
            }
        });
        
        System.out.println(Times.getCurrentTime() - l);
    }
    
    private void writeRegion(int base, IRawWriteRegion region)
    {
        for (int i = 0; i < region.getLength(); i++)
            region.writeByte(i, (byte)(base + i));
    }
    
    private void checkRegion(int base, IRawReadRegion region)
    {
        for (int i = 0; i < region.getLength(); i++)
            Assert.checkState(region.readByte(i) == (byte)(base + i));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Region count ''{0}''.")
        ILocalizedMessage regionCount(int count);
        @DefaultMessage("Read region int ''{0}''.")
        ILocalizedMessage readRegionInt(Object benchmark);
        @DefaultMessage("Write region int ''{0}''.")
        ILocalizedMessage writeRegionInt(Object benchmark);
        @DefaultMessage("Read region long ''{0}''.")
        ILocalizedMessage readRegionLong(Object benchmark);
        @DefaultMessage("Write region long ''{0}''.")
        ILocalizedMessage writeRegionLong(Object benchmark);
        @DefaultMessage("Read buffer int ''{0}''.")
        ILocalizedMessage readBufferInt(Object benchmark);
        @DefaultMessage("Write buffer int ''{0}''.")
        ILocalizedMessage writeBufferInt(Object benchmark);
        @DefaultMessage("Read buffer long ''{0}''.")
        ILocalizedMessage readBufferLong(Object benchmark);
        @DefaultMessage("Write buffer long ''{0}''.")
        ILocalizedMessage writeBufferLong(Object benchmark);
        @DefaultMessage("Read direct buffer int ''{0}''.")
        ILocalizedMessage readDirectBufferInt(Object benchmark);
        @DefaultMessage("Write direct buffer int ''{0}''.")
        ILocalizedMessage writeDirectBufferInt(Object benchmark);
        @DefaultMessage("Read direct buffer long ''{0}''.")
        ILocalizedMessage readDirectBufferLong(Object benchmark);
        @DefaultMessage("Write direct buffer long ''{0}''.")
        ILocalizedMessage writeDirectBufferLong(Object benchmark);
        @DefaultMessage("Read transaction page ''{0}''.")
        ILocalizedMessage getReadTransactionPage(Object benchmark);
        @DefaultMessage("Single read transaction page ''{0}''.")
        ILocalizedMessage getSingleReadTransactionPage(Object benchmark);
        @DefaultMessage("Write transaction page ''{0}''.")
        ILocalizedMessage getWriteTransactionPage(Object benchmark);
        @DefaultMessage("Read transaction ''{0}''.")
        ILocalizedMessage readTransaction(Object benchmark);
        @DefaultMessage("Read transaction list ''{0}''.")
        ILocalizedMessage readTransactionList(Object benchmark);
        @DefaultMessage("Write transaction ''{0}''.")
        ILocalizedMessage writeTransaction(Object benchmark);
        @DefaultMessage("Write transaction list ''{0}''.")
        ILocalizedMessage writeTransactionList(Object benchmark);
        @DefaultMessage("====================================================================")
        ILocalizedMessage separator();
    }
}