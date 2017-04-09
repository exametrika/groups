/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.spi.groups.ISimpleStateTransferFactory;
import com.exametrika.spi.groups.ISimpleStateTransferServer;

/**
 * The {@link SimpleStateTransferServerProtocol} represents a simple state transfer server protocol, which keeps state in memory.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SimpleStateTransferServerProtocol extends AbstractProtocol
{
    private final ISimpleStateTransferServer server;
    
    public SimpleStateTransferServerProtocol(String channelName, IMessageFactory messageFactory, 
        ISimpleStateTransferFactory stateTransferFactory)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(stateTransferFactory);
        
        this.server = stateTransferFactory.createServer();
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.hasFlags(MessageFlags.STATE_TRANSFER_REQUEST))
        {
            ByteArray state = server.saveSnapshot();
                
            send(messageFactory.create(message.getSource(), new SimpleStateTransferResponseMessagePart(state)));
        }
        else
            receiver.receive(message);
    }
}
