/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.compression;


import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Strings;


/**
 * The {@link CompressionProtocol} is used to compress/decompress messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompressionProtocol extends AbstractProtocol
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int compressionLevel;
    private final ISerializationRegistry serializationRegistry;

    public CompressionProtocol(String channelName, int compressionLevel, IMessageFactory messageFactory,
        ISerializationRegistry serializationRegistry)
    {
        this(channelName, null, compressionLevel, messageFactory, serializationRegistry);
    }
    
    public CompressionProtocol(String channelName, String loggerName, int compressionLevel, IMessageFactory messageFactory,
        ISerializationRegistry serializationRegistry)
    {
        super(channelName, loggerName, messageFactory);
        
        Assert.notNull(serializationRegistry);
        
        this.compressionLevel = compressionLevel;
        this.serializationRegistry = serializationRegistry;
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new CompressionMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(CompressionMessagePartSerializer.ID);
    }

    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (!message.hasFlags(MessageFlags.NO_COMPRESS))
            message = compress(message);
        
        super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        if (!message.hasFlags(MessageFlags.NO_COMPRESS))
            message = compress(message);
        
        return super.doSend(feed, sink, message);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof CompressionMessagePart)
            message = decompress(message);
        super.doReceive(receiver, message);
    }

    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }

    private IMessage compress(IMessage message)
    {
        ByteOutputStream stream = new ByteOutputStream();
        Serialization serialization = new Serialization(serializationRegistry, true, stream);
        MessageSerializers.serialize(serialization, (Message)message);
        
        int maxCompressedLength = LZ4.maxCompressedLength(stream.getLength());
        byte[] buffer = new byte[maxCompressedLength];
        
        int compressedLength = LZ4.compress(compressionLevel <= 5, stream.getBuffer(), 0, stream.getLength(), 
            buffer, 0, maxCompressedLength);
        
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.messageCompressed(Strings.wrap(message.toString(), 4, 120),
                stream.getLength(), compressedLength));
        
        return messageFactory.create(message.getDestination(), new CompressionMessagePart(stream.getLength(),
            new ByteArray(buffer, 0, compressedLength), message), message.getFlags() | MessageFlags.NO_COMPRESS, message.getFiles());
    }

    private IMessage decompress(IMessage message)
    {
        CompressionMessagePart part = message.getPart();
        
        ByteArray compressedMessage = part.getCompressedMessage();
        byte[] buffer = new byte[part.getDecompressedSize()];
        LZ4.decompress(compressedMessage.getBuffer(), compressedMessage.getOffset(), buffer, 0, buffer.length);
        
        ByteInputStream stream = new ByteInputStream(buffer, 0, buffer.length);
        Deserialization deserialization = new Deserialization(serializationRegistry, stream);

        IMessage outputMessage = MessageSerializers.deserialize(deserialization, message.getSource(), message.getDestination(),
            message.getFiles());
        
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.messageDecompressed(Strings.wrap(outputMessage.toString(), 4, 120), 
                compressedMessage.getLength(), stream.getLength()));
        
        return outputMessage;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Message has been compressed, size: {1}, compressed size: {2}, message:\n{0}.")
        ILocalizedMessage messageCompressed(String message, int size, int compressedSize);
        @DefaultMessage("Message has been decompressed, size: {2}, compressed size: {1}, message:\n{0}.")
        ILocalizedMessage messageDecompressed(String outputMessage, int compressedSize, int size);
    }
}
