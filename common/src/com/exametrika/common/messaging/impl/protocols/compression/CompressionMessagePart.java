/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.compression;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link CompressionMessagePart} is a compression message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompressionMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ByteArray compressedMessage;
    private final int decompressedSize;

    public CompressionMessagePart(int decompressedSize, ByteArray compressedMessage)
    {
        Assert.notNull(compressedMessage);

        this.decompressedSize = decompressedSize;
        this.compressedMessage = compressedMessage;
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

