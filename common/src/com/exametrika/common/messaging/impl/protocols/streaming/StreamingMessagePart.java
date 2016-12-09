/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.streaming;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link StreamingMessagePart} is a stream fragment message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StreamingMessagePart implements IMessagePart
{
    private static final int FLAG_FIRST = 0x1;
    private static final int FLAG_LAST = 0x2;
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int id;
    private final int streamIndex;
    private final int streamCount;
    private final byte flags;
    private final ByteArray fragment;

    public StreamingMessagePart(int id, int streamIndex, int streamCount, byte flags, ByteArray fragment)
    {
        this.id = id;
        this.streamIndex = streamIndex;
        this.streamCount = streamCount;
        this.flags = flags;
        this.fragment = fragment;
    }
    
    public StreamingMessagePart(int id, int streamIndex, int streamCount, boolean first, boolean last, ByteArray fragment)
    {
        this.id = id;
        this.streamIndex = streamIndex;
        this.streamCount = streamCount;
        this.fragment = fragment;
        this.flags = (byte)((first ? FLAG_FIRST : 0) | (last ? FLAG_LAST : 0));
    }
    
    public int getId()
    {
        return id;
    }

    public int getStreamIndex()
    {
        return streamIndex;
    }
    
    public int getStreamCount()
    {
        return streamCount;
    }

    public ByteArray getFragment()
    {
        return fragment;
    }
    
    public byte getFlags()
    {
        return flags;
    }
    
    public boolean isFirst()
    {
        return (flags & FLAG_FIRST) != 0;
    }
    
    public boolean isLast()
    {
        return (flags & FLAG_LAST) != 0;
    }
    
    @Override
    public int getSize()
    {
        return (fragment != null ? fragment.getLength() : 0) + 13;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(streamIndex + 1, streamCount, isFirst(), isLast(), fragment != null ? fragment.getLength() : 0).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("stream: {0}/{1}, first: {2}, last: {3}, fragment size: {4}")
        ILocalizedMessage toString(int streamIndex, int streamCount, boolean first, boolean last, int fragmentSize);
    }
}

