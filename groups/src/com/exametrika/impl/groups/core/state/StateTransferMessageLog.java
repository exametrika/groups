/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.state;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.zip.CRC32;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;


/**
 * The {@link StateTransferMessageLog} is a state transfer message log.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateTransferMessageLog implements Closeable
{
    private static final int MAGIC = 0x1717;
    private final RandomAccessFile file;
    
    public StateTransferMessageLog(File path, boolean read)
    {
        RandomAccessFile file = null;
        try
        {
            file = new RandomAccessFile(path, read ? "r" : "rw");
        }
        catch (FileNotFoundException e)
        {
            Exceptions.wrapAndThrow(e);
        }
        
        this.file = file;
    }
    
    @Override
    public void close()
    {
        IOs.close(file);
    }
    
    public void rewind()
    {
        try
        {
            file.seek(0);
        }
        catch (IOException e)
        {
            Exceptions.wrapAndThrow(e);
        }
    }
    
    public void load(List<IMessage> messages, ISerializationRegistry serializationRegistry)
    {
        try
        {
            if (file.getFilePointer() >= file.length())
                return;
            
            CRC32 crc = new CRC32();
            
            Assert.isTrue(file.readShort() == MAGIC);
            
            int length = file.readInt();
            int messageCount = file.readInt();
            long checkSum = file.readLong();
            
            byte[] buffer = new byte[length];
            file.readFully(buffer);
            
            crc.update(buffer);
            Assert.isTrue(crc.getValue() == checkSum);
            
            ByteInputStream stream = new ByteInputStream(buffer);
            Deserialization deserialization = new Deserialization(serializationRegistry, stream);
            
            for (int i = 0; i < messageCount; i++)
            {
                IMessage message = MessageSerializers.deserializeFully(deserialization);
                messages.add(message);
            }
        }
        catch (IOException e)
        {
            Exceptions.wrapAndThrow(e);
        }
    }
    
    public void save(List<IMessage> messages, ISerializationRegistry serializationRegistry)
    {
        try
        {
            CRC32 crc = new CRC32();
            ByteOutputStream stream = new ByteOutputStream();
            Serialization serialization = new Serialization(serializationRegistry, true, stream);
            
            for (IMessage message : messages)
                MessageSerializers.serializeFully(serialization, (Message)message);
            
            crc.update(stream.getBuffer(), 0, stream.getLength());
            
            file.writeShort(MAGIC);
            file.writeInt(stream.getLength());
            file.writeInt(messages.size());
            file.writeLong(crc.getValue());
            file.write(stream.getBuffer(), 0, stream.getLength());
        }
        catch (IOException e)
        {
            Exceptions.wrapAndThrow(e);
        }
    }
}