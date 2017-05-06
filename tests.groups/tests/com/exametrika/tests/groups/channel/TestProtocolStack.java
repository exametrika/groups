/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.channel;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.transports.UnicastAddressSerializer;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Serializers;
import com.exametrika.tests.groups.mocks.LiveNodeProviderMock;

/**
 * The {@link TestProtocolStack} is a test protocol stack.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class TestProtocolStack extends AbstractProtocol implements ITimeService, IConnectionProvider
{
    private TestTerminator terminator;
    private ISerializationRegistry serializationRegistry;
    private AbstractProtocol protocol;
    private List<IMessage> sentMessages = new ArrayList<IMessage>();
    private List<IMessage> receivedMessages = new ArrayList<IMessage>();
    private long currentTime;
    
    public static TestProtocolStack create(String channelName)
    {
        ISerializationRegistry serializationRegistry = Serializers.createRegistry();
        serializationRegistry.register(new UnicastAddressSerializer());
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        MessageFactory messageFactory = new MessageFactory(serializationRegistry, liveNodeProvider);
        
        return new TestProtocolStack(channelName, messageFactory, serializationRegistry);
    }
    
    public void setProtocol(AbstractProtocol protocol)
    {
        this.protocol = protocol;
    }
    
    public ISerializationRegistry getSerializationRegistry()
    {
        return serializationRegistry;
    }
    
    public AbstractProtocol getProtocol()
    {
        return protocol;
    }
    
    public List<IMessage> getSentMessages()
    {
        return sentMessages;
    }
    
    public List<IMessage> getReceivedMessages()
    {
        return receivedMessages;
    }
    
    @Override
    public void start()
    {
        Assert.notNull(protocol);
        
        protocol.setSender(terminator);
        protocol.setPullableSender(terminator);
        protocol.setReceiver(terminator);
        protocol.setTimeService(this);
        protocol.setConnectionProvider(this);
        
        setSender(protocol);
        setPullableSender(protocol);
        setReceiver(protocol);
        
        serializationRegistry.register(protocol);
        
        protocol.start();
    }
    
    @Override
    public void stop()
    {
        protocol.stop();
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        this.currentTime = currentTime;
        protocol.onTimer(currentTime);
    }
    

    @Override
    public void connect(String connection)
    {
    }

    @Override
    public void connect(IAddress connection)
    {
    }

    @Override
    public void disconnect(String connection)
    {
    }

    @Override
    public void disconnect(IAddress connection)
    {
    }

    @Override
    public String canonicalize(String connection)
    {
        return connection;
    }

    @Override
    public long getCurrentTime()
    {
        return currentTime;
    }
    
    public void reset()
    {
        receivedMessages.clear();
        sentMessages.clear();
    }
    
    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }
    
    private void checkSerialization(IMessage message)
    {
        ByteOutputStream out = new ByteOutputStream();
        Serialization serialization = new Serialization(serializationRegistry, true, out);
        MessageSerializers.serializeFully(serialization, (Message)message);
        
        ByteInputStream in = new ByteInputStream(out.getBuffer(), 0, out.getLength());
        Deserialization deserialization = new Deserialization(serializationRegistry, in);
        Assert.notNull(MessageSerializers.deserializeFully(deserialization));
    }
    
    private TestProtocolStack(String channelName, IMessageFactory messageFactory, ISerializationRegistry serializationRegistry) 
    {
        super(channelName, messageFactory);
        
        this.serializationRegistry = serializationRegistry;
        terminator = new TestTerminator(channelName, messageFactory);
    }
    
    private class TestTerminator extends AbstractProtocol
    {
        public TestTerminator(String channelName, IMessageFactory messageFactory)
        {
            super(channelName, messageFactory);
        }

        @Override
        protected boolean supportsPullSendModel()
        {
            return true;
        }
        
        @Override
        protected void doSend(ISender sender, IMessage message)
        {
            checkSerialization(message);
            sentMessages.add(message);
        }
        
        @Override
        protected boolean doSend(IFeed feed, ISink sink, IMessage message)
        {
            doSend(null, message);
            return true;
        }
        
        @Override
        protected void doReceive(IReceiver receiver, IMessage message)
        {
            checkSerialization(message);
            receivedMessages.add(message);   
        }
    }
}
