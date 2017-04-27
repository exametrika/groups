/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.groups.ISimpleStateStore;
import com.exametrika.spi.groups.ISimpleStateTransferClient;
import com.exametrika.spi.groups.ISimpleStateTransferServer;
import com.exametrika.spi.groups.IStateTransferFactory;

/**
 * The {@link ClusterMembershipStateTransferFactory} is a cluster membership state transfer factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipStateTransferFactory implements IStateTransferFactory
{
    private final IClusterMembershipManager membershipManager;
    private final List<IClusterMembershipProvider> membershipProviders;
    private final ISimpleStateStore stateStore;

    public ClusterMembershipStateTransferFactory(IClusterMembershipManager membershipManager,
        List<IClusterMembershipProvider> membershipProviders, ISimpleStateStore stateStore)
    {
        Assert.notNull(membershipManager);
        Assert.notNull(membershipProviders);
        Assert.notNull(stateStore);
        
        this.membershipManager = membershipManager;
        this.membershipProviders = membershipProviders;
        this.stateStore = stateStore;
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
        
        return new ClusterMembershipStateTransferServer();
    }

    @Override
    public ISimpleStateTransferClient createClient(UUID groupId)
    {
        Assert.notNull(groupId);
        Assert.isTrue(groupId.equals(GroupMemberships.CORE_GROUP_ID));
        
        return new ClusterMembershipStateTransferClient();
    }
    
    private class ClusterMembershipStateTransferServer implements ISimpleStateTransferServer
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
            if (!full)
                return new ByteArray(new byte[]{});
            IClusterMembership membership = membershipManager.getMembership();
            ClusterMembershipDelta delta = null;
            if (membership != null)
            {
                List<IDomainMembershipDelta> domains = new ArrayList<IDomainMembershipDelta>();
                for (IDomainMembership domain : membership.getDomains())
                {
                    List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
                    for (int i = 0; i < membershipProviders.size(); i++)
                        deltas.add(membershipProviders.get(i).createCoreFullDelta(domain.getElements().get(i)));
                    
                    domains.add(new DomainMembershipDelta(domain.getName(), deltas));
                }
                delta = new ClusterMembershipDelta(membership.getId(), true, domains);
            }
            
            SerializationRegistry registry = new SerializationRegistry();
            registry.register(new ClusterMembershipSerializationRegistrar());
            
            ByteOutputStream stream = new ByteOutputStream(0x1000);
            ISerialization serialization = new Serialization(registry, true, stream);
            serialization.writeTypedObject(delta);
            
            return new ByteArray(stream.getBuffer(), 0, stream.getLength());
        }
    }
    
    private class ClusterMembershipStateTransferClient implements ISimpleStateTransferClient
    {
        @Override
        public void loadSnapshot(boolean full, ByteArray buffer)
        {
            if (!full)
                return;
            
            SerializationRegistry registry = new SerializationRegistry();
            registry.register(new ClusterMembershipSerializationRegistrar());
            
            ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(registry, stream);
            ClusterMembershipDelta delta = deserialization.readTypedObject(ClusterMembershipDelta.class);
            if (delta == null)
                return;
            
            Assert.isTrue(delta.isFull());
            List<IDomainMembership> domains = new ArrayList<IDomainMembership>();
            for (IDomainMembershipDelta domainDelta : delta.getDomains())
            {
                Assert.isTrue(domainDelta.getDeltas().size() == membershipProviders.size());
               
                List<IClusterMembershipElement> elements = new ArrayList<IClusterMembershipElement>();
                IDomainMembership domain = new DomainMembership(domainDelta.getName(), elements);
                domains.add(domain);
                
                for (int i= 0; i < membershipProviders.size(); i++)
                {
                    IClusterMembershipElement element = membershipProviders.get(i).createMembership(domain,
                        domainDelta.getDeltas().get(i), null);
                    elements.add(element);
                }
            }
            IClusterMembership membership = new ClusterMembership(delta.getId(), domains);
            membershipManager.installMembership(membership);
        }
    }
}

