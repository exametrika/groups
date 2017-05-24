/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.message;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.IVisitor;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;


/**
 * The {@link Message} is a message.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Message implements IMessage
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IAddress source;
    private final IAddress destination;
    private final int flags;
    private final List<File> files;
    private final Entry partsHead;
    private final ISerializationRegistry serializationRegistry;

    public Message(IAddress source, IAddress destination, int flags, ISerializationRegistry serializationRegistry)
    {
        this(source, destination, flags, null, null, serializationRegistry);
    }
    
    public Message(IAddress source, IAddress destination, IMessagePart part, ISerializationRegistry serializationRegistry)
    {
        this(source, destination, 0, null, part != null ? new Entry(part, null) : null, serializationRegistry);
    }
    
    public Message(IAddress source, IAddress destination, IMessagePart part, int flags, List<File> files, ISerializationRegistry serializationRegistry)
    {
        this(source, destination, flags, files, part != null ? new Entry(part, null) : null, serializationRegistry);
    }
    
    public Message(IAddress  source, IAddress destination, List<IMessagePart> parts, int flags, List<File> files,
        ISerializationRegistry serializationRegistry)
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        Assert.notNull(serializationRegistry);
        
        Entry entry = null;
        
        if (parts != null && !parts.isEmpty())
        {
            for (int i = parts.size() - 1; i >= 1; i--)
                entry = new Entry(parts.get(i), entry);
            
            entry = new Entry(deserialize(parts.get(0)), entry);
        }

        this.source = source;
        this.destination = destination;
        this.flags = flags;
        this.files = Immutables.wrap(files);
        this.partsHead = entry;
        this.serializationRegistry = serializationRegistry;
    }

    public int getPartCount()
    {
        return partsHead != null ? partsHead.count : 0;
    }

    public void visitParts(IVisitor<IMessagePart> visitor)
    {
        Assert.notNull(visitor);
        
        Entry entry = partsHead;
        while (entry != null)
        {
            if (!visitor.visit(entry.part))
                break;
            entry = entry.next;
        }
    }

    public ISerializationRegistry getRegistry()
    {
        return serializationRegistry;
    }
    
    @Override
    public IAddress getSource()
    {
        return source;
    }

    @Override
    public IAddress getDestination()
    {
        return destination;
    }
    
    @Override
    public int getSize()
    {
        return (partsHead != null ? partsHead.size : 0);
    }

    @Override
    public int getFlags()
    {
        return flags;
    }
    
    @Override
    public boolean hasFlags(int flags)
    {
        return (this.flags & flags) == flags; 
    }
    
    @Override
    public boolean hasOneOfFlags(int flags)
    {
        return (this.flags & flags) != 0; 
    }
    
    @Override
    public List<File> getFiles()
    {
        return files;
    }
    
    @Override
    public <T extends IMessagePart> T getPart()
    {
        return partsHead != null ? (T)partsHead.part : null;
    }
    
    @Override
    public Message addFlags(int flags)
    {
        return new Message(source, destination, this.flags | flags, files, partsHead, serializationRegistry);
    }
    
    @Override
    public Message addPart(IMessagePart part)
    {
        return addPart(part, false);
    }
    
    @Override
    public Message addPart(IMessagePart part, boolean serialize)
    {
        Assert.notNull(part);
        
        Entry partsHead = this.partsHead;
        
        if (serialize)
            partsHead = serialize(partsHead);
        
        return new Message(source, destination, flags, files, new Entry(part, partsHead), serializationRegistry);
    }
    
    @Override
    public Message addPart(IMessagePart part, int flags)
    {
        return addPart(part, flags, false);
    }
    
    @Override
    public Message addPart(IMessagePart part, int flags, boolean serialize)
    {
        Assert.notNull(part);
        Assert.notNull(flags);
        
        Entry partsHead = this.partsHead;
        
        if (serialize)
            partsHead = serialize(partsHead);
        
        return new Message(source, destination, this.flags | flags, files, new Entry(part, partsHead), serializationRegistry);
    }
    
    @Override
    public Message removeFlags(int flags)
    {
        return new Message(source, destination, this.flags & ~flags, files, partsHead, serializationRegistry);
    }
    
    @Override
    public Message removePart()
    {
        if (partsHead == null)
            return null;
        
        Entry next = partsHead.next;
        if (next != null)
            next = new Entry(deserialize(next.part), next.next);

        return new Message(source, destination, flags, files, next, serializationRegistry);
    }
    
    @Override
    public Message removePart(int flags)
    {
        Assert.notNull(flags);
        
        if (partsHead == null)
            return null;
        
        Entry next = partsHead.next;
        if (next != null)
            next = new Entry(deserialize(next.part), next.next);

        return new Message(source, destination, this.flags & ~flags, files, next, serializationRegistry);
    }
    
    @Override
    public Message setFiles(List<File> files)
    {
        return new Message(source, destination, flags, files, partsHead, serializationRegistry);
    }
    
    @Override
    public Message retarget(IAddress source, IAddress destination)
    {
        if (source == null)
            source = this.source;
        if (destination == null)
            destination = this.destination;
        
        return new Message(source, destination, flags, files, partsHead, serializationRegistry);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        
        List<Integer> flags = new ArrayList<Integer>();
        for (int i = 0; i < 32; i++)
        {
            if ((this.flags & (1 << i)) != 0)
                flags.add(i);
        }
        builder.append(messages.messageHeader(destination, source, flags.toString()));
        
        Entry entry = partsHead;
        while (entry != null)
        {
            IMessagePart part = deserialize(entry.part);
            entry = entry.next;
            
            builder.append(messages.messagePart(part.getClass().getSimpleName(), Strings.wrap(part.toString(), 4, 120)));
        }
        
        return builder.toString();
    }
    
    private Message(IAddress source, IAddress destination, int flags, List<File> files, Entry partsHead, 
        ISerializationRegistry serializationRegistry)
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        Assert.notNull(serializationRegistry);
        
        this.source = source;
        this.destination = destination;
        this.flags = flags;
        this.files = Immutables.wrap(files);
        this.partsHead = partsHead;
        this.serializationRegistry = serializationRegistry;
    }

    private Entry serialize(Entry entry)
    {
        if (entry == null || entry.part instanceof SerializedMessagePart)
            return entry;
        
        return new Entry(serialize(entry.part), serialize(entry.next));
    }
    
    private SerializedMessagePart serialize(IMessagePart part)
    {
        Assert.isTrue(part != null && !(part instanceof SerializedMessagePart));
        
        //MultiByteOutputStream outputStream = new MultiByteOutputStream();
        ByteOutputStream outputStream = new ByteOutputStream();
        Serialization serialization = new Serialization(serializationRegistry, true, outputStream);
        serialization.writeObject(part);
        //outputStream.close();
        
        //return new SerializedMessagePart(serializationRegistry, outputStream.getBuffers(), outputStream.getLength());
        return new SerializedMessagePart(serializationRegistry, 
            Collections.singletonList(new ByteArray(outputStream.getBuffer(), 0, outputStream.getLength())), outputStream.getLength(), part);
    }
    
    private IMessagePart deserialize(IMessagePart part)
    {
        if (part instanceof SerializedMessagePart)
        {
            SerializedMessagePart serializedPart = (SerializedMessagePart)part;
            if (serializedPart.getMessagePart() != null)
                return serializedPart.getMessagePart();
            
            ByteArray buffer;
            if (serializedPart.getBuffers().size() > 1)
            {
                byte[] buf = new byte[serializedPart.getSize()];
                int pos = 0;
                for (ByteArray partBuffer : serializedPart.getBuffers())
                {
                    System.arraycopy(partBuffer.getBuffer(), partBuffer.getOffset(), buf, pos, partBuffer.getLength());
                    pos += partBuffer.getLength();
                }
                
                buffer = new ByteArray(buf);
            }
            else
                buffer = serializedPart.getBuffers().get(0);
            
            ByteInputStream inputStream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(serializedPart.getRegistry(), inputStream);
            return deserialization.readObject();
        }
        
        return part;
    }
    
    private static final class Entry
    {
        public final IMessagePart part;
        public final Entry next;
        public final int size;
        public final int count;
        
        public Entry(IMessagePart part, Entry next)
        {
            Assert.notNull(part);
            
            this.part = part;
            this.next = next;
            this.size = part.getSize() + (next != null ? next.size : 0);
            this.count = 1 + (next != null ? next.count : 0);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("destination: [{0}], source: [{1}], flags: {2}")
        ILocalizedMessage messageHeader(IAddress destination, IAddress source, String flags);
        @DefaultMessage("\npart ''{0}'':\n{1}")
        ILocalizedMessage messagePart(String name, String contents);
    }
}
