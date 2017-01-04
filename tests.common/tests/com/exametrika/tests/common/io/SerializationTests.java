/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.io;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializer;
import com.exametrika.common.io.IncompatibleStreamVersionException;
import com.exametrika.common.io.UnsupportedStreamFormatException;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.io.jdk.JdkSerializationRegistryExtension;
import com.exametrika.common.io.jdk.JdkSerializer;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Serializers;


/**
 * The {@link SerializationTests} are tests for {@link Serialization} and {@link Deserialization}.
 * 
 * @see Serialization
 * @see Deserialization
 * @author Medvedev-A
 */
public class SerializationTests
{
    private static final short MAGIC_HEADER = 0x1717;
    private static final byte STREAM_VERSION = 0x1;
    private static final byte MIN_VERSION = 0x1;
    private static final byte MAX_VERSION = 0x2;
    private static final short MAGIC_HEADER2 = 0x1718;
    private static final byte STREAM_VERSION2 = 0x2;
    private static final byte MIN_VERSION2 = 0x1;
    private static final byte MAX_VERSION2 = 0x2;
    private static final byte MIN_VERSION3 = 0x3;
    private static final byte MAX_VERSION3 = 0x3;
    private SerializationRegistry registry;
    
    @Before
    public void setUp()
    {
        registry = new SerializationRegistry();
        registry.register(new Test1Serializer());
        registry.register(new TestA2Serializer());
        registry.register(new TestBSerializer());
        registry.register(new TestCSerializer());
    }
    
    @Test
    public void testCompatibility() throws Throwable
    {
        TestA test1 = new TestA();
        test1.value = "test";
        test1.value2 = "test2";
        ByteOutputStream outputStream = new ByteOutputStream();
        Serialization serialization1 = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        serialization1.writeObject(test1);
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Deserialization deserialization2 = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION2, 
            MAX_VERSION2, inputStream);
        TestA test11 = deserialization2.readObject();
        assertThat(test11.value, is("test"));
        assertThat(test11.value2, is("test2"));
        
