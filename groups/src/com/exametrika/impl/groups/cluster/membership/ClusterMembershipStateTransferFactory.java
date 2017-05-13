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
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.impl.transports.UnicastAddressSerializer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;
import com.exametrika.spi.groups.cluster.state.ISimpleStateStore;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferClient;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

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
    private final List<ICoreClusterMembershipProvider> coreMembershipProviders;
    private final ISimpleStateStore stateStore;
    private final ISerializationRegistry serializationRegistry;

    public ClusterMembershipStateTransferFactory(IClusterMembershipManager membershipManager,
        List<IClusterMembershipProvider> membershipProviders, List<ICoreClusterMembershipProvider> coreMembershipProviders,
        ISimpleStateStore stateStore)
    {
        Assert.notNull(membershipManager);
        Assert.notNull(membershipProviders);
        Assert.notNull(coreMembershipProviders);
        Assert.notNull(stateStore);
        
        this.membershipManager = membershipManager;
        this.membershipProviders = membershipProviders;
        this.coreMembershipProviders = coreMembershipProviders;
        this.stateStore = stateStore;
        serializationRegistry = Serializers.createRegistry();
        serializationRegistry.register(new ClusterMembershipSerializationRegistrar());
        serializationRegistry.register(new UnicastAddressSerializer());
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
            ClusterMembership membership = (ClusterMembership)membershipManager.getMembership();
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
                
                IDomainMembership coreDomain = membership.getCoreDomain();
                List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
                for (int i = 0; i < coreMembershipProviders.size(); i++)
                    deltas.add(coreMembershipProviders.get(i).createCoreFullDelta(coreDomain.getElements().get(i)));
                
                DomainMembershipDelta coreDomainDelta = new DomainMembershipDelta(coreDomain.getName(), deltas);
                delta = new ClusterMembershipDelta(membership.getId(), true, domains, coreDomainDelta);
            }
            
            ByteOutputStream stream = new ByteOutputStream(0x1000);
            ISerialization serialization = new Serialization(serializationRegistry, true, stream);
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
            
            ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(serializationRegistry, stream);
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
                
                for (int i = 0; i < membershipProviders.size(); i++)
                {
                    IClusterMembershipElement element = membershipProviders.get(i).createMembership(domain,
                        domainDelta.getDeltas().get(i), null);
                    elements.add(element);
                }
            }
            
            IDomainMembershipDelta coreDomainDelta = delta.getCoreDomain();
            Assert.isTrue(coreDomainDelta.getDeltas().size() == coreMembershipProviders.size());
            
            List<IClusterMembershipElement> elements = new ArrayList<IClusterMembershipElement>();
            IDomainMembership coreDomain = new DomainMembership(coreDomainDelta.getName(), elements);
            IClusterMembership membership = new ClusterMembership(delta.getId(), domains, coreDomain);
            
            for (int i = 0; i < coreMembershipProviders.size(); i++)
            {
                IClusterMembershipElement element = coreMembershipProviders.get(i).createMembership(membership,
                    coreDomainDelta.getDeltas().get(i), null);
                elements.add(element);
            }
            
            membershipManager.installMembership(membership);
        }
    }
}

