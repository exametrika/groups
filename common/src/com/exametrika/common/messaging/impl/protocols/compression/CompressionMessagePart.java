/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.compression;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.impl.message.IWrapperMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link CompressionMessagePart} is a compression message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompressionMessagePart implements IWrapperMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ByteArray compressedMessage;
    private final int decompressedSize;
    private final IMessage originalMessage;

    public CompressionMessagePart(int decompressedSize, ByteArray compressedMessage, IMessage originalMessage)
    {
        Assert.notNull(compressedMessage);

        this.decompressedSize = decompressedSize;
        this.compressedMessage = compressedMessage;
        this.originalMessage = originalMessage;
    }
    
    public int getDecompressedSize()
    {
        return decompressedSize;
    }
    
    public ByteArray getCompressedMessage()
    {
        return compressedMessage;
    }

    @Override
    public IMessage getMessage()
    {
        return originalMessage;
    }

    @Override
    public int getSize()
    {
        return 4 + compressedMessage.getLength();
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(compressedMessage.getLength()).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("compressed message length: {0}")
        ILocalizedMessage toString(int length);
    }
}

