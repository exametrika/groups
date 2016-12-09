package recovery;
import java.io.File;

import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.resource.config.MemoryResourceProviderConfiguration;
import com.exametrika.common.resource.config.PercentageResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Files;


public class RawDbRecoveryTest
{
    public static void main(String[] args) throws Throwable
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        
        RawDatabaseConfigurationBuilder builder = new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath())
            .setName("db")    
            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder()
                .setName("db")
                .setInitializePeriod(10000)
                .setResourceProvider(new PercentageResourceProviderConfiguration(new MemoryResourceProviderConfiguration(true), 70))
                .toConfiguration())    
            .addPageType("normal", 0x4000)
                .getDefaultPageCategory()
                    .setMaxPageIdlePeriod(60000)
                .end()
            .end();
        IRawDatabase database = new RawDatabaseFactory().createDatabase(builder.toConfiguration());
        database.start();
        testRecovery(database);
        database.stop();
    }
    
    private static void testRecovery(IRawDatabase database) throws Throwable
    {
        int i = 0;
        while (true)
        {
            System.out.println("Transaction " + i);
            
            final int t = i;
            database.transactionSync(new RawOperation()
            {
                @Override
                public void run(IRawTransaction transaction)
                {
                    int base = t;
                    for (int f = 0; f < 10; f++)
                    {
                        for (int p = 0; p < 10; p++)
                            base = writeRegion(base, transaction.getPage(f, p).getWriteRegion());
                    }
                }
            });
            i++;
        }
    }
    
    private static int writeRegion(int base, IRawWriteRegion region)
    {
        for (int i = 0; i < region.getLength(); i += 4)
        {
            region.writeInt(i, base);
            base++;
        }
        
        return base;
    }
}
