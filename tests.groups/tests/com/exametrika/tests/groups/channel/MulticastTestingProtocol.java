/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;

/**
 * The {@link MulticastTestingProtocol} is a multicast testing protocol.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class MulticastTestingProtocol extends AbstractProtocol implements IFlushParticipant, IFailureDetectionListener
{
    private final long requestStatePeriod;
    private final long requestIdGap;
    private IGroupFailureDetector failureDetector;
    private boolean coordinator;
    private IFlush flush;
    private long lastRequestTime;
    private Set<IAddress> respondingNodes;
   
    public MulticastTestingProtocol(String channelName, IMessageFactory messageFactory, long requestStatePeriod,
        long requestIdGap)
    {
        super(channelName, messageFactory);
        
        this.requestStatePeriod = requestStatePeriod;
        this.requestIdGap = requestIdGap;
        this.lastRequestTime = Times.getCurrentTime();
    }
    
    public void setFailureDetector(IGroupFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new TestControlMessagePartSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(TestControlMessagePartSerializer.ID);
    }
    
    @Override
    public boolean isFlushProcessingRequired()
    {
        return true;
    }
    
    @Override
    public void setCoordinator()
    {
        coordinator = true;
    }

    @Override
    public void startFlush(IFlush flush)
    {
        this.flush = flush;
        flush.grantFlush(this);
    }

    @Override
    public void beforeProcessFlush()
    {
    }

    @Override
    public void processFlush()
    {
        flush.grantFlush(this);
    }

    @Override
    public void endFlush()
    {
        flush = null;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (!coordinator || flush != null || respondingNodes != null)
            return;
        
        if (currentTime > lastRequestTime + requestStatePeriod)
        {
            List<INode> healthyNodes = failureDetector.getHealthyMembers();
        }
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof TestControlMessagePart)
        {
            TestControlMessagePart part = message.getPart();
            
        }
        else
            receiver.receive(message);
    }
    
    private static final class TestControlMessagePart implements IMessagePart
    {
        private final String action;
        private final Map<String, Object> parameters;

        public TestControlMessagePart(String action, Map<String, Object> parameters)
        {
            Assert.notNull(action);
            Assert.notNull(parameters);
            
            this.action = action;
            this.parameters = parameters;
        }
        
        @Override
        public int getSize()
        {
            return 65536;
        }
        
        @Override
        public String toString()
        {
            return action + parameters;
        }
    }
    
    private static final class TestControlMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("ce4f0701-c317-4ab6-8069-5759517c4ebb");
     
        public TestControlMessagePartSerializer()
        {
            super(ID, TestControlMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestControlMessagePart part = (TestControlMessagePart)object;

            serialization.writeString(part.action);
            serialization.writeInt(part.parameters.size());
            for (Map.Entry<String, Object> entry : part.parameters.entrySet())
            {
                serialization.writeString(entry.getKey());
                serialization.writeObject(entry.getValue());
            }
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            String action = deserialization.readString();
            int count = deserialization.readInt();
            Map<String, Object> parameters = new HashMap<String, Object>();
            for (int i = 0; i < count; i++)
                parameters.put(deserialization.readString(), deserialization.readObject());
            
            return new TestControlMessagePart(action, parameters);
        }
    }
}
