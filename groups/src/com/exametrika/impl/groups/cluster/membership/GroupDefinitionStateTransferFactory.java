/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;
import com.exametrika.spi.groups.cluster.state.ISimpleStateStore;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferClient;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

/**
 * The {@link GroupDefinitionStateTransferFactory} is a definition state transfer factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupDefinitionStateTransferFactory implements IStateTransferFactory
{
    private IGroupManagementService groupManagementService;
    private final ISimpleStateStore stateStore;
    private final ISerializationRegistry serializationRegistry;

    public GroupDefinitionStateTransferFactory(ISimpleStateStore stateStore)
    {
        Assert.notNull(stateStore);
        
        this.stateStore = stateStore;
        serializationRegistry = Serializers.createRegistry();
        serializationRegistry.register(new GroupDefinitionSerializer());
    }
    
    public void setGroupManagementService(IGroupManagementService groupManagementService)
    {
        Assert.notNull(groupManagementService);
        Assert.isNull(this.groupManagementService);
        
        this.groupManagementService = groupManagementService;
    }
    
    @Override
    public ISimpleStateStore createStore(UUID groupId)
    {
        Assert.notNull(groupId);
        Assert.isTrue(groupId.equals(GroupMemberships.CORE_GROUP_ID));
        
        return stateStore;
    }
    
    @Override
    public ISimpleStateTransferServer createServer(UUID groupId)
    {
        Assert.notNull(groupId);
        Assert.isTrue(groupId.equals(GroupMemberships.CORE_GROUP_ID));
        
        return new GroupDefinitionStateTransferServer();
    }

    @Override
    public ISimpleStateTransferClient createClient(UUID groupId)
    {
        Assert.notNull(groupId);
        Assert.isTrue(groupId.equals(GroupMemberships.CORE_GROUP_ID));
        
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
            List<GroupDefinition> groupDefinitions = groupManagementService.getGroupDefinitions();
            
            ByteOutputStream stream = new ByteOutputStream();
            ISerialization serialization = new Serialization(serializationRegistry, true, stream);
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
            ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(serializationRegistry, stream);
            int count = deserialization.readInt();
            for (int i = 0; i < count; i++)
            {
                GroupDefinition groupDefinition = deserialization.readTypedObject(GroupDefinition.class);
                groupManagementService.addGroupDefinition(groupDefinition);
            }
        }
    }
}

