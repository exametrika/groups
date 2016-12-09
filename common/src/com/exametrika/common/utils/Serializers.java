/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.io.jdk.JdkDeserializationInputStream;
import com.exametrika.common.io.jdk.SimpleJdkSerializationRegistryExtension;
import com.exametrika.common.services.Services;


/**
 * The {@link Serializers} is utility class to serialize with JDK serialization.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Serializers
{
    public static int readVarInt(IDataDeserialization deserialization)
    {
        int res = 0;

        byte b = deserialization.readByte();
        res |= b & 0x7F;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (b & 0x7F) << 7;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (b & 0x7F) << 14;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (b & 0x7F) << 21;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (b & 0x7F) << 28;
   
        return res;
    }

    public static void writeVarInt(IDataSerialization serialization, int value)
    {
        if (writeVarByte(serialization, value >>> 0))
            return;
        if (writeVarByte(serialization, value >>> 7))
            return;
        if (writeVarByte(serialization, value >>> 14))
            return;
        if (writeVarByte(serialization, value >>> 21))
            return;
        
        writeVarByte(serialization, value >>> 28);
    }
    
    public static int readSignedVarInt(IDataDeserialization deserialization)
    {
        int res = readVarInt(deserialization);
        return (res >>> 1) ^ -(res & 1);
    }
    
    public static void writeSignedVarInt(IDataSerialization serialization, int value)
    {
        value = (value << 1) ^ (value >> 31);
        writeVarInt(serialization, value);
    }
    
    public static long readVarLong(IDataDeserialization deserialization)
    {
        long res = 0;

        byte b = deserialization.readByte();
        res |= b & 0x7F;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (b & 0x7F) << 7;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (b & 0x7F) << 14;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (long)(b & 0x7F) << 21;
        if ((b & 0x80) == 0)
            return res;

        b = deserialization.readByte();
        res |= (long)(b & 0x7F) << 28;
        if ((b & 0x80) == 0)
            return res;
        
        b = deserialization.readByte();
        res |= (long)(b & 0x7F) << 35;
        if ((b & 0x80) == 0)
            return res;
        
        b = deserialization.readByte();
        res |= (long)(b & 0x7F) << 42;
        if ((b & 0x80) == 0)
            return res;
   
        b = deserialization.readByte();
        res |= (long)(b & 0x7F) << 49;
        if ((b & 0x80) == 0)
            return res;
        
        b = deserialization.readByte();
        res |= (long)(b & 0x7F) << 56;
        if ((b & 0x80) == 0)
            return res;
        
        b = deserialization.readByte();
        res |= (long)(b & 0x7F) << 63;
        
        return res;
    }

    public static void writeVarLong(IDataSerialization serialization, long value)
    {
        if (writeVarByte(serialization, value >>> 0))
            return;
        if (writeVarByte(serialization, value >>> 7))
            return;
        if (writeVarByte(serialization, value >>> 14))
            return;
        if (writeVarByte(serialization, value >>> 21))
            return;
        if (writeVarByte(serialization, value >>> 28))
            return;
        if (writeVarByte(serialization, value >>> 35))
            return;
        if (writeVarByte(serialization, value >>> 42))
            return;
        if (writeVarByte(serialization, value >>> 49))
            return;
        if (writeVarByte(serialization, value >>> 56))
            return;
        
        writeVarByte(serialization, value >>> 63);
    }
    
    public static long readSignedVarLong(IDataDeserialization deserialization)
    {
        long res = readVarLong(deserialization);
        return (res >>> 1) ^ -(res & 1);
    }

    public static void writeSignedVarLong(IDataSerialization serialization, long value)
    {
        value = (value << 1) ^ (value >> 63);
        writeVarLong(serialization, value);
    }
    
    public static UUID readUUID(IDataDeserialization deserialization)
    {
        long leastSignificantBits = deserialization.readLong();
        long mostSignificantBits = deserialization.readLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    public static void writeUUID(IDataSerialization serialization, UUID value)
    {
        Assert.notNull(value);

        serialization.writeLong(value.getLeastSignificantBits());
        serialization.writeLong(value.getMostSignificantBits());
    }
    
    public static <T extends Enum<T>> T readEnum(IDataDeserialization deserialization, Class<T> enumType)
    {
        Assert.notNull(enumType);
        
        int ordinal = deserialization.readInt();
        return enumType.getEnumConstants()[ordinal];
    }
    
    public static <T extends Enum<T>> void writeEnum(IDataSerialization serialization, T value)
    {
        serialization.writeInt(value.ordinal());   
    }
    
    public static <T extends Enum<T>> Set<T> readEnumSet(IDataDeserialization deserialization, Class<T> enumType)
    {
        return Enums.deserialize(deserialization, enumType, enumType.getEnumConstants());
    }
    
    public static <T extends Enum<T>> void writeEnumSet(IDataSerialization serialization, Set<T> value)
    {
        Enums.serialize(serialization, value);
    }
    
    /**
     * Serializes object.
     *
     * @param stream output stream
     * @param object serialized object
     */
    public static void serialize(OutputStream stream, Serializable object)
    {
        try
        {
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(object);
        }
        catch (IOException e)
        {
            throw new SerializationException(e);
        }
    }
    
    /**
     * Deserializes objects.
     *
     * @param stream input stream
     * @param classLoader classloader
     * @return deserialized object
     */
    public static <T extends Serializable> T deserialize(InputStream stream, ClassLoader classLoader)
    {
        try
        {
            ObjectInputStream objectStream = new JdkDeserializationInputStream(stream, classLoader);
            return (T)objectStream.readObject();
        }
        catch (Exception e)
        {
            throw new SerializationException(e);
        }
    }
    
    /**
     * Creates serialization registry and JDK serializer and service serializers using {@link ISerializationRegistrar} as service name.
     *
     * @return serialization registry
     */
    public static ISerializationRegistry createRegistry()
    {
        SimpleJdkSerializationRegistryExtension extension = new SimpleJdkSerializationRegistryExtension(Serializers.class.getClassLoader());
        ISerializationRegistry registry = new SerializationRegistry(extension);
        
        List<ISerializationRegistrar> registrars = Services.loadProviders(ISerializationRegistrar.class);
        for (ISerializationRegistrar registrar : registrars)
            registrar.register(registry);
        
        return registry;
    }

    private static boolean writeVarByte(IDataSerialization serialization, int value)
    {
        byte b = (byte)(value & 0x7F);
        if ((value & 0xFFFFFF80) == 0)
        {
            serialization.writeByte(b);
            return true;
        }
        else
        {
            b |= 0x80;
            serialization.writeByte(b);
            return false;
        }
    }
    
    private static boolean writeVarByte(IDataSerialization serialization, long value)
    {
        byte b = (byte)(value & 0x7F);
        if ((value & 0xFFFFFFFFFFFFFF80l) == 0)
        {
            serialization.writeByte(b);
            return true;
        }
        else
        {
            b |= 0x80;
            serialization.writeByte(b);
            return false;
        }
    }
    
    private Serializers()
    {
    }
}
