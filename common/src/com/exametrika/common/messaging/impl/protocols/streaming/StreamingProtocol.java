/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.streaming;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.IStreamReceiveHandler;
import com.exametrika.common.messaging.IStreamSendHandler;
import com.exametrika.common.messaging.impl.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Strings;


/**
 * The {@link StreamingProtocol} is used to fragment/defragment streaming messages. Streaming message must contain {@link IStreamSendHandler}
 * on send and {@link IStreamReceiveHandler} on receive implementation in current message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StreamingProtocol extends AbstractProtocol
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int maxFragmentSize;
    private final Map<IFeed, StreamSendInfo> outgoingStreams = new HashMap<IFeed, StreamSendInfo>();
    private final Map<StreamId, StreamReceiveInfo> incomingStreams = new HashMap<StreamId, StreamReceiveInfo>();
    private AtomicInteger nextStreamId = new AtomicInteger();
    private final boolean receivePart;
    private final boolean sendPart;

    public StreamingProtocol(String channelName, IMessageFactory messageFactory, int maxFragmentSize, 
        boolean receivePart, boolean sendPart)
    {
        this(channelName, null, messageFactory, maxFragmentSize, receivePart, sendPart);
    }
    
    public StreamingProtocol(String channelName, String loggerName, IMessageFactory messageFactory, int maxFragmentSize, 
        boolean receivePart, boolean sendPart)
    {
        super(channelName, loggerName, messageFactory);
        
        this.maxFragmentSize = maxFragmentSize;
        this.receivePart = receivePart;
        this.sendPart = sendPart;
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new StreamingMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(StreamingMessagePartSerializer.ID);
    }

    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        synchronized (incomingStreams)
        {
            for (Iterator<Map.Entry<StreamId, StreamReceiveInfo>> it = incomingStreams.entrySet().iterator(); it.hasNext();)
            {
                StreamReceiveInfo info = it.next().getValue();
                if (cleanupManager.canCleanup(info.message.getSource()))
                {
                    IStreamReceiveHandler handler = info.message.getPart();
                    handler.receiveCanceled();
                    it.remove();
                    
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, marker, messages.receiveCanceled(Strings.wrap(info.message.toString(), 4, 120)));
                }
            }
        }
        
        synchronized (outgoingStreams)
        {
            for (Iterator<Map.Entry<IFeed, StreamSendInfo>> it = outgoingStreams.entrySet().iterator(); it.hasNext();)
            {
                StreamSendInfo info = it.next().getValue();
                if (cleanupManager.canCleanup(info.message.getDestination()))
                {
                    IStreamSendHandler handler = info.message.getPart();
                    handler.sendCanceled();
                    it.remove();
                    
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, marker, messages.sendCanceled(Strings.wrap(info.message.toString(), 4, 120)));
                }
            }
        }
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (!sendPart || !(message.getPart() instanceof IStreamSendHandler))
        {
            super.doSend(sender, message);
            return;
        }
        
        IStreamSendHandler handler = message.getPart();
        
        int streamCount = handler.getStreamCount();
        if (streamCount == 0)
        {
            super.doSend(sender, message);
            return;
        }
        
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.messageFragmented(Strings.wrap(message.toString(), 4, 120)));
        
        int id = nextStreamId.getAndIncrement();
        
        handler.sendStarted();
        boolean sent = false;
        
        for (int i = 0; i < streamCount; i++)
        {
            handler.sendStreamStarted(i);
            
            boolean first = true;
            while (true)
            {
                byte[] buffer = new byte[maxFragmentSize];
                int length = handler.read(buffer);
                boolean lastFragment = !handler.hasData(); 
    
                ByteArray fragment = null;
                if (length != -1)
                    fragment = new ByteArray(buffer, 0, length);
                
                if (!sent)
                {
                    message = message.addPart(new StreamingMessagePart(id, i, streamCount, true, 
                        lastFragment, fragment)).addFlags(MessageFlags.NO_DELAY);
                    sent = true;
                }
                else
                    message = messageFactory.create(message.getDestination(), new StreamingMessagePart(id, i, streamCount, first, 
                        lastFragment, fragment), message.getFlags() | MessageFlags.NO_DELAY, null);
                    
                sender.send(message);
                
                first = false;
                
                if (lastFragment)
                    break;
            }
            
            handler.sendStreamCompleted();
        }
        
        handler.sendCompleted();
    }

    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        if (!sendPart || !(message.getPart() instanceof IStreamSendHandler))
            return sink.send(message);
        
        IStreamSendHandler handler = message.getPart();
        
        int streamCount = handler.getStreamCount();
        if (streamCount == 0)
        {
            return sink.send(message);
        }
        
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.messageFragmented(Strings.wrap(message.toString(), 4, 120)));

        int id = nextStreamId.getAndIncrement();
        
        handler.sendStarted();
        
        int i = 0;
        boolean res = true;
        boolean completed = false;
        boolean streamStarted = true;
        boolean sent = false;
        while (i < streamCount)
        {
            handler.sendStreamStarted(i);
            streamStarted = true;
            
            boolean lastFragment;
            boolean first = true;
            while (true)
            {
                byte[] buffer = new byte[maxFragmentSize];
                int length = handler.read(buffer);
                lastFragment = !handler.hasData(); 
    
                ByteArray fragment = null;
                if (length != -1)
                    fragment = new ByteArray(buffer, 0, length); 
    
                IMessage newMessage;
                if (!sent)
                {
                    newMessage = message.addPart(new StreamingMessagePart(id, i, streamCount, true, 
                        lastFragment, fragment)).addFlags(MessageFlags.NO_DELAY);
                    sent = true;
                }
                else
                    newMessage = messageFactory.create(message.getDestination(), new StreamingMessagePart(id, i, streamCount, first, 
                        lastFragment, fragment), message.getFlags() | MessageFlags.NO_DELAY, null);
                    
                first = false;
                
                res = sink.send(newMessage); 
                if (!res)
                    break;
                
                if (lastFragment)
                    break;
            }
            
            if (lastFragment)
            {
                handler.sendStreamCompleted();
                
                i++;
                
                if (i == streamCount)
                    completed = true;
                
                streamStarted = false;
            }
            
            if (!res)
                break;
        }

        if (!completed)
        {
            synchronized (outgoingStreams)
            {
                outgoingStreams.put(feed, new StreamSendInfo(message, id, i, streamCount, streamStarted));
            }
            return false;
        }
        else
        {
            handler.sendCompleted();
            return res;
        }
    }
    
    @Override
    protected boolean doSendPending(IFeed feed, ISink sink)
    {
        if (!sendPart)
            return true;
        
        StreamSendInfo info;
        synchronized (outgoingStreams)
        {
            info = outgoingStreams.get(feed);
            if (info == null)
                return true;
        }
        
        IStreamSendHandler handler = info.message.getPart();
        boolean res = true;
        boolean completed = false;
        
        while (info.streamIndex < info.streamCount)
        {
            boolean first = false;
            if (!info.streamStarted)
            {
                handler.sendStreamStarted(info.streamIndex);
                info.streamStarted = true;
                first = true;
            }
            
            boolean lastFragment;
            while (true)
            {
                byte[] buffer = new byte[maxFragmentSize];
                int length = handler.read(buffer);
                lastFragment = !handler.hasData(); 
    
                ByteArray fragment = null;
                if (length != -1)
                    fragment = new ByteArray(buffer, 0, length); 
    
                IMessage message = messageFactory.create(info.message.getDestination(), new StreamingMessagePart(info.id, 
                    info.streamIndex, info.streamCount, first, lastFragment, fragment), 
                    info.message.getFlags() | MessageFlags.NO_DELAY, null);
                
                first = false;
                
                res = sink.send(message); 
                if (!res)
                    break;
                
                if (lastFragment)
                    break;
            }
            
            if (lastFragment)
            {
                handler.sendStreamCompleted();
                
                info.streamIndex++;
                
                if (info.streamIndex == info.streamCount)
                    completed = true;
                
                info.streamStarted = false;
            }
            
            if (!res)
                break;
        }
    
        if (!completed)
            return false;
        else
        {
            handler.sendCompleted();
            
            synchronized (outgoingStreams)
            {
                outgoingStreams.remove(feed);
            }
            return res;
        }
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (!receivePart || !(message.getPart() instanceof StreamingMessagePart))
        {
            super.doReceive(receiver, message);
            return;
        }
        
        StreamingMessagePart part = message.getPart();
        StreamId id = new StreamId(message.getSource(), part.getId());

        IMessage firstMessage;
        if (part.isFirst() && part.getStreamIndex() == 0)
        {
            firstMessage = message.removePart();
            
            if (!part.isLast() || part.getStreamCount() > 1)
            {
                synchronized (incomingStreams)
                {
                    incomingStreams.put(id, new StreamReceiveInfo(firstMessage));
                }
            }
        }
        else
        {
            StreamReceiveInfo info;
            synchronized (incomingStreams)
            {
                info = incomingStreams.get(id);
            }
            
            if (info == null)
                return;
            
            firstMessage = info.message;
        }

        IStreamReceiveHandler handler = firstMessage.getPart();
        
        if (part.isFirst())
        {
            if (part.getStreamIndex() == 0)
                handler.receiveStarted(part.getStreamCount());
            
            handler.receiveStreamStarted(part.getStreamIndex());
        }
        
        if (part.getFragment() != null)
            handler.write(part.getFragment().getBuffer(), part.getFragment().getOffset(), part.getFragment().getLength());

        if (part.isLast())
        {
            handler.receiveStreamCompleted();
            
            if (part.getStreamIndex() == part.getStreamCount() - 1)
            {
                handler.receiveCompleted();
                
                if (!part.isFirst() || part.getStreamCount() > 1)
                {
                    synchronized (incomingStreams)
                    {
                        incomingStreams.remove(id);
                    }
                }
                
                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, marker, messages.messageDefragmented(Strings.wrap(firstMessage.toString(), 4, 120)));
                
                receiver.receive(firstMessage);
            }
        }
    }

    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }

    private static final class StreamId
    {
        private final IAddress address;
        private final int id;
        
        public StreamId(IAddress address, int id)
        {
            Assert.notNull(address);
            
            this.address = address;
            this.id = id;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            
            if (!(o instanceof StreamId))
                return false;
            
            StreamId streamId = (StreamId)o;
            return address.equals(streamId.address) && id == streamId.id;
        }
        
        @Override
        public int hashCode()
        {
            return 31 * id + address.hashCode();
        }
    }
    
    private static class StreamSendInfo
    {
        private final IMessage message;
        private final int id;
        private final int streamCount;
        private int streamIndex;
        private boolean streamStarted;
        
        public StreamSendInfo(IMessage message, int id, int streamIndex, int streamCount, boolean streamStarted)
        {
            this.message = message;
            this.id = id;
            this.streamCount = streamCount;
            this.streamIndex = streamIndex;
            this.streamStarted = streamStarted;
        }
    }
    private static class StreamReceiveInfo
    {
        private final IMessage message;
        
        public StreamReceiveInfo(IMessage message)
        {
            this.message = message;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Message has been fragmented:\n{0}.")
        ILocalizedMessage messageFragmented(String message);
        @DefaultMessage("Message has been defragmented:\n{0}.")
        ILocalizedMessage messageDefragmented(String message);
        @DefaultMessage("Sending of message has been canceled:\n{0}.")
        ILocalizedMessage sendCanceled(String message);
        @DefaultMessage("Receiving of message has been canceled:\n{0}.")
        ILocalizedMessage receiveCanceled(String message);
    }
}
