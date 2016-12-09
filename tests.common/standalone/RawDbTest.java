import java.io.File;

import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.resource.config.MemoryResourceProviderConfiguration;
import com.exametrika.common.resource.config.PercentageResourceProviderConfiguration;
import com.exametrika.common.resource.config.SharedResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.Times;


public class RawDbTest
{
    public static void main(String[] args) throws Throwable
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db" + args[0]);
        Files.emptyDir(tempDir);
        
        File exchangeFile = new File(System.getProperty("java.io.tmpdir"), "exchange.dat");
        
        RawDatabaseConfigurationBuilder builder = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath())
            .setName("db" + args[0])    
            .setResourceAllocator(new SharedResourceAllocatorConfigurationBuilder()
                .setName("db" + args[0])
                .setInitializePeriod(10000).setDataExchangeFileName(exchangeFile.getPath())
                .setResourceProvider(new PercentageResourceProviderConfiguration(new MemoryResourceProviderConfiguration(true), 70))
                .toConfiguration())    
            .addPageType("normal", 0x4000)
                .getDefaultPageCategory()
                    .setMaxPageIdlePeriod(60000)
                .end()
            .end();
        IRawDatabase database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        testSmallScalability(database);
        database.stop();
    }
    
    private static void testSmallScalability(IRawDatabase database) throws Throwable
    {
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
    
    private static void writeRegion(int base, IRawWriteRegion region)
    {
        for (int i = 0; i < region.getLength(); i++)
            region.writeByte(i, (byte)(base + i));
    }
    
    private static void checkRegion(int base, IRawReadRegion region)
    {
        for (int i = 0; i < region.getLength(); i++)
            Assert.checkState(region.readByte(i) == (byte)(base + i));
    }
}
