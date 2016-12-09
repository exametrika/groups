/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;


/**
 * The {@link SerializationPerfTests} are performance tests for serialization framework.
 * 
 * @author Medvedev-A
 */
public class SerializationPerfTests
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(SerializationPerfTests.class);
    private static final short MAGIC_HEADER = 0x1717;
    private static final byte STREAM_VERSION = 0x1;
    private static final byte MAX_VERSION = 0x1;
    private static final byte MIN_VERSION = 0x1;
    private SerializationRegistry registry;
    
    @Before
    public void setUp()
    {
        registry = new SerializationRegistry();
        registry.register(new Test1Serializer());
        registry.register(new TestASerializer());
        registry.register(new TestBSerializer());
        registry.register(new TestCSerializer());
    }
    
    @Test
    public void testSimple() throws Throwable
    {
        final Test1 test = new Test1();
        test.value = "111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.serializeSimple(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                serialize(outputStream, test);
            }
        }, 10000)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        serialize(outputStream, test);
        
        logger.log(LogLevel.INFO, messages.streamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.deserializeSimple(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                deserialize(inputStream);
            }
        }, 10000)));
    }

    @Test
    public void testSimpleSerializable() throws Throwable
    {
        final Test1 test = new Test1();
        test.value = "111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.serializableSimpleSerialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                jdkSerialize(outputStream, test);
            }
        }, 10000)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        jdkSerialize(outputStream, test);
        
        logger.log(LogLevel.INFO, messages.streamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.serializableSimpleDeserialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                jdkDeserialize(inputStream);
            }
        }, 10000)));
    }
    
    @Test
    public void testIdentity() throws Throwable
    {
        TestA testA = new TestA();
        testA.value = "testA";
        testA.value2 = "testA";
        
        TestB testB = new TestB();
        final TestC testC = new TestC();
        
        testB.testA1 = testA;
        testB.testA2 = testA;
        testB.testA3 = testA;
        testB.testB = testB;
        testB.testC = testC;
        testB.testByte = createBytes();
        
        testC.testB1 = testB;
        testC.testB2 = testB;
        testC.testB3 = testB;
        testC.tests = new TestB[]{testB, testB, testB};
        testC.fixedTests = new TestB[]{testB, testB, testB};
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.serializeIdentity(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                serialize(outputStream, testC);
            }
        }, 10000)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        serialize(outputStream, testC);
        
        logger.log(LogLevel.INFO, messages.serializeStreamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.deserializeIdentity(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                deserialize(inputStream);
            }
        }, 10000)));
    }

    @Test
    public void testIdentitySerializable() throws Throwable
    {
        TestA testA = new TestA();
        testA.value = "testA";
        testA.value2 = "testA";
        
        TestB testB = new TestB();
        final TestC testC = new TestC();
        
        testB.testA1 = testA;
        testB.testA2 = testA;
        testB.testA3 = testA;
        testB.testB = testB;
        testB.testC = testC;
        testB.testByte = createBytes();
        
        testC.testB1 = testB;
        testC.testB2 = testB;
        testC.testB3 = testB;
        testC.tests = new TestB[]{testB, testB, testB};
        testC.fixedTests = new TestB[]{testB, testB, testB};
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.serializableIdentitySerialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                jdkSerialize(outputStream, testC);
            }
        }, 10000)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        jdkSerialize(outputStream, testC);
        
        logger.log(LogLevel.INFO, messages.serializableStreamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.serializableIdentityDeserialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                jdkDeserialize(inputStream);
            }
        }, 10000)));
    }
    
    @Test
    public void testIdentityExternalizable() throws Throwable
    {
        TestA1 testA = new TestA1();
        testA.value = "testA";
        testA.value2 = "testA";
        
        TestB1 testB = new TestB1();
        final TestC1 testC = new TestC1();
        
        testB.testA1 = testA;
        testB.testA2 = testA;
        testB.testA3 = testA;
        testB.testB = testB;
        testB.testC = testC;
        testB.testByte = createBytes();
        
        testC.testB1 = testB;
        testC.testB2 = testB;
        testC.testB3 = testB;
        testC.tests = new TestB1[]{testB, testB, testB};
        testC.fixedTests = new TestB1[]{testB, testB, testB};
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.externalizableIdentitySerialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                jdkSerialize(outputStream, testC);
            }
        }, 10000)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        jdkSerialize(outputStream, testC);
        
        logger.log(LogLevel.INFO, messages.externalizableStreamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.externalizableIdentityDeserialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                jdkDeserialize(inputStream);
            }
        }, 10000)));
    }
    
    @Test
    public void testSerialize() throws Throwable
    {
        final TestC testC = new TestC();
        
        testC.tests = new TestB[1000];
        testC.fixedTests = new TestB[1000];
        for (int i = 0; i < 1000; i++)
        {
            testC.tests[i] = new TestB(new TestA("val1", "val2"), new TestA("val1", "val2"), new TestA("val1", "val2"), null, null,
                createBytes()); 
            testC.fixedTests[i] = new TestB(new TestA("val1", "val2"), new TestA("val1", "val2"), new TestA("val1", "val2"), null, null,
                createBytes());
        }
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.serialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                serialize(outputStream, testC);
            }
        }, 100)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        serialize(outputStream, testC);
        
        logger.log(LogLevel.INFO, messages.serializeStreamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.deserialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                deserialize(inputStream);
            }
        }, 100)));
    }

    @Test
    public void testSerializable() throws Throwable
    {
        final TestC testC = new TestC();
        
        testC.tests = new TestB[1000];
        testC.fixedTests = new TestB[1000];
        for (int i = 0; i < 1000; i++)
        {
            testC.tests[i] = new TestB(new TestA("val1", "val2"), new TestA("val1", "val2"), new TestA("val1", "val2"), null, null,
                createBytes()); 
            testC.fixedTests[i] = new TestB(new TestA("val1", "val2"), new TestA("val1", "val2"), new TestA("val1", "val2"), null, null,
                createBytes());
        }
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.serializableSerialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                jdkSerialize(outputStream, testC);
            }
        }, 100)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        jdkSerialize(outputStream, testC);
        
        logger.log(LogLevel.INFO, messages.serializableStreamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.serializableDeserialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                jdkDeserialize(inputStream);
            }
        }, 100)));
    }

    @Test
    public void testExternalizable() throws Throwable
    {
        final TestC1 testC = new TestC1();
        
        testC.tests = new TestB1[1000];
        testC.fixedTests = new TestB1[1000];
        for (int i = 0; i < 1000; i++)
        {
            testC.tests[i] = new TestB1(new TestA1("val1", "val2"), new TestA1("val1", "val2"), new TestA1("val1", "val2"), null, null,
                createBytes()); 
            testC.fixedTests[i] = new TestB1(new TestA1("val1", "val2"), new TestA1("val1", "val2"), new TestA1("val1", "val2"), null, null,
                createBytes());
        }
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.externalizableSerialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                jdkSerialize(outputStream, testC);
            }
        }, 100)));
        
        final ByteOutputStream outputStream = new ByteOutputStream();
        jdkSerialize(outputStream, testC);
        
        logger.log(LogLevel.INFO, messages.externalizableStreamSize(outputStream.getLength()));
        
        logger.log(LogLevel.INFO, messages.externalizableDeserialize(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                jdkDeserialize(inputStream);
            }
        }, 100)));
    }

    @Test
    public void testVarSerialization() throws Throwable
    {
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.serializeVarInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                IDataSerialization serialization = new DataSerialization(outputStream);
                for (int i = 0; i < 1000000; i++)
                    Serializers.writeVarInt(serialization, Integer.MAX_VALUE);
                
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                IDataDeserialization deserialization = new DataDeserialization(inputStream);
                for (int i = 0; i < 1000000; i++)
                    Serializers.readVarInt(deserialization);
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.serializeInt(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                IDataSerialization serialization = new DataSerialization(outputStream);
                for (int i = 0; i < 1000000; i++)
                    serialization.writeInt(Integer.MAX_VALUE);
                
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                IDataDeserialization deserialization = new DataDeserialization(inputStream);
                for (int i = 0; i < 1000000; i++)
                    deserialization.readInt();
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.serializeVarLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                IDataSerialization serialization = new DataSerialization(outputStream);
                for (int i = 0; i < 1000000; i++)
                    Serializers.writeVarLong(serialization, Long.MAX_VALUE);
                
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                IDataDeserialization deserialization = new DataDeserialization(inputStream);
                for (int i = 0; i < 1000000; i++)
                    Serializers.readVarLong(deserialization);
            }
        }, 1)));
        logger.log(LogLevel.INFO, messages.serializeLong(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ByteOutputStream outputStream = new ByteOutputStream();
                IDataSerialization serialization = new DataSerialization(outputStream);
                for (int i = 0; i < 1000000; i++)
                    serialization.writeLong(Long.MAX_VALUE);
                
                ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                IDataDeserialization deserialization = new DataDeserialization(inputStream);
                for (int i = 0; i < 1000000; i++)
                    deserialization.readLong();
            }
        }, 1)));
    }
    
    private void serialize(ByteOutputStream out, Object object)
    {
        ISerialization serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, out);
        serialization.writeObject(object);
    }
    
    private <T> T deserialize(ByteInputStream in)
    {
        IDeserialization deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, MAX_VERSION, in);
        return (T)deserialization.readObject();
    }
    
    private byte[] createBytes()
    {
        byte[] res = new byte[256];
        for (int i = 0; i < res.length; i++)
            res[i] = (byte)(i - 127);
        
        return res;
    }
    
    private void jdkSerialize(ByteOutputStream out, Object object)
    {
        try
        {
            ObjectOutputStream objectStream = new ObjectOutputStream(out);
            objectStream.writeObject(object);
        }
        catch (IOException e)
        {
            throw new SerializationException(e);
        }
    }
    
    private <T> T jdkDeserialize(ByteInputStream in)
    {
        try
        {
            ObjectInputStream objectStream = new ObjectInputStream(in);
            return (T)objectStream.readObject();
        }
        catch (Exception e)
        {
            throw new SerializationException(e);
        }
    }

    private static class Test1Serializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("9a964304-f9f1-4eca-8cd7-105117e25638");

        public Test1Serializer()
        {
            super(ID, Test1.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            Test1 test = (Test1)object;
            serialization.writeString(test.value);
        }

        @Override
        public Object deserialize(final IDeserialization deserialization, UUID id)
        {
            Test1 test = new Test1();
            test.value = deserialization.readString();
            
            return test;
        }
    }
 
    private static class Test1 implements Serializable
    {
        private static final long serialVersionUID = -9047985463450942730L;
        
        String value;
    }
    
    private static class TestASerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("69f28174-f5b8-403f-99eb-f87775d072f1");

        public TestASerializer()
        {
            super(ID, TestA.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestA test = (TestA)object;
            serialization.writeString(test.value);
            serialization.writeString(test.value2);
        }

        @Override
        public Object deserialize(final IDeserialization deserialization, UUID id)
        {
            TestA test = new TestA();
            test.value = deserialization.readString();
            test.value2 = deserialization.readString();
            
            return test;
        }
    }
    
    private static class TestBSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("01c9fdbf-ebcc-4643-ab41-b6e25793cbc2");

        public TestBSerializer()
        {
            super(ID, TestB.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestB test = (TestB)object;
            serialization.writeObject(test.testA1);
            serialization.writeObject(test.testA2);
            serialization.writeObject(test.testA3);
            serialization.writeObject(test.testB);
            serialization.writeObject(test.testC);
            serialization.writeByteArray(new ByteArray(test.testByte));
        }

        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            TestB test = new TestB();
            deserialization.publishReference(test);
            
            test.testA1 = deserialization.readObject();
            test.testA2 = deserialization.readObject();
            test.testA3 = deserialization.readObject();
            test.testB = deserialization.readObject();
            test.testC = deserialization.readObject();
            deserialization.readByteArray();
            
            return test;
        }
    }
    
    private static class TestCSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("d419bc48-6eef-4380-93c7-f5b15f137e6a");

        public TestCSerializer()
        {
            super(ID, TestC.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestC test = (TestC)object;
            serialization.writeObject(test.testB1);
            serialization.writeObject(test.testB2);
            serialization.writeObject(test.testB3);
            
            serialization.writeInt(test.tests.length);
            for (int i = 0; i < test.tests.length; i++)
                serialization.writeObject(test.tests[i]);
            
            serialization.writeInt(test.fixedTests.length);
            for (int i = 0; i < test.fixedTests.length; i++)
                serialization.writeObject(test.fixedTests[i]);
        }

        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            TestC test = new TestC();
            deserialization.publishReference(test);
            
            test.testB1 = deserialization.readObject();
            test.testB2 = deserialization.readObject();
            test.testB3 = deserialization.readObject();
            int length = deserialization.readInt();
            test.tests = new TestB[length];
            for (int i = 0; i < length; i++)
                test.tests[i] = deserialization.readObject();
            
            length = deserialization.readInt();
            test.fixedTests = new TestB[length];
            for (int i = 0; i < length; i++)
                test.fixedTests[i] = deserialization.readObject();
            
            return test;
        }
    }

    private static class TestA implements Serializable
    {
        private static final long serialVersionUID = -9047985463450942730L;
        
        public TestA()
        {
        }
        
        public TestA(String value, String value2)
        {
            this.value = value;
            this.value2 = value2;
        }
        
        String value;
        String value2;
    }
    
    private static class TestB implements Serializable
    {
        private static final long serialVersionUID = -571730412183304030L;

        public TestB()
        {
        }
        
        public TestB(TestA testA1, TestA testA2, TestA testA3, TestB testB, TestC testC, byte[] testByte)
        {
            this.testA1 = testA1;
            this.testA2 = testA2;
            this.testA3 = testA3;
            this.testB = testB;
            this.testC = testC;
            this.testByte = testByte;
        }
        TestA testA1;
        TestA testA2;
        TestA testA3;
        TestB testB;
        TestC testC;
        byte[] testByte;
    }
    
    private static class TestC implements Serializable
    {
        private static final long serialVersionUID = -2598844146965351888L;
        
        public TestC()
        {
        }
        
        TestB testB1;
        TestB testB2;
        TestB testB3;
        TestB[] tests;
        TestB[] fixedTests;
    }
    
    private static class TestA1 implements Externalizable
    {
        private static final long serialVersionUID = -9047985463450942730L;
        
        String value;
        String value2;
    
        public TestA1()
        {
        }

        public TestA1(String value, String value2)
        {
            this.value = value;
            this.value2 = value2;
        }
        
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            value = in.readUTF();
            value2 = in.readUTF();
        }
        
        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeUTF(value);
            out.writeUTF(value2);
        }
    }
    
    private static class TestB1 implements Externalizable
    {
        private static final long serialVersionUID = -571730412183304030L;
        
        TestA1 testA1;
        TestA1 testA2;
        TestA1 testA3;
        TestB1 testB;
        TestC1 testC;
        byte[] testByte;

        public TestB1()
        {
        }

        public TestB1(TestA1 testA1, TestA1 testA2, TestA1 testA3, TestB1 testB, TestC1 testC, byte[] testByte)
        {
            this.testA1 = testA1;
            this.testA2 = testA2;
            this.testA3 = testA3;
            this.testB = testB;
            this.testC = testC;
            this.testByte = testByte;
        }
        
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            testA1 = (TestA1)in.readObject();
            testA2 = (TestA1)in.readObject();
            testA3 = (TestA1)in.readObject();
            testB = (TestB1)in.readObject();
            testC = (TestC1)in.readObject();
            int length = in.readInt();
            testByte = new byte[length];
            in.readFully(testByte);
        }
        
        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeObject(testA1);
            out.writeObject(testA2);
            out.writeObject(testA3);
            out.writeObject(testB);
            out.writeObject(testC);
            out.writeInt(testByte.length);
            out.write(testByte);
        }
    }
    
    private static class TestC1 implements Externalizable
    {
        private static final long serialVersionUID = -2598844146965351888L;
        
        TestB1 testB1;
        TestB1 testB2;
        TestB1 testB3;
        TestB1[] tests;
        TestB1[] fixedTests;
        
        public TestC1()
        {
        }
        
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            testB1 = (TestB1)in.readObject();
            testB2 = (TestB1)in.readObject();
            testB3 = (TestB1)in.readObject();
            
            int length = in.readInt();
            tests = new TestB1[length];
            for (int i = 0; i < length; i++)
                tests[i] = (TestB1)in.readObject();
            
            length = in.readInt();
            fixedTests = new TestB1[length];
            for (int i = 0; i < length; i++)
                fixedTests[i] = (TestB1)in.readObject();
        }
        
        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeObject(testB1);
            out.writeObject(testB2);
            out.writeObject(testB3);
            
            out.writeInt(tests.length);
            for (int i = 0; i < tests.length; i++)
                out.writeObject(tests[i]);
            
            out.writeInt(fixedTests.length);
            for (int i = 0; i < fixedTests.length; i++)
                out.writeObject(fixedTests[i]);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("[Grid serialize simple test] {0}.")
        ILocalizedMessage serializeSimple(Object results);
        @DefaultMessage("[Grid deserialize simple test] {0}.")
        ILocalizedMessage deserializeSimple(Object results);
        @DefaultMessage("[Java serializable simple serialize test] {0}.")
        ILocalizedMessage serializableSimpleSerialize(Object results);
        @DefaultMessage("[Java serializable simple deserialize test] {0}.")
        ILocalizedMessage serializableSimpleDeserialize(Object results);
        @DefaultMessage("Stream size ''{0}''.")
        ILocalizedMessage streamSize(Object size);
        @DefaultMessage("[Grid serialize identity test] {0}.")
        ILocalizedMessage serializeIdentity(Object results);
        @DefaultMessage("[Grid deserialize identity test] {0}.")
        ILocalizedMessage deserializeIdentity(Object results);
        @DefaultMessage("[Grid serialize test] {0}.")
        ILocalizedMessage serialize(Object results);
        @DefaultMessage("[Grid deserialize test] {0}.")
        ILocalizedMessage deserialize(Object results);
        @DefaultMessage("Grid serialized stream size ''{0}''.")
        ILocalizedMessage serializeStreamSize(Object size);
        @DefaultMessage("[Java serializable identity serialize test] {0}.")
        ILocalizedMessage serializableIdentitySerialize(Object results);
        @DefaultMessage("[Java serializable identity deserialize test] {0}.")
        ILocalizedMessage serializableIdentityDeserialize(Object results);
        @DefaultMessage("[Java serializable serialize test] {0}.")
        ILocalizedMessage serializableSerialize(Object results);
        @DefaultMessage("[Java serializable deserialize test] {0}.")
        ILocalizedMessage serializableDeserialize(Object results);
        @DefaultMessage("Java serializable stream size ''{0}''.")
        ILocalizedMessage serializableStreamSize(Object size);
        @DefaultMessage("[Java externalizable identity serialize test] {0}.")
        ILocalizedMessage externalizableIdentitySerialize(Object results);
        @DefaultMessage("[Java externalizable identity deserialize test] {0}.")
        ILocalizedMessage externalizableIdentityDeserialize(Object results);
        @DefaultMessage("[Java externalizable serialize test] {0}.")
        ILocalizedMessage externalizableSerialize(Object results);
        @DefaultMessage("[Java externalizable deserialize test] {0}.")
        ILocalizedMessage externalizableDeserialize(Object results);
        @DefaultMessage("Java externalizable stream size ''{0}''.")
        ILocalizedMessage externalizableStreamSize(Object size);
        @DefaultMessage("[Integer serialization test] {0}.")
        ILocalizedMessage serializeInt(Benchmark benchmark);
        @DefaultMessage("[Var integer serialization test] {0}.")
        ILocalizedMessage serializeVarInt(Benchmark benchmark);
        @DefaultMessage("[Long serialization test] {0}.")
        ILocalizedMessage serializeLong(Benchmark benchmark);
        @DefaultMessage("[Var long serialization test] {0}.")
        ILocalizedMessage serializeVarLong(Benchmark benchmark);
        @DefaultMessage("====================================================================")
        ILocalizedMessage separator();
    }
}