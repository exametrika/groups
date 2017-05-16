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
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IPullableSender;
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
    private LiveNodeProviderMock liveNodeProvider;
    private TestTerminator terminator;
    private ISerializationRegistry serializationRegistry;
    private AbstractProtocol protocol;
    private List<IMessage> sentMessages = new ArrayList<IMessage>();
    private List<IMessage> receivedMessages = new ArrayList<IMessage>();
    private long currentTime;
    private Object object;
    private boolean active = true;
    
    public static TestProtocolStack create(String channelName)
    {
        ISerializationRegistry serializationRegistry = Serializers.createRegistry();
        serializationRegistry.register(new UnicastAddressSerializer());
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock(channelName);
        MessageFactory messageFactory = new MessageFactory(serializationRegistry, liveNodeProvider);
        
        return new TestProtocolStack(channelName, liveNodeProvider, messageFactory, serializationRegistry);
    }
    
    public void setProtocol(AbstractProtocol protocol)
    {
        this.protocol = protocol;
    }
    
    public void setObject(Object object)
    {
        this.object = object;
    }
    
    public IAddress getAddress()
    {
        return liveNodeProvider.localNode;
    }
    
    public ILiveNodeProvider getLiveNodeProvider()
    {
        return liveNodeProvider;
    }
    
    public ISerializationRegistry getSerializationRegistry()
    {
        return serializationRegistry;
    }
    
    public <T extends AbstractProtocol> T getProtocol()
    {
        return (T)protocol;
    }
    
    public List<IMessage> getSentMessages()
    {
        return sentMessages;
    }
    
    public List<IMessage> getReceivedMessages()
    {
        return receivedMessages;
    }
    
    public <T> T getObject()
    {
        return (T)object;
    }
    
    public boolean isActive()
    {
        return active;
    }
    
    public void setActive(boolean active)
    {
        this.active = active;
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
    
    public void clearSentMessages()
    {
        sentMessages.clear();
    }
    
    public void clearReceivedMessages()
    {
        receivedMessages.clear();
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
    
    private TestProtocolStack(String channelName, LiveNodeProviderMock liveNodeProvider, IMessageFactory messageFactory,
        ISerializationRegistry serializationRegistry) 
    {
        super(channelName, messageFactory);
        
        this.liveNodeProvider = liveNodeProvider;
        this.serializationRegistry = serializationRegistry;
        terminator = new TestTerminator();
    }
    
    private class TestTerminator implements ISender, IReceiver, IPullableSender
    {
        @Override
        public ISink register(IAddress destination, IFeed feed)
        {
            Assert.supports(false);
            return null;
        }

        @Override
        public void unregister(ISink sink)
        {
            Assert.supports(false);
        }

        @Override
        public void receive(IMessage message)
        {
            checkSerialization(message);
            receivedMessages.add(message);
        }

        @Override
        public void send(IMessage message)
        {
            checkSerialization(message);
            sentMessages.add(message);
        }
    }
}
