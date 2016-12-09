/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.ISerializationRegistry.ISerializationInfo;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.InvalidStateException;
import com.exametrika.common.utils.Serializers;


/**
 * The {@link Serialization} is an implementation of {@link ISerialization}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class Serialization extends DataSerialization implements ISerialization
{
    private static final byte REFERENCE = 0x1;
    private static final byte OBJECT_UUID = 0x2;
    private static final byte OBJECT_TYPEID = 0x4;
    private static final byte OBJECT = 0x8;
    private static final byte PRESERVE_IDENTITY_FLAG = 0x10;
    private static final byte REGION = 0x20;
    private final ISerializationRegistry serializationRegistry;
    private final ByteOutputStream outputStream;
    private final boolean defaultPreserveIdentity;
    private Map<Class, TypeInfo> typeMap = new HashMap<Class, TypeInfo>();
    private TObjectIntMap<Object> identityMap = new TObjectIntCustomHashMap<Object>(new IdentityHashingStrategy<Object>(), 
        10, 0.5f, Integer.MAX_VALUE);
    private int typeCounter;
    private int referenceCounter;
    private Stack stack;
    private Region region;

    /**
     * Creates a new object.
     *
     * @param serializationRegistry serialization registry
     * @param preserveIdentity if true preserves object identity in serialized data, if false makes a copy of each object occurence
     * @param outputStream output stream to serialize into
     */
    public Serialization(ISerializationRegistry serializationRegistry, boolean preserveIdentity, ByteOutputStream outputStream)
    {
        super(outputStream);
        
        Assert.notNull(serializationRegistry);
        Assert.notNull(outputStream);

        this.serializationRegistry = serializationRegistry;
        this.outputStream = outputStream;
        this.stack = new Stack(0, preserveIdentity, null);
        this.defaultPreserveIdentity = preserveIdentity;
    }
    
    /**
     * Creates a new object and writes specified magic header and version to the stream.
     *
     * @param serializationRegistry serialization registry
     * @param preserveIdentity if true preserves object identity in serialized data, if false makes a copy of each object occurence
     * @param streamMagicHeader stream magic header
     * @param streamVersion stream version
     * @param outputStream output stream to serialize into
     */
    public Serialization(ISerializationRegistry serializationRegistry, boolean preserveIdentity, 
        int streamMagicHeader, int streamVersion, ByteOutputStream outputStream)
    {
        super(outputStream);
        
        Assert.notNull(serializationRegistry);
        Assert.notNull(outputStream);

        this.serializationRegistry = serializationRegistry;
        this.outputStream = outputStream;
        this.stack = new Stack(0, preserveIdentity, null);
        this.defaultPreserveIdentity = preserveIdentity;
        
        writeStreamHeader(streamMagicHeader, streamVersion);
    }

    @Override
    public ByteOutputStream getStream()
    {
        return outputStream;
    }
 
    @Override
    public ISerializationRegistry getRegistry()
    {
        return serializationRegistry;
    }
    
    @Override
    public void setVersion(int version)
    {
        if (version < 0 || version > 255)
            throw new InvalidArgumentException();
        
        stack.version = version;   
    }
    
    @Override
    public void setPreserveIdentity(boolean value)
    {
        stack.preserveIdentity = value;
    }
    
    @Override
    public void writeObject(Object value)
    {
        if (value != null)
        {
            if (stack.preserveIdentity)
            {
                int refId = identityMap.get(value);
                if (refId != identityMap.getNoEntryValue())
                {
                    // If reference is found, write reference
                    writeByte(REFERENCE);
                    writeInt(refId);
                    return;
                }

                // If reference is not found, store refId of current object
                identityMap.put(value, referenceCounter++);
            }
            
            Class clazz = value.getClass();
            TypeInfo typeInfo = typeMap.get(clazz);
            if (typeInfo == null)
            {
                // If serializer UUID is not written yet, write object header with serializer UUID
                ISerializationInfo info = serializationRegistry.getInfo(clazz);
                typeInfo = new TypeInfo(info, typeCounter++);
                typeMap.put(clazz, typeInfo);
                
                byte recordType = OBJECT_UUID;
                if (stack.preserveIdentity)
                    recordType |= PRESERVE_IDENTITY_FLAG;
                
                writeByte(recordType);
                Serializers.writeUUID(this, info.getId());
            }
            else
            {
                // If serializer UUID already written, write object header with serial typeId only
                byte recordType = OBJECT_TYPEID;
                if (stack.preserveIdentity)
                    recordType |= PRESERVE_IDENTITY_FLAG;
                
                writeByte(recordType);
                writeInt(typeInfo.typeId);
            }

            // Reserve space for object version (byte) and length (int)
            outputStream.grow(5);
            int pos = outputStream.getLength();
            int markPos = outputStream.getBufferLength();
            Object mark = outputStream.getMark();
            
            pushStack();

            // Serialize object
            typeInfo.info.getSerializer().serialize(this, value);
            
            // Write object version and length
            int length = outputStream.getLength() - pos;
            byte[] buf = outputStream.getBuffer(mark);
            buf[markPos - 5] = (byte)stack.version;
            writeInt(buf, markPos - 4, length);
            
            popStack();
        }
        else
            writeByte((byte)0);
    }
    
    @Override
    public <T> void writeTypedObject(T value)
    {
        writeTypedObject(value, value != null ? (Class<T>)value.getClass() : null);
    }
    
    @Override
    public <T> void writeTypedObject(T value, Class<? super T> objectClass)
    {
        if (value != null)
        {
            Assert.notNull(objectClass);
            
            if (stack.preserveIdentity)
            {
                int refId = identityMap.get(value);
                if (refId != identityMap.getNoEntryValue())
                {
                    // If reference is found, write reference
                    writeByte(REFERENCE);
                    writeInt(refId);
                    return;
                }

                // If reference is not found, store refId of current object
                identityMap.put(value, referenceCounter++);
            }
            
            // Write typed object
            byte recordType = OBJECT;
            
            if (stack.preserveIdentity)
                recordType |= PRESERVE_IDENTITY_FLAG;
            
            writeByte(recordType);
            
            // Reserve space for object version (byte) and length (int)
            outputStream.grow(5);
            int pos = outputStream.getLength();
            int markPos = outputStream.getBufferLength();
            Object mark = outputStream.getMark();
            
            pushStack();

            // Serialize object
            ISerializationInfo info = serializationRegistry.getInfo(objectClass);
            info.getSerializer().serialize(this, value);
            
            // Write object version and length
            int length = outputStream.getLength() - pos;
            byte[] buf = outputStream.getBuffer(mark);
            buf[markPos - 5] = (byte)stack.version;
            writeInt(buf, markPos - 4, length);
            
            popStack();
        }
        else
            writeByte((byte)0);
    }

    @Override
    public void beginWriteRegion()
    {
        if (region != null)
            throw new InvalidStateException();
        
        writeByte(REGION);
        // Reserve space for region length(int)
        outputStream.grow(4);
        
        // Store serialization state
        region = new Region();
        region.startPos = outputStream.getLength();
        region.markPos = outputStream.getBufferLength();
        region.mark = outputStream.getMark();
        region.typeMap = typeMap;
        region.identityMap = identityMap;
        region.extensions = extensions;
        region.typeCounter = typeCounter;
        region.referenceCounter = referenceCounter;
        region.stack = stack;
        
        // Initialize serialization state by default values
        typeMap = new HashMap<Class, TypeInfo>();
        identityMap = new TObjectIntCustomHashMap<Object>(new IdentityHashingStrategy<Object>(), 
            10, 0.5f, Integer.MAX_VALUE);
        extensions = new HashMap<UUID, Object>();
        typeCounter = 0;
        referenceCounter = 0;
        stack = new Stack(0, defaultPreserveIdentity, null);
    }
    
    @Override
    public void endWriteRegion()
    {
        if (region == null)
            throw new InvalidStateException();
        
        // Write region length
        int length = outputStream.getLength() - region.startPos;
        byte[] buf = outputStream.getBuffer(region.mark);
        writeInt(buf, region.markPos - 4, length);
        
        // Restore serialization state
        typeMap = region.typeMap;
        identityMap = region.identityMap;
        extensions = region.extensions;
        typeCounter = region.typeCounter;
        referenceCounter = region.referenceCounter;
        stack = region.stack;
        region = null;
    }
    
    @Override
    public void writeRegion(ByteArray buffer)
    {
        Assert.notNull(buffer);
        
        outputStream.write(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
    }
    
    private void writeInt(byte[] buffer, int offset, int value)
    {
        unsafe.putInt(buffer, byteArrayOffset + offset, value);
    }
    
    private void writeStreamHeader(int streamMagicHeader, int streamVersion)
    {
        writeInt(streamMagicHeader);
        writeInt(streamVersion);
    }

    private void pushStack()
    {
        stack = new Stack(0, stack.preserveIdentity, stack);
    }
    
    private void popStack()
    {
        stack = stack.prev;
    }
    
    private static final class TypeInfo
    {
        private final ISerializationInfo info;
        private final int typeId;
        
        public TypeInfo(ISerializationInfo info, int typeId)
        {
            this.info = info;
            this.typeId = typeId;
        }
    }
    
    private static final class Stack
    {
        private int version;
        private boolean preserveIdentity;
        private final Stack prev;
        
        Stack(int version, boolean preserveIdentity, Stack prev)
        {
            this.version = version;
            this.preserveIdentity = preserveIdentity;
            this.prev = prev;
        }
    }
    
    private static final class Region
    {
        private int startPos;
        private Object mark;
        private int markPos;
        private Map<Class, TypeInfo> typeMap;
        private TObjectIntMap<Object> identityMap;
        private Map<UUID, Object> extensions;
        private int typeCounter;
        private int referenceCounter;
        private Stack stack;
    }
}
