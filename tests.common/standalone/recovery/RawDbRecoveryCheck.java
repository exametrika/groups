package recovery;
import java.io.File;

import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.resource.config.MemoryResourceProviderConfiguration;
import com.exametrika.common.resource.config.PercentageResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;


public class RawDbRecoveryCheck
{
    public static void main(String[] args) throws Throwable
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        
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
        testRecoveryCheck(database);
        database.stop();
    }
    
    private static void testRecoveryCheck(IRawDatabase database) throws Throwable
    {
        database.transactionSync(new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                int base = transaction.getPage(0, 0).getReadRegion().readInt(0);
                System.out.println("Transaction " + base);
                for (int f = 0; f < 10; f++)
                {
                    for (int p = 0; p < 10; p++)
                        base = checkRegion(base, transaction.getPage(f, p).getReadRegion());
                }
            }
        });
    }
    
    private static int checkRegion(int base, IRawReadRegion region)
    {
        for (int i = 0; i < region.getLength(); i += 4)
        {
            Assert.isTrue(region.readInt(i) == base);
            base++;
        }
        
        return base;
    }
}
