/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.rawdb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.UUID;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.rawdb.impl.RawTransaction;
import com.exametrika.common.rawdb.impl.RawTransactionManager;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Serializers;


/**
 * The {@link RawDatabaseSerializationTests} are tests for {@link RawPageSerialization} and {@link RawPageDeserialization}.
 * 
 * @see RawPageSerialization
 * @see RawPageDeserialization
 * @author Medvedev-A
 */
public class RawDatabaseSerializationTests
{
    private RawDatabase database;
    private UUID id = UUID.randomUUID();
    
    @Before
    public void setUp()
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);
        database = new RawDatabaseFactory().createDatabase(new RawDatabaseConfigurationBuilder().addPath(tempDir.getPath()).toConfiguration());
    }
    
    @After
    public void tearDown()
    {
        IOs.close(database);
    }

    @Test
    public void testSimpleSerialization() throws Throwable
    {
        RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(new TestOperation(), transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        
        RawPageSerialization serialization = new RawPageSerialization(transaction, 0, 0, 0);
        
        serialize(serialization);
        
        serialization.setPosition(1000, 1);
        serialize(serialization);
        
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, 0, 0, 0);
        deserialize(deserialization);
        
        deserialization.setPosition(1000, 1);
        deserialize(deserialization);
    }

    @Test
    public void testByteArraySerialization() throws Throwable
    {
        RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(new TestOperation(), transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        RawPageSerialization serialization = new RawPageSerialization(transaction, 0, 0, 5);
        
        ByteArray b1 = createBuffer(1, 4);
        ByteArray b2 = createBuffer(2, 1);
        ByteArray b3 = createBuffer(3, 10);
        ByteArray b4 = createBuffer(4, 25);
        ByteArray b5 = createBuffer(5, 31);
        serialization.writeByteArray(b1);
        serialization.writeByteArray(b2);
        serialization.writeByteArray(b3);
        serialization.writeByteArray(b4);
        serialization.writeByteArray(b5);
        serialization.writeByteArray(null);
        
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, 0, 0, 5);
        
        assertThat(deserialization.readByteArray(), is(b1));
        assertThat(deserialization.readByteArray(), is(b2));
        assertThat(deserialization.readByteArray(), is(b3));
        assertThat(deserialization.readByteArray(), is(b4));
        assertThat(deserialization.readByteArray(), is(b5));
        assertThat(deserialization.readByteArray(), nullValue());
    }
    
    @Test
    public void testStringSerialization() throws Throwable
    {
        RawTransactionManager transactionManager = database.getTransactionManager();
        RawTransaction transaction = new RawTransaction(new TestOperation(), transactionManager, new Object());
        transactionManager.setTransaction(transaction);
        RawPageSerialization serialization = new RawPageSerialization(transaction, 0, 0, 1);
        
        String b1 = createString(1, 2);
        String b2 = createString(2, 1);
        String b3 = createString(3, 10);
        String b4 = createString(4, 25);
        String b5 = createString(5, 31);
        serialization.writeString(b1);
        serialization.writeString(b2);
        serialization.writeString(b3);
        serialization.writeString(b4);
        serialization.writeString(b5);
        serialization.writeString(null);
        
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, 0, 0, 1);
        
        assertThat(deserialization.readString(), is(b1));
        assertThat(deserialization.readString(), is(b2));
        assertThat(deserialization.readString(), is(b3));
        assertThat(deserialization.readString(), is(b4));
        assertThat(deserialization.readString(), is(b5));
        assertThat(deserialization.readString(), nullValue());
    }
    
    private void deserialize(RawPageDeserialization deserialization)
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
        assertThat(deserialization.readBoolean(), is(true));
        assertThat(deserialization.readDouble(), is(0.12345d));
        assertThat(deserialization.readByteArray(), is(new ByteArray(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0})));
        assertThat(deserialization.readByteArray(), is(new ByteArray(new byte[]{})));
        assertThat(deserialization.readByteArray(), is(new ByteArray(new byte[]{3, 4, 5})));
        assertThat(deserialization.readByteArray(), nullValue());
        assertThat(deserialization.readString(), is("Hello world!!!"));
        assertThat(deserialization.readString(), is(""));
        assertThat(deserialization.readString(), nullValue());
        assertThat(Serializers.readUUID(deserialization), is(id));
        
        Assert.assertEquals(SmallEnum.B, Serializers.readEnum(deserialization, SmallEnum.class));
        Assert.assertEquals(LargeEnum.A22, Serializers.readEnum(deserialization, LargeEnum.class));
        
        Assert.assertEquals(Enums.of(SmallEnum.A, SmallEnum.D), Serializers.readEnumSet(deserialization, SmallEnum.class));
        Assert.assertEquals(Enums.of(SmallEnum.A, SmallEnum.D), Serializers.readEnumSet(deserialization, SmallEnum.class));
        Assert.assertEquals(Enums.range(LargeEnum.A1, LargeEnum.A70), Serializers.readEnumSet(deserialization, LargeEnum.class));
    }

    private void serialize(RawPageSerialization serialization)
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
        serialization.writeBoolean(true);
        serialization.writeDouble(0.12345d);
        serialization.writeByteArray(new ByteArray(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0}));
        serialization.writeByteArray(new ByteArray(new byte[]{}));
        serialization.writeByteArray(new ByteArray(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0}, 2, 3));
        serialization.writeByteArray(null);
        serialization.writeString("Hello world!!!");
        serialization.writeString("");
        serialization.writeString(null);
        
        Serializers.writeUUID(serialization, id);
        
        Serializers.writeEnum(serialization, SmallEnum.B);
        Serializers.writeEnum(serialization, LargeEnum.A22);
        
        Serializers.writeEnumSet(serialization, Enums.of(SmallEnum.A, SmallEnum.D));
        Serializers.writeEnumSet(serialization, Immutables.wrap(Enums.of(SmallEnum.A, SmallEnum.D)));
        Serializers.writeEnumSet(serialization, Enums.range(LargeEnum.A1, LargeEnum.A70));
    }

    private ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
    
    private String createString(int base, int length)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++)
            builder.append(Integer.toString((base + i) % 10));
        
        return builder.toString();
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
    
    public static class TestOperation extends RawOperation
    {
        @Override
        public void run(IRawTransaction transaction)
        {
        }
    }
}