/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;

/**
 * The {@link AbstractClusterMembershipProtocol} represents an abstract cluster membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractClusterMembershipProtocol extends AbstractProtocol
{
    protected final IClusterMembershipManager clusterMembershipManager;
    protected final List<IClusterMembershipProvider> membershipProviders;

    public AbstractClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipManager clusterMembershipManager,
        List<IClusterMembershipProvider> membershipProviders)
    {
        super(channelName, messageFactory);

        Assert.notNull(clusterMembershipManager);
        Assert.notNull(membershipProviders);

        this.clusterMembershipManager = clusterMembershipManager;
        this.membershipProviders = membershipProviders;
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new ClusterMembershipSerializationRegistrar());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(new ClusterMembershipSerializationRegistrar());
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof ClusterMembershipMessagePart)
        {
            ClusterMembershipMessagePart part = message.getPart();
            IClusterMembership oldMembership = clusterMembershipManager.getMembership();
            if (oldMembership == null)
                Assert.isTrue(part.getDelta().isFull());
            else
            {
                if (part.getDelta().getId() <= oldMembership.getId())
                    return;
                
                if (!part.getDelta().isFull())
                    Assert.isTrue(part.getDelta().getId() == oldMembership.getId() + 1);
            }
            
            Assert.isTrue(part.getDelta().getDeltas().size() == membershipProviders.size());
            
            List<IClusterMembershipElement> elements = new ArrayList<IClusterMembershipElement>();
            List<IClusterMembershipElementChange> changes = new ArrayList<IClusterMembershipElementChange>();
            for (int i= 0; i < membershipProviders.size(); i++)
            {
                IClusterMembershipElement element = membershipProviders.get(i).createMembership(part.getDelta().getDeltas().get(i),
                    !part.getDelta().isFull() ? oldMembership.getElements().get(i) : null);
                elements.add(element);
                
                if (oldMembership != null)
                {
                    IClusterMembershipElementChange change;
                    if (!part.getDelta().isFull())
                        change = membershipProviders.get(i).createChange(part.getDelta().getDeltas().get(i),
                            oldMembership.getElements().get(i));
                    else
                        change = membershipProviders.get(i).createChange(element, oldMembership.getElements().get(i));
                    
                    changes.add(change);
                }
            }
            
            IClusterMembership newMembership = new ClusterMembership(part.getDelta().getId(), elements);
            if (oldMembership == null)
                clusterMembershipManager.installMembership(newMembership);
            else
                clusterMembershipManager.changeMembership(newMembership, new ClusterMembershipChange(changes));
            
            onInstalled(newMembership);
        }
        else
            receiver.receive(message);
    }
    
    protected void onInstalled(IClusterMembership newMembership)
    {
    }
}
