/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.groups.ISimpleStateTransferClient;
import com.exametrika.spi.groups.ISimpleStateTransferFactory;
import com.exametrika.spi.groups.ISimpleStateTransferServer;

/**
 * The {@link GroupDefinitionStateTransferFactory} is a definition state transfer factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupDefinitionStateTransferFactory implements ISimpleStateTransferFactory
{
    private SimpleGroupMappingStrategy groupMappingStrategy;

    public void setGroupMappingStrategy(SimpleGroupMappingStrategy groupMappingStrategy)
    {
        Assert.notNull(groupMappingStrategy);
        Assert.isNull(this.groupMappingStrategy);
        
        this.groupMappingStrategy = groupMappingStrategy;
    }
    
    @Override
    public ISimpleStateTransferServer createServer()
    {
        return new GroupDefinitionStateTransferServer();
    }

    @Override
    public ISimpleStateTransferClient createClient()
    {
        return new GroupDefinitionStateTransferClient();
    }
    
    private class GroupDefinitionStateTransferServer implements ISimpleStateTransferServer
    {
        @Override
        public MessageType classifyMessage(IMessage message)
        {
            if (message.getDestination().equals(GroupMemberships.CORE_GROUP_ADDRESS))
                return MessageType.STATE_WRITE;
            else
                return MessageType.NON_STATE;
        }

        @Override
        public ByteArray saveSnapshot(boolean full)
        {
            List<GroupDefinition> groupDefinitions = groupMappingStrategy.getGroupDefinitions();
            
            SerializationRegistry registry = new SerializationRegistry();
            registry.register(new GroupDefinitionSerializer());
            
            ByteOutputStream stream = new ByteOutputStream();
            ISerialization serialization = new Serialization(registry, true, stream);
            serialization.writeInt(groupDefinitions.size());
            for (GroupDefinition groupDefinition : groupDefinitions)
                serialization.writeTypedObject(groupDefinition);
            
            return new ByteArray(stream.getBuffer(), 0, stream.getLength());
        }
    }
    
    private class GroupDefinitionStateTransferClient implements ISimpleStateTransferClient
    {
        @Override
        public void loadSnapshot(boolean full, ByteArray buffer)
        {
            SerializationRegistry registry = new SerializationRegistry();
            registry.register(new GroupDefinitionSerializer());
            
            ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(registry, stream);
            int count = deserialization.readInt();
            for (int i = 0; i < count; i++)
            {
                GroupDefinition groupDefinition = deserialization.readTypedObject(GroupDefinition.class);
                groupMappingStrategy.addGroup(groupDefinition);
            }
        }
    }
}

