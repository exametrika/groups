/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.impl.groups.core.membership.Memberships;
import com.exametrika.spi.groups.IStateTransferClient;
import com.exametrika.spi.groups.IStateTransferFactory;
import com.exametrika.spi.groups.IStateTransferServer;

/**
 * The {@link ClusterMembershipStateTransferFactory} is a cluster membership state transfer factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipStateTransferFactory implements IStateTransferFactory
{
    private final ICompartment compartment;
    private final IClusterMembershipManager membershipManager;
    private final List<IClusterMembershipProvider> membershipProviders;

    public ClusterMembershipStateTransferFactory(ICompartment compartment, IClusterMembershipManager membershipManager,
        List<IClusterMembershipProvider> membershipProviders)
    {
        Assert.notNull(compartment);
        Assert.notNull(membershipManager);
        Assert.notNull(membershipProviders);
        
        this.compartment = compartment;
        this.membershipManager = membershipManager;
        this.membershipProviders = membershipProviders;
    }
    
    @Override
    public IStateTransferServer createServer()
    {
        return new ClusterMembershipStateTransferServer();
    }

    @Override
    public IStateTransferClient createClient()
    {
        return new ClusterMembershipStateTransferClient();
    }
    
    private class ClusterMembershipStateTransferServer implements IStateTransferServer
    {
        @Override
        public MessageType classifyMessage(IMessage message)
        {
            if (message.getDestination().equals(Memberships.CORE_GROUP_ADDRESS))
                return MessageType.STATE_WRITE;
            else
                return MessageType.NON_STATE;
        }

        @Override
        public void saveSnapshot(File file)
        {
            SyncCompletionHandler handler = new SyncCompletionHandler(new Callable()
            {
                @Override
                public Object call() throws Exception
                {
                    return membershipManager.getMembership();
                }
            });
            
            compartment.offer(handler);
            
            ClusterMembership membership = handler.await();
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
            
            Files.writeBytes(file, new ByteArray(stream.getBuffer(), 0, stream.getLength()));
        }
    }
    
    private class ClusterMembershipStateTransferClient implements IStateTransferClient
    {
        @Override
        public void loadSnapshot(File file)
        {
            ByteArray buffer = Files.readBytes(file);
            
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
            final IClusterMembership membership = new ClusterMembership(delta.getId(), domains);
            
            SyncCompletionHandler handler = new SyncCompletionHandler(new Callable()
            {
                @Override
                public Object call() throws Exception
                {
                    membershipManager.installMembership(membership);
                    return null;
                }
            });
            
            compartment.offer(handler);
            
            handler.await();
        }
    }
}

