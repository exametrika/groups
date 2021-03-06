/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferClient;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateStore;
import com.exametrika.spi.groups.cluster.state.IStateTransferClient;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;
import com.exametrika.spi.groups.cluster.state.IStateTransferServer;

/**
 * The {@link CompositeSimpleStateTransferFactory} represents a composite simple state transfer factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompositeSimpleStateTransferFactory implements IStateTransferFactory
{
    private final List<IStateTransferFactory> factories;

    public CompositeSimpleStateTransferFactory(List<IStateTransferFactory> factories)
    {
        Assert.notNull(factories);
        
        this.factories = factories;
    }
    
    @Override
    public IStateStore createStore(UUID groupId)
    {
        return factories.get(0).createStore(groupId);
    }
    
    @Override
    public IStateTransferServer createServer(UUID groupId)
    {
        List<ISimpleStateTransferServer> servers = new ArrayList<ISimpleStateTransferServer>();
        for (IStateTransferFactory factory : factories)
            servers.add((ISimpleStateTransferServer)factory.createServer(groupId));
        
        return new CompositeSimpleStateTransferServer(servers);
    }

    @Override
    public IStateTransferClient createClient(UUID groupId)
    {
        List<ISimpleStateTransferClient> clients = new ArrayList<ISimpleStateTransferClient>();
        for (IStateTransferFactory factory : factories)
            clients.add((ISimpleStateTransferClient)factory.createClient(groupId));
        
        return new CompositeSimpleStateTransferClient(clients);
    }
    
    private static class CompositeSimpleStateTransferServer implements ISimpleStateTransferServer
    {
        private final List<ISimpleStateTransferServer> servers;

        public CompositeSimpleStateTransferServer(List<ISimpleStateTransferServer> servers)
        {
            Assert.notNull(servers);
            
            this.servers = servers;
        }
        
        @Override
        public MessageType classifyMessage(IMessage message)
        {
            MessageType result = MessageType.NON_STATE;
            for (ISimpleStateTransferServer server : servers)
            {
                MessageType messageType = server.classifyMessage(message);
                if (messageType.ordinal() > result.ordinal())
                    messageType = result;
            }
            return result;
        }

        @Override
        public ByteArray saveSnapshot(boolean full)
        {
            ByteOutputStream outputStream = new ByteOutputStream();
            DataSerialization serialization = new DataSerialization(outputStream);
            
            serialization.writeInt(servers.size());
            
            for (ISimpleStateTransferServer server : servers)
                serialization.writeByteArray(server.saveSnapshot(full));
            
            return new ByteArray(outputStream.getBuffer(), 0, outputStream.getLength());
        }
    }
    
    private static class CompositeSimpleStateTransferClient implements ISimpleStateTransferClient
    {
        private final List<ISimpleStateTransferClient> clients;

        public CompositeSimpleStateTransferClient(List<ISimpleStateTransferClient> clients)
        {
            Assert.notNull(clients);
            
            this.clients = clients;
        }

        @Override
        public void loadSnapshot(boolean full, ByteArray data)
        {
            ByteInputStream inputStream = new ByteInputStream(data.getBuffer(), data.getOffset(), data.getLength());
            DataDeserialization deserialization = new DataDeserialization(inputStream);
            
            int count = deserialization.readInt();
            for (int i = 0; i < count; i++)
                clients.get(i).loadSnapshot(full, deserialization.readByteArray());
        }
    }
}