        outputStream = new ByteOutputStream();
        Serialization serialization2 = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION2, outputStream);
        serialization2.writeObject(test1);
        
        inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Deserialization deserialization1 = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, 
            MAX_VERSION, inputStream);
        TestA test12 = deserialization1.readObject();
        assertThat(test12.value, is("test"));
        assertThat(test12.value2, is("test2"));
        
        outputStream = new ByteOutputStream();
        serialization2 = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION2, outputStream);
        serialization2.writeObject(test1);
        
        final ByteInputStream inputStream2 = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        
        new Expected(UnsupportedStreamFormatException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Deserialization deserialization3 = new Deserialization(registry, MAGIC_HEADER2, MIN_VERSION, 
                    MAX_VERSION, inputStream2);
                deserialization3.readObject();
            }
        });
        
        outputStream = new ByteOutputStream();
        serialization2 = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION2, outputStream);
        serialization2.writeObject(test1);
        
        final ByteInputStream inputStream3 = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        
        new Expected(IncompatibleStreamVersionException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Deserialization deserialization4 = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION3, 
                    MAX_VERSION3, inputStream3);
                deserialization4.readObject();
            }
        });
    }
    
    @Test
    public void testSerialize() throws Throwable
    {
        ByteOutputStream outputStream = new ByteOutputStream();
        serialize(outputStream, new Test1());
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Test1 test2 = deserialize(inputStream);
        assertThat(test2 != null, is(true));
    }
    
    @Test
    public void testIdentity() throws Throwable
    {
        ByteOutputStream outputStream = new ByteOutputStream();
        
        TestA testA = new TestA();
        testA.value = "testA";
        
        TestB testB = new TestB();
        TestC testC = new TestC();
        TestC testC2 = new TestC();
        
        testB.testA1 = testA;
        testB.testA2 = testA;
        testB.testA3 = testA;
        testB.testB = testB;
        testB.testC1 = testC;
        testB.testC2 = testC2;
        
        testC.testB1 = testB;
        testC.testB2 = testB;
        testC.testB3 = testB;
        
        TestB testB2 = new TestB();
        testC2.testB1 = testB2;
        testC2.testB2 = testB2;
        testC2.testB3 = testB2;
        
        ISerialization serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        serialization.writeObject(testC);
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        
        IDeserialization deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, MAX_VERSION, inputStream);
        testC = deserialization.readObject();
        testB = testC.testB1;
        testA = testB.testA1;
        testC2 = testB.testC2;
        testB2 = testC2.testB1;
        
        assertThat(testA.value, is("testA"));
        
        assertThat(testB.testA1 == testA, is(true));
        assertThat(testB.testA2 == testA, is(true));
        assertThat(testB.testA3 == testA, is(true));
        assertThat(testB.testB == testB, is(true));
        assertThat(testB.testC1 == testC, is(true));
        
        assertThat(testC.testB1 == testB, is(true));
        assertThat(testC.testB2 == testB, is(true));
        assertThat(testC.testB3 == testB, is(true));
        
        assertThat(testC2.testB2 != testB2, is(true));
        assertThat(testC2.testB3 != testB2, is(true));
        assertThat(testC2.testB2, is(testB2));
        assertThat(testC2.testB3, is(testB2));
        
        outputStream = new ByteOutputStream();
        serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        serialization.writeTypedObject(testC);
        
        inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        
        deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, MAX_VERSION, inputStream);
        testC = deserialization.readTypedObject(TestC.class);
        testB = testC.testB1;
        testA = testB.testA1;
        testC2 = testB.testC2;
        testB2 = testC2.testB1;
        
        assertThat(testA.value, is("testA"));
        
        assertThat(testB.testA1 == testA, is(true));
        assertThat(testB.testA2 == testA, is(true));
        assertThat(testB.testA3 == testA, is(true));
        assertThat(testB.testB == testB, is(true));
        assertThat(testB.testC1 == testC, is(true));
        
        assertThat(testC.testB1 == testB, is(true));
        assertThat(testC.testB2 == testB, is(true));
        assertThat(testC.testB3 == testB, is(true));
        
        assertThat(testC2.testB2 != testB2, is(true));
        assertThat(testC2.testB3 != testB2, is(true));
        assertThat(testC2.testB2, is(testB2));
        assertThat(testC2.testB3, is(testB2));
    }
    
    @Test
    public void testVersions()
    {
        SerializationRegistry registry1 = new SerializationRegistry();
        registry1.register(new TestA1Serializer());
        
        SerializationRegistry registry2 = new SerializationRegistry();
        registry2.register(new TestA2Serializer());
        
        ByteOutputStream outputStream = new ByteOutputStream();
        Serialization serialization1 = new Serialization(registry1, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        serialization1.writeObject(new TestA("Test", null));
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Deserialization deserialization2 = new Deserialization(registry2, MAGIC_HEADER, MIN_VERSION2, 
            MAX_VERSION2, inputStream);
        TestA test = deserialization2.readObject();
        assertThat(test, is(new TestA("Test", "Default")));
        
        outputStream = new ByteOutputStream();
        Serialization serialization2 = new Serialization(registry2, true, MAGIC_HEADER, STREAM_VERSION2, outputStream);
        serialization2.writeObject(new TestA("Test", "Test2"));
        
        inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Deserialization deserialization1 = new Deserialization(registry1, MAGIC_HEADER, MIN_VERSION, 
            MAX_VERSION, inputStream);
        test = deserialization1.readObject();
        assertThat(test, is(new TestA("Test", null)));
    }
    
    @Test
    public void testMixedSerialization()
    {
        final JdkSerializationRegistryExtension extension = new JdkSerializationRegistryExtension();
        extension.register(UUID.randomUUID(), null, new JdkSerializer(null));
        
        SerializationRegistry registry = new SerializationRegistry(extension);
        registry.register(new MixedSerializer());
        registry.register(new TestA1Serializer());
        
        Mixed test = new Mixed();
        test.field1 = "test";
        test.field2 = 123;
        test.field3 = Arrays.asList("test1", "test2");
        test.field4 = new Date();
        test.field5 = new TestA("a", null);
        
        Mixed test2 = new Mixed();
        test2.field1 = "test";
        test2.field2 = 123;
        test2.field3 = test.field3;
        test2.field4 = test.field4;
        test2.field5 = test.field5;
        
        ByteOutputStream outputStream = new ByteOutputStream();
        Serialization serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        
        serialization.writeObject(test);
        serialization.writeObject(test2);
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Deserialization deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, 
            MAX_VERSION, inputStream);
        
        Mixed testRead = deserialization.readObject();
        Mixed test2Read = deserialization.readObject();
        
        assertThat(testRead, is(test));
        assertThat(test2Read, is(test2));
        assertThat(testRead.field3 == test2Read.field3, is(true));
        assertThat(testRead.field4 == test2Read.field4, is(true));
        assertThat(testRead.field5 == test2Read.field5, is(true));
    }
    
    @Test
    public void testRegions()
    {
        final JdkSerializationRegistryExtension extension = new JdkSerializationRegistryExtension();
        extension.register(UUID.randomUUID(), null, new JdkSerializer(null));

        SerializationRegistry registry = new SerializationRegistry(extension);
        registry.register(new MixedSerializer());
        registry.register(new TestA1Serializer());
        
        Mixed test = new Mixed();
        test.field1 = "test";
        test.field2 = 123;
        test.field3 = Arrays.asList("test1", "test2");
        test.field4 = new Date();
        test.field5 = new TestA("a", null);
        
        Mixed test2 = new Mixed();
        test2.field1 = "test";
        test2.field2 = 123;
        test2.field3 = test.field3;
        test2.field4 = test.field4;
        test2.field5 = test.field5;
        
        ByteOutputStream outputStream = new ByteOutputStream();
        Serialization serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        
        serialization.writeObject(test);
        
        serialization.beginWriteRegion();
        serialization.writeObject(test);
        serialization.writeObject(test2);
        serialization.endWriteRegion();
        
        serialization.writeObject(test2);
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Deserialization deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, 
            MAX_VERSION, inputStream);
        
        Mixed testRead = deserialization.readObject();
        ByteArray region = deserialization.readRegion();
        Mixed test2Read = deserialization.readObject();
        
        assertThat(testRead, is(test));
        assertThat(test2Read, is(test2));
        assertThat(testRead.field3 == test2Read.field3, is(true));
        assertThat(testRead.field4 == test2Read.field4, is(true));
        assertThat(testRead.field5 == test2Read.field5, is(true));
        
        inputStream = new ByteInputStream(region.getBuffer(), region.getOffset(), region.getLength());
        deserialization = new Deserialization(registry, inputStream);
        
        testRead = deserialization.readObject();
        test2Read = deserialization.readObject();
        
        assertThat(testRead, is(test));
        assertThat(test2Read, is(test2));
        assertThat(testRead.field3 == test2Read.field3, is(true));
        assertThat(testRead.field4 == test2Read.field4, is(true));
        assertThat(testRead.field5 == test2Read.field5, is(true));
    }
    
    @Test
    public void testMultiSerializer() throws Throwable
    {
        ISerializer serializer = new MultiSerializer();
        SerializationRegistry registry = new SerializationRegistry();
        registry.register(MultiSerializer.ID_A, TestA.class, serializer);
        registry.register(MultiSerializer.ID_B, TestB.class, serializer);
        registry.register(MultiSerializer.ID_C, TestC.class, serializer);
        
        ByteOutputStream outputStream = new ByteOutputStream();
        
        TestA testA = new TestA();
        testA.value = "testA";
        
        TestB testB = new TestB();
        TestC testC = new TestC();
        TestC testC2 = new TestC();
        
        testB.testA1 = testA;
        testB.testA2 = testA;
        testB.testA3 = testA;
        testB.testB = testB;
        testB.testC1 = testC;
        testB.testC2 = testC2;
        
        testC.testB1 = testB;
        testC.testB2 = testB;
        testC.testB3 = testB;
        
        TestB testB2 = new TestB();
        testC2.testB1 = testB2;
        testC2.testB2 = testB2;
        testC2.testB3 = testB2;
        
        ISerialization serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        serialization.writeObject(testC);
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        
        IDeserialization deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, MAX_VERSION, inputStream);
        testC = deserialization.readObject();
        testB = testC.testB1;
        testA = testB.testA1;
        testC2 = testB.testC2;
        testB2 = testC2.testB1;
        
        assertThat(testA.value, is("testA"));
        
        assertThat(testB.testA1 == testA, is(true));
        assertThat(testB.testA2 == testA, is(true));
        assertThat(testB.testA3 == testA, is(true));
        assertThat(testB.testB == testB, is(true));
        assertThat(testB.testC1 == testC, is(true));
        
        assertThat(testC.testB1 == testB, is(true));
        assertThat(testC.testB2 == testB, is(true));
        assertThat(testC.testB3 == testB, is(true));
        
        assertThat(testC2.testB2 != testB2, is(true));
        assertThat(testC2.testB3 != testB2, is(true));
        assertThat(testC2.testB2, is(testB2));
        assertThat(testC2.testB3, is(testB2));
        
        outputStream = new ByteOutputStream();
        serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        serialization.writeTypedObject(testC);
        
        inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        
        deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, MAX_VERSION, inputStream);
        testC = deserialization.readTypedObject(TestC.class);
        testB = testC.testB1;
        testA = testB.testA1;
        testC2 = testB.testC2;
        testB2 = testC2.testB1;
        
        assertThat(testA.value, is("testA"));
        
        assertThat(testB.testA1 == testA, is(true));
        assertThat(testB.testA2 == testA, is(true));
        assertThat(testB.testA3 == testA, is(true));
        assertThat(testB.testB == testB, is(true));
        assertThat(testB.testC1 == testC, is(true));
        
        assertThat(testC.testB1 == testB, is(true));
        assertThat(testC.testB2 == testB, is(true));
        assertThat(testC.testB3 == testB, is(true));
        
        assertThat(testC2.testB2 != testB2, is(true));
        assertThat(testC2.testB3 != testB2, is(true));
        assertThat(testC2.testB2, is(testB2));
        assertThat(testC2.testB3, is(testB2));
    }

    @Test
    public void testEnums() throws Throwable
    {
        ByteOutputStream outputStream = new ByteOutputStream();
        Serialization serialization = new Serialization(registry, true, MAGIC_HEADER, STREAM_VERSION, outputStream);
        
        Serializers.writeEnum(serialization, SmallEnum.B);
        Serializers.writeEnum(serialization, LargeEnum.A22);
        
        Serializers.writeEnumSet(serialization, Enums.of(SmallEnum.A, SmallEnum.D));
        Serializers.writeEnumSet(serialization, Immutables.wrap(Enums.of(SmallEnum.A, SmallEnum.D)));
        Serializers.writeEnumSet(serialization, Enums.range(LargeEnum.A1, LargeEnum.A70));
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        Deserialization deserialization = new Deserialization(registry, MAGIC_HEADER, MIN_VERSION, 
            MAX_VERSION, inputStream);
        
        Assert.assertEquals(SmallEnum.B, Serializers.readEnum(deserialization, SmallEnum.class));
        Assert.assertEquals(LargeEnum.A22, Serializers.readEnum(deserialization, LargeEnum.class));
        
        Assert.assertEquals(Enums.of(SmallEnum.A, SmallEnum.D), Serializers.readEnumSet(deserialization, SmallEnum.class));
        Assert.assertEquals(Enums.of(SmallEnum.A, SmallEnum.D), Serializers.readEnumSet(deserialization, SmallEnum.class));
        Assert.assertEquals(Enums.range(LargeEnum.A1, LargeEnum.A70), Serializers.readEnumSet(deserialization, LargeEnum.class));
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

    private static class Test1
    {
    }
    
    private static class Test1Serializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("c7250181-5114-4b63-b95c-f6343ada76a9");

        public Test1Serializer()
        {
            super(ID, Test1.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            serialization.writeByte((byte)-123);
            serialization.writeByte(Byte.MIN_VALUE);
            serialization.writeByte(Byte.MAX_VALUE);
            serialization.writeChar('c');
            serialization.writeChar(Character.MIN_VALUE);
            serialization.writeChar(Character.MAX_VALUE);
            serialization.writeShort((short)1234);
            serialization.writeShort(Short.MIN_VALUE);
            serialization.writeShort(Short.MAX_VALUE);
            serialization.writeInt(1234567890);
            serialization.writeInt(Integer.MIN_VALUE);
            serialization.writeInt(Integer.MAX_VALUE);
            serialization.writeInt(1234567890);
            serialization.writeInt(0);
            serialization.writeInt(12345);
            serialization.writeInt(Integer.MIN_VALUE);
            serialization.writeInt(Integer.MAX_VALUE);
            serialization.writeInt(Integer.MIN_VALUE);
            serialization.writeInt(Integer.MAX_VALUE);
            Serializers.writeVarInt(serialization, 1234567890);
            Serializers.writeVarInt(serialization, 0);
            Serializers.writeVarInt(serialization, 12345);
            Serializers.writeVarInt(serialization, Integer.MIN_VALUE);
            Serializers.writeVarInt(serialization, Integer.MAX_VALUE);
            Serializers.writeSignedVarInt(serialization, Integer.MIN_VALUE);
            Serializers.writeSignedVarInt(serialization, Integer.MAX_VALUE);
            serialization.writeLong(12345678909876L);
            serialization.writeLong(Long.MIN_VALUE);
            serialization.writeLong(Long.MAX_VALUE);
            serialization.writeLong(12345678909876L);
            serialization.writeLong(0);
            serialization.writeLong(12345);
            serialization.writeLong(Long.MIN_VALUE);
            serialization.writeLong(Long.MAX_VALUE);
            serialization.writeLong(Long.MIN_VALUE);
            serialization.writeLong(Long.MAX_VALUE);
            Serializers.writeVarLong(serialization, 12345678909876L);
            Serializers.writeVarLong(serialization, 0);
            Serializers.writeVarLong(serialization, 12345);
            Serializers.writeVarLong(serialization, Long.MIN_VALUE);
            Serializers.writeVarLong(serialization, Long.MAX_VALUE);
            Serializers.writeSignedVarLong(serialization, Long.MIN_VALUE);
            Serializers.writeSignedVarLong(serialization, Long.MAX_VALUE);
            serialization.writeBoolean(true);
            serialization.writeDouble(0.12345d);
            serialization.writeByteArray(new ByteArray(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0}));
            serialization.writeByteArray(new ByteArray(new byte[]{}));
            serialization.writeByteArray(new ByteArray(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0}, 2, 3));
            serialization.writeByteArray(null);
            serialization.writeString("Hello world!!!");
            serialization.writeString("");
            serialization.writeString(null);
            Serializers.writeUUID(serialization, TestA1Serializer.ID);
            serialization.writeObject(new TestC(new TestB(new TestA("qawerty", "1234"), new TestA(), new TestA(), 
                new TestB(), new TestC(), new TestC()), new TestB(), new TestB()));
            serialization.writeObject(null);
            serialization.writeTypedObject(new TestC(new TestB(new TestA("qawerty", "1234"), new TestA(), new TestA(), 
                new TestB(), new TestC(), new TestC()), new TestB(), new TestB()));
            serialization.writeTypedObject(null);
        }

        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            assertThat(deserialization.readByte(), is((byte)-123));
            assertThat(deserialization.readByte(), is(Byte.MIN_VALUE));
            assertThat(deserialization.readByte(), is(Byte.MAX_VALUE));
            assertThat(deserialization.readChar(), is('c'));
            assertThat(deserialization.readChar(), is(Character.MIN_VALUE));
            assertThat(deserialization.readChar(), is(Character.MAX_VALUE));
            assertThat(deserialization.readShort(), is((short)1234));
            assertThat(deserialization.readShort(), is(Short.MIN_VALUE));
            assertThat(deserialization.readShort(), is(Short.MAX_VALUE));
            assertThat(deserialization.readInt(), is(1234567890));
            assertThat(deserialization.readInt(), is(Integer.MIN_VALUE));
            assertThat(deserialization.readInt(), is(Integer.MAX_VALUE));
            assertThat(deserialization.readInt(), is(1234567890));
            assertThat(deserialization.readInt(), is(0));
            assertThat(deserialization.readInt(), is(12345));
            assertThat(deserialization.readInt(), is(Integer.MIN_VALUE));
            assertThat(deserialization.readInt(), is(Integer.MAX_VALUE));
            assertThat(deserialization.readInt(), is(Integer.MIN_VALUE));
            assertThat(deserialization.readInt(), is(Integer.MAX_VALUE));
            assertThat(Serializers.readVarInt(deserialization), is(1234567890));
            assertThat(Serializers.readVarInt(deserialization), is(0));
            assertThat(Serializers.readVarInt(deserialization), is(12345));
            assertThat(Serializers.readVarInt(deserialization), is(Integer.MIN_VALUE));
            assertThat(Serializers.readVarInt(deserialization), is(Integer.MAX_VALUE));
            assertThat(Serializers.readSignedVarInt(deserialization), is(Integer.MIN_VALUE));
            assertThat(Serializers.readSignedVarInt(deserialization), is(Integer.MAX_VALUE));
            assertThat(deserialization.readLong(), is(12345678909876L));
            assertThat(deserialization.readLong(), is(Long.MIN_VALUE));
            assertThat(deserialization.readLong(), is(Long.MAX_VALUE));
            assertThat(deserialization.readLong(), is(12345678909876L));
            assertThat(deserialization.readLong(), is(0L));
            assertThat(deserialization.readLong(), is(12345L));
            assertThat(deserialization.readLong(), is(Long.MIN_VALUE));
            assertThat(deserialization.readLong(), is(Long.MAX_VALUE));
            assertThat(deserialization.readLong(), is(Long.MIN_VALUE));
            assertThat(deserialization.readLong(), is(Long.MAX_VALUE));
            assertThat(Serializers.readVarLong(deserialization), is(12345678909876L));
            assertThat(Serializers.readVarLong(deserialization), is(0L));
            assertThat(Serializers.readVarLong(deserialization), is(12345L));
            assertThat(Serializers.readVarLong(deserialization), is(Long.MIN_VALUE));
            assertThat(Serializers.readVarLong(deserialization), is(Long.MAX_VALUE));
            assertThat(Serializers.readSignedVarLong(deserialization), is(Long.MIN_VALUE));
            assertThat(Serializers.readSignedVarLong(deserialization), is(Long.MAX_VALUE));
            assertThat(deserialization.readBoolean(), is(true));
            assertThat(deserialization.readDouble(), is(0.12345d));
            assertThat(deserialization.readByteArray(), is(new ByteArray(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0})));
            assertThat(deserialization.readByteArray(), is(new ByteArray(new byte[]{})));
            assertThat(deserialization.readByteArray(), is(new ByteArray(new byte[]{3, 4, 5})));
            assertThat(deserialization.readByteArray(), nullValue());
            assertThat(deserialization.readString(), is("Hello world!!!"));
            assertThat(deserialization.readString(), is(""));
            assertThat(deserialization.readString(), nullValue());
            assertThat(Serializers.readUUID(deserialization), is(TestA1Serializer.ID));
            assertThat((TestC)deserialization.readObject(), is(new TestC(new TestB(new TestA("qawerty", "1234"), 
                new TestA(), new TestA(), new TestB(), new TestC(), new TestC()), new TestB(), new TestB())));
            assertThat(deserialization.readObject(), nullValue());
            assertThat(deserialization.readTypedObject(TestC.class), is(new TestC(new TestB(new TestA("qawerty", "1234"), 
                new TestA(), new TestA(), new TestB(), new TestC(), new TestC()), new TestB(), new TestB())));
            assertThat(deserialization.readTypedObject(TestC.class), nullValue());
            return new Test1();
        }
    }

    private static class TestA
    {
        String value;
        String value2;
        
        public TestA()
        {
        }
        
        public TestA(String value, String value2)
        {
            this.value = value;
            this.value2 = value2;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof TestA))
                return false;
            
            TestA test = (TestA)o;
            
            return Objects.equals(value, test.value) && Objects.equals(value2, test.value2);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(value, value2);
        }
    }
    
    private static class TestB
    {
        TestA testA1;
        TestA testA2;
        TestA testA3;
        TestB testB;
        TestC testC1;
        TestC testC2;
        
        public TestB()
        {
        }
        
        public TestB(TestA testA1, TestA testA2, TestA testA3, TestB testB, TestC testC1, TestC testC2)
        {
            this.testA1 = testA1;
            this.testA2 = testA2;
            this.testA3 = testA3;
            this.testB = testB;
            this.testC1 = testC1;
            this.testC2 = testC2;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof TestB))
                return false;
            
            TestB test = (TestB)o;
            
            return Objects.equals(testA1, test.testA1) && Objects.equals(testA2, test.testA2) && 
                Objects.equals(testA3, test.testA3) && Objects.equals(testB, test.testB) && 
                Objects.equals(testC1, test.testC1) && Objects.equals(testC2, test.testC2);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(testA1, testA2, testA3, testB, testC1, testC2);
        }
    }
    
    private static class TestC
    {
        TestB testB1;
        TestB testB2;
        TestB testB3;
        
        public TestC()
        {
        }
        
        public TestC(TestB testB1, TestB testB2, TestB testB3)
        {
            this.testB1 = testB1;
            this.testB2 = testB2;
            this.testB3 = testB3;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof TestC))
                return false;
            
            TestC test = (TestC)o;
            
            return Objects.equals(testB1, test.testB1) && Objects.equals(testB2, test.testB2) && 
                Objects.equals(testB3, test.testB3);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(testB1, testB2, testB3);
        }
    }
    
    private static class TestA1Serializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("33e4d29c-5ac6-4c6b-bad2-14fd82069cf8");

        public TestA1Serializer()
        {
            super(ID, TestA.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestA test = (TestA)object;
            serialization.writeString(test.value);
        }

        @Override
        public Object deserialize(final IDeserialization deserialization, UUID id)
        {
            TestA test = new TestA();
            test.value = deserialization.readString();

            return test;
        }
    }
    
    private static class TestA2Serializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("33e4d29c-5ac6-4c6b-bad2-14fd82069cf8");

        public TestA2Serializer()
        {
            super(ID, TestA.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            serialization.setVersion(1);
            TestA test = (TestA)object;
            serialization.writeString(test.value);
            serialization.writeString(test.value2);
        }

        @Override
        public Object deserialize(final IDeserialization deserialization, UUID id)
        {
            int version = deserialization.getVersion();
            TestA test = new TestA();
            test.value = deserialization.readString();
            
            if (version > 0)
                test.value2 = deserialization.readString();
            else
                test.value2 = "Default";

            return test;
        }
    }
    
    private static class TestBSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("be6fd766-b301-4334-bae2-a0e6ce791532");

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
            serialization.writeTypedObject(test.testC1);
            
            serialization.setPreserveIdentity(false);
            serialization.writeObject(test.testC2);
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
            test.testC1 = deserialization.readTypedObject(TestC.class);
            test.testC2 = deserialization.readObject();
            
            return test;
        }
    }
    
    private static class TestCSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("7f33911f-71c1-45b4-beac-14ad7b3863a2");

        public TestCSerializer()
        {
            super(ID, TestC.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestC test = (TestC)object;
            serialization.writeObject(test.testB1);
            serialization.writeTypedObject(test.testB2);
            serialization.writeObject(test.testB3);
        }

        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            TestC test = new TestC();
            
            deserialization.publishReference(test);
            
            test.testB1 = deserialization.readObject();
            test.testB2 = deserialization.readTypedObject(TestB.class);
            test.testB3 = deserialization.readObject();
            
            return test;
        }
    }
    
    private static class Mixed
    {
        String field1;
        int field2;
        List<String> field3;
        Date field4;
        TestA field5;
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof Mixed))
                return false;
            
            Mixed test = (Mixed)o;
            
            return Objects.equals(field1, test.field1) && field2 == test.field2 && Objects.equals(field3, test.field3) && 
                Objects.equals(field4, test.field4) && Objects.equals(field5, test.field5);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(field1, field2, field3, field4, field5);
        }
    }
    
    private static class MixedSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("94bd4537-9081-4738-8737-b25af7d9a814");

        public MixedSerializer()
        {
            super(ID, Mixed.class);
        }
        
        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            Mixed test = (Mixed)object;
            serialization.writeString(test.field1);
            serialization.writeInt(test.field2);
            serialization.writeObject(test.field3);
            serialization.writeTypedObject(test.field4);
            serialization.writeObject(test.field5);
        }

        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            Mixed test = new Mixed();
            
            deserialization.publishReference(test);
            
            test.field1 = deserialization.readString();
            test.field2 = deserialization.readInt();
            test.field3 = deserialization.readObject();
            test.field4 = deserialization.readTypedObject(Serializable.class);
            test.field5 = deserialization.readObject();
            
            return test;
        }
    }
    
    private static class MultiSerializer implements ISerializer
    {
        public static final UUID ID_A = UUID.fromString("56d6ea07-56a2-4b52-a359-0408e94271c1");
        public static final UUID ID_B = UUID.fromString("a83f3faa-7307-4e6f-ad80-427851f98c78");
        public static final UUID ID_C = UUID.fromString("8aa19e79-e04d-4aba-b91f-4c198b664761");

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            if (object instanceof TestA)
            {
                serialization.setVersion(1);
                TestA test = (TestA)object;
                serialization.writeString(test.value);
                serialization.writeString(test.value2);
            }
            else if (object instanceof TestB)
            {
                TestB test = (TestB)object;
                serialization.writeObject(test.testA1);
                serialization.writeObject(test.testA2);
                serialization.writeObject(test.testA3);
                serialization.writeObject(test.testB);
                serialization.writeTypedObject(test.testC1);
                
                serialization.setPreserveIdentity(false);
                serialization.writeObject(test.testC2);
            }
            else if (object instanceof TestC)
            {
                TestC test = (TestC)object;
                serialization.writeObject(test.testB1);
                serialization.writeTypedObject(test.testB2);
                serialization.writeObject(test.testB3); 
            }
            else
                throw new AssertionError();
            
        }

        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            if (id.equals(ID_A))
            {
                int version = deserialization.getVersion();
                TestA test = new TestA();
                test.value = deserialization.readString();
                
                if (version > 0)
                    test.value2 = deserialization.readString();
                else
                    test.value2 = "Default";

                return test;  
            }
            else if (id.equals(ID_B))
            {
                TestB test = new TestB();
                
                deserialization.publishReference(test);
                
                test.testA1 = deserialization.readObject();
                test.testA2 = deserialization.readObject();
                test.testA3 = deserialization.readObject();
                test.testB = deserialization.readObject();
                test.testC1 = deserialization.readTypedObject(TestC.class);
                test.testC2 = deserialization.readObject();
                
                return test;
            }
            else if (id.equals(ID_C))
            {
                TestC test = new TestC();
                
                deserialization.publishReference(test);
                
                test.testB1 = deserialization.readObject();
                test.testB2 = deserialization.readTypedObject(TestB.class);
                test.testB3 = deserialization.readObject();
                
                return test;
            }
            else
                throw new AssertionError();
        }
    }
    
    private enum SmallEnum
    {
        A, B, C, D
    }
    
    private enum LargeEnum
    {
        A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25,
        A26, A27, A28, A29, A30, A31, A32, A33, A34, A35, A36, A37, A38, A39, A40, A41, A42, A43, A44, A45, A46, A47, A48,
        A49, A50, A51, A52, A53, A54, A55, A56, A57, A58, A59, A60, A61, A62, A63, A64, A65, A66, A67, A68, A69, A70
    }
}