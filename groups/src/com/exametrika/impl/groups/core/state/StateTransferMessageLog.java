/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.state;

import java.io.File;
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
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;


/**
 * The {@link StateTransferMessageLog} is a state transfer message log.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateTransferMessageLog
{
    public static void load(File path, List<IMessage> messages, ISerializationRegistry serializationRegistry)
    {
        RandomAccessFile file = null;
        try
        {
            file = new RandomAccessFile(path, "r");
            CRC32 crc = new CRC32();
            
            Assert.isTrue(file.readShort() == 0x1717);
            
            int length = file.readInt();
            int messageCount = file.readInt();
            
            byte[] buffer = new byte[length];
            file.readFully(buffer);
            
            crc.update(buffer);
            int checkSum = file.readInt();
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
            throw new RawDatabaseException(e);
        }
        finally
        {
            IOs.close(file);
        }
    }
    
    public static void save(List<IMessage> messages, File path, ISerializationRegistry serializationRegistry)
    {
        RandomAccessFile file = null;
        try
        {
            file = new RandomAccessFile(path, "rw");
            CRC32 crc = new CRC32();
            ByteOutputStream stream = new ByteOutputStream();
            Serialization serialization = new Serialization(serializationRegistry, true, stream);
            
            for (IMessage message : messages)
                MessageSerializers.serializeFully(serialization, (Message)message);
            
            crc.update(stream.getBuffer(), 0, stream.getLength());
            
            file.writeShort(0x1717);
            file.writeInt(stream.getLength());
            file.writeInt(messages.size());
            file.writeInt((int)crc.getValue());
            file.write(stream.getBuffer(), 0, stream.getLength());
        }
        catch (IOException e)
        {
            throw new RawDatabaseException(e);
        }
        finally
        {
            IOs.close(file);
        }
    }
    
    private StateTransferMessageLog()
    {
    }
}