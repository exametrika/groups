/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;

/**
 * The {@link PiggybackProtocol} represents a protocol that optimizes send of control messages by piggybacking them on payload messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PiggybackProtocol extends AbstractProtocol implements IPiggybackManager
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Map<IAddress, List<IPiggybackSender>> sendersMap = new HashMap<IAddress, List<IPiggybackSender>>();
    private final Map<IAddress, List<IPiggybackReceiver>> receiversMap = new HashMap<IAddress, List<IPiggybackReceiver>>();
    
    public PiggybackProtocol(String channelName, IMessageFactory messageFactory)
    {
        super(channelName, messageFactory);
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new PiggybackMessagePartSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(PiggybackMessagePartSerializer.ID);
    }
    
    @Override
    public void registerSender(IAddress address, IPiggybackSender sender)
    {
        Assert.notNull(address);
        Assert.notNull(sender);
        
        List<IPiggybackSender> senders = sendersMap.get(address);
        if (senders == null)
        {
            senders = new ArrayList<IPiggybackSender>();
            sendersMap.put(address, senders);
        }
        
        senders.add(sender);
    }

    @Override
    public void registerReceiver(IAddress address, IPiggybackReceiver receiver)
    {
        Assert.notNull(address);
        Assert.notNull(receiver);
        
        List<IPiggybackReceiver> receivers = receiversMap.get(address);
        if (receivers == null)
        {
            receivers = new ArrayList<IPiggybackReceiver>();
            receiversMap.put(address, receivers);
        }
        
        receivers.add(receiver);
    }
    
    @Override
    public void unregisterSender(IAddress address, IPiggybackSender sender)
    {
        Assert.notNull(address);
        Assert.notNull(sender);
        
        List<IPiggybackSender> senders = sendersMap.get(address);
        if (senders != null)
        {
            senders.remove(sender);
        
            if (senders.isEmpty())
                sendersMap.remove(address);
        }
    }
    
    @Override
    public void unregisterReceiver(IAddress address, IPiggybackReceiver receiver)
    {
        Assert.notNull(address);
        Assert.notNull(receiver);
        
        List<IPiggybackReceiver> receivers = receiversMap.get(address);
        if (receivers != null)
        {
            receivers.remove(receiver);
        
            if (receivers.isEmpty())
                receiversMap.remove(address);
        }
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        message = piggyback(message);
        super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        message = piggyback(message);
        return super.doSend(feed, sink, message);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof PiggybackMessagePart)
        {
            PiggybackMessagePart part = message.getPart();
            List<IPiggybackReceiver> receivers = receiversMap.get(message.getSource());
            if (receivers != null)
            {
                for (IMessagePart child : part.getParts())
                {
                    boolean handled = false;
                    for (IPiggybackReceiver piggybackReceiver : receivers)
                    {
                        if (piggybackReceiver.receive(child))
                        {
                            handled = true;
                            break;
                        }
                    }
                    
                    if (!handled  && logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, messages.receiverNotFoundForMessagePart(child));
                }
            }
            else if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.receiverNotFoundForSource(message.getSource()));
            
            message = message.removePart();
        }
        
        super.doReceive(receiver, message);
    }

    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }

    private IMessage piggyback(IMessage message)
    {
        List<IMessagePart> parts = null;
        List<IPiggybackSender> senders = sendersMap.get(message.getDestination());
        if (senders != null)
        {
            for (IPiggybackSender sender : senders)
            {
                IMessagePart child = sender.send();
                if (child != null)
                {
                    if (parts == null)
                        parts = new ArrayList<IMessagePart>();
                    
                    parts.add(child);
                }
            }
        }
        
        if (parts != null)
            return message.addPart(new PiggybackMessagePart(parts));
        else
            return message;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Piggyback receiver is not found for message source ''{0}''.")
        ILocalizedMessage receiverNotFoundForSource(IAddress source);
        @DefaultMessage("Piggyback receiver is not found for message part:\n{0}")
        ILocalizedMessage receiverNotFoundForMessagePart(IMessagePart part);
    }
}
