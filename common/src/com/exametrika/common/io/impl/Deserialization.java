/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.EndOfObjectException;
import com.exametrika.common.io.EndOfRegionException;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.ISerializationRegistry.ISerializationInfo;
import com.exametrika.common.io.IncompatibleStreamVersionException;
import com.exametrika.common.io.UnsupportedStreamFormatException;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.InvalidStateException;
import com.exametrika.common.utils.Serializers;



/**
 * The {@link Deserialization} is an implementation of {@link IDeserialization}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class Deserialization extends DataDeserialization implements IDeserialization
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final byte REFERENCE = 0x1;
    private static final byte OBJECT_UUID = 0x2;
    private static final byte OBJECT_TYPEID = 0x4;
    private static final byte OBJECT = 0x8;
    private static final byte PRESERVE_IDENTITY_FLAG = 0x10;
    private static final byte REGION = 0x20;
    private final ISerializationRegistry serializationRegistry;
    private final ByteInputStream inputStream;
    private List<ISerializationInfo> typeList = new ArrayList<ISerializationInfo>();
    private List<Object> identityList = new ArrayList<Object>();
    private Stack stack;
    private Region region;

    /**
     * Creates a new object.
     *
     * @param serializationRegistry serialization registry
     * @param inputStream data input stream
     */
    public Deserialization(ISerializationRegistry serializationRegistry, ByteInputStream inputStream)
    {
        super(inputStream);
        
        Assert.notNull(serializationRegistry);
        Assert.notNull(inputStream);
        
        this.serializationRegistry = serializationRegistry;
        this.inputStream = inputStream;
        stack = new Stack(0, false, 0, null);
    }
    
    /**
     * Creates a new object reading and checking specified magic header and versions from stream.
     *
     * @param serializationRegistry serialization registry
     * @param streamMagicHeader stream magic header
     * @param minCompatibleStreamVersion minimal compatible stream version
     * @param maxCompatibleStreamVersion maximal compatible stream version
     * @param inputStream data input stream
     */
    public Deserialization(ISerializationRegistry serializationRegistry, int streamMagicHeader, 
        int minCompatibleStreamVersion, int maxCompatibleStreamVersion, ByteInputStream inputStream)
    {
        super(inputStream);
        
        Assert.notNull(serializationRegistry);
        Assert.notNull(inputStream);
        
        this.serializationRegistry = serializationRegistry;
        this.inputStream = inputStream;
        stack = new Stack(0, false, 0, null);
        
        readStreamHeader(streamMagicHeader, minCompatibleStreamVersion, maxCompatibleStreamVersion);
    }
    
    @Override
    public ByteInputStream getStream()
    {
        return inputStream;
    }
    
    @Override
    public ISerializationRegistry getRegistry()
    {
        return serializationRegistry;
    }
    
    @Override
    public int getVersion()
    {
        return stack.version;
    }
    
    @Override
    public <T> T readObject()
    {
        byte recordType = readByte();
        if (recordType == 0)
            return null;
        
        ISerializationInfo info = null;
        if ((recordType & REFERENCE) == REFERENCE)
        {
            int refId = readInt();
            return (T)identityList.get(refId);
        }
        else if ((recordType & OBJECT_UUID) == OBJECT_UUID)
        {
            UUID id = Serializers.readUUID(this);
            
            info = serializationRegistry.getInfo(id);
            typeList.add(info);
        }
        else if ((recordType & OBJECT_TYPEID) == OBJECT_TYPEID)
        {
            int typeId = readInt();
            info = typeList.get(typeId);
        }
        else
            throw new UnsupportedStreamFormatException();
        
        pushStack();
        
        if ((recordType & PRESERVE_IDENTITY_FLAG) == PRESERVE_IDENTITY_FLAG)
        {
            stack.preserveIdentity = true;
            stack.currentRefId = identityList.size(); 
            identityList.add(null);
        }
        
        stack.version = readByte();
        int objectLength = readInt();
        
        int pos = inputStream.getPosition();
        Object object = info.getSerializer().deserialize(this, info.getId());
        
        int readObjectLength = inputStream.getPosition() - pos;
        if (readObjectLength < objectLength)
            inputStream.skip(objectLength - readObjectLength);
        else if (readObjectLength > objectLength)
            throw new EndOfObjectException(messages.endOfObject(object));
        
        if ((recordType & PRESERVE_IDENTITY_FLAG) == PRESERVE_IDENTITY_FLAG)
            identityList.set(stack.currentRefId, object);
        
        popStack();
        
        return (T)object;
    }

    @Override
    public <C, T extends C> T readTypedObject(Class<C> objectClass)
    {
        byte recordType = readByte();
        if (recordType == 0)
            return null;
        
        if ((recordType & REFERENCE) == REFERENCE)
        {
            int refId = readInt();
            return (T)identityList.get(refId);
        }
        else if ((recordType & OBJECT) != OBJECT)
            throw new UnsupportedStreamFormatException();
        
        pushStack();
        
        if ((recordType & PRESERVE_IDENTITY_FLAG) == PRESERVE_IDENTITY_FLAG)
        {
            stack.preserveIdentity = true;
            stack.currentRefId = identityList.size(); 
            identityList.add(null);
        }
        
        stack.version = readByte();
        int objectLength = readInt();
        
        int pos = inputStream.getPosition();
        
        ISerializationInfo info = serializationRegistry.getInfo(objectClass);
        Object object = info.getSerializer().deserialize(this, info.getId());
        
        int readObjectLength = inputStream.getPosition() - pos;
        if (readObjectLength < objectLength)
            inputStream.skip(objectLength - readObjectLength);
        else if (readObjectLength > objectLength)
            throw new EndOfObjectException(messages.endOfObject(object));
        
        if ((recordType & PRESERVE_IDENTITY_FLAG) == PRESERVE_IDENTITY_FLAG)
            identityList.set(stack.currentRefId, object);
        
        popStack();
        
        return (T)object;
    }

    @Override
    public void publishReference(Object reference)
    {
        if (stack.preserveIdentity)
            identityList.set(stack.currentRefId, reference);
    }
    
    @Override
    public ByteArray beginReadRegion()
    {
        if (region != null)
            throw new InvalidStateException();
        
        byte recordType = readByte();
        if (recordType != REGION)
            throw new UnsupportedStreamFormatException();
        
        // Store deserialization state
        region = new Region();
        region.length = readInt();
        region.startPos = inputStream.getPosition();
        region.typeList = typeList;
        region.identityList = identityList;
        region.extensions = extensions;
        region.stack = stack;
        region.buffer = new ByteArray(inputStream.getBuffer(), region.startPos, region.length);
        
        // Initialize deserialization state by default values
        typeList = new ArrayList<ISerializationInfo>();
        identityList = new ArrayList<Object>();
        extensions = new HashMap<UUID, Object>();
        stack = new Stack(0, false, 0, null);
        
        return region.buffer;
    }
    
    @Override
    public void endReadRegion()
    {
        if (region == null)
            throw new InvalidStateException();
        
        int readLength = inputStream.getPosition() - region.startPos;
        if (readLength < region.length)
            inputStream.skip(region.length - readLength);
        else if (readLength > region.length)
            throw new EndOfRegionException(messages.endOfRegion());
        
        // Restore deserialization state
        typeList = region.typeList;
        identityList = region.identityList;
        extensions = region.extensions;
        stack = region.stack;
        region = null;
    }
    
    @Override
    public ByteArray readRegion()
    {
        ByteArray buffer = beginReadRegion();
        endReadRegion();
        
        return buffer;
    }
    
    private void readStreamHeader(int streamMagicHeader, int minCompatibleStreamVersion, int maxCompatibleStreamVersion)
    {
        int header = readInt();
        if (header != streamMagicHeader)
            throw new UnsupportedStreamFormatException(messages.unsupportedStreamFormat(header));
        
        int version = readInt();
        if (version < minCompatibleStreamVersion || version > maxCompatibleStreamVersion)
            throw new IncompatibleStreamVersionException(
                messages.incompatibleStreamVersion(version, minCompatibleStreamVersion, maxCompatibleStreamVersion));
    }
    
    private void pushStack()
    {
        stack = new Stack(0, false, 0, stack);
    }
    
    private void popStack()
    {
        stack = stack.prev;
    }
    
    private static final class Stack
    {
        private int version;
        private boolean preserveIdentity;
        private int currentRefId;
        private final Stack prev;
        
        Stack(int version, boolean preserveIdentity,int currentRefId, Stack prev)
        {
            this.version = version;
            this.preserveIdentity = preserveIdentity;
            this.currentRefId = currentRefId;
            this.prev = prev;
        }
    }
    
    private static final class Region
    {
        private int startPos;
        private int length;
        private List<ISerializationInfo> typeList;
        private List<Object> identityList;
        private Map<UUID, Object> extensions;
        private Stack stack;
        private ByteArray buffer;
    }
    
    private interface IMessages
    {
        @DefaultMessage("End of object ''{0}'' has been reached.")
        ILocalizedMessage endOfObject(Object object);
        @DefaultMessage("End of region has been reached.")
        ILocalizedMessage endOfRegion();
        @DefaultMessage("Identity is not supported for object ''{0}''.")
        ILocalizedMessage identityNotSupported(Object object);
        @DefaultMessage("Unsupported stream format ''{0}''.")
        ILocalizedMessage unsupportedStreamFormat(int header);
        @DefaultMessage("Stream version ''{0}'' is incompatible with deserializer. Deserializer supports stream versions from range ''[{1}..{2}]''.")
        ILocalizedMessage incompatibleStreamVersion(int streamVersion, int minCompatibleStreamVersion, int maxCompatibleStreamVersion);
    }
}
