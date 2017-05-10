/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IDomainMembershipChange;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessageFactory;
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

    protected void installMembership(ClusterMembershipMessagePart part)
    {
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
        
        List<IDomainMembership> domains = new ArrayList<IDomainMembership>();
        Set<String> domainNames = new HashSet<String>();
        List<IDomainMembershipChange> domainsChanges = new ArrayList<IDomainMembershipChange>();
        List<IDomainMembership> newDomains = new ArrayList<IDomainMembership>();
        for (IDomainMembershipDelta domainDelta : part.getDelta().getDomains())
        {
            Assert.isTrue(domainDelta.getDeltas().size() == membershipProviders.size());
            IDomainMembership oldDomain = null;
            if (oldMembership != null)
                oldDomain = oldMembership.findDomain(domainDelta.getName());
            List<IClusterMembershipElement> elements = new ArrayList<IClusterMembershipElement>();
            List<IClusterMembershipElementChange> changes = new ArrayList<IClusterMembershipElementChange>();
            IDomainMembership domain = new DomainMembership(domainDelta.getName(), elements);
            domains.add(domain);
            domainNames.add(domain.getName());
            
            for (int i = 0; i < membershipProviders.size(); i++)
            {
                IClusterMembershipElement element = membershipProviders.get(i).createMembership(domain, domainDelta.getDeltas().get(i),
                    (oldDomain != null && !part.getDelta().isFull()) ? oldDomain.getElements().get(i) : null);
                elements.add(element);
                
                if (oldDomain != null)
                {
                    IClusterMembershipElementChange change;
                    if (!part.getDelta().isFull())
                        change = membershipProviders.get(i).createChange(domain, domainDelta.getDeltas().get(i),
                            oldDomain.getElements().get(i));
                    else
                        change = membershipProviders.get(i).createChange(domain, element, oldDomain.getElements().get(i));
                    
                    changes.add(change);
                }
            }
            
            if (oldDomain != null)
            {
                IDomainMembershipChange domainChange = new DomainMembershipChange(domainDelta.getName(), changes);
                domainsChanges.add(domainChange);
            }
            else
                newDomains.add(domain);
        }
        
        Set<IDomainMembership> removedDomains = new LinkedHashSet<IDomainMembership>();
        if (oldMembership != null)
        {
            for (IDomainMembership oldDomain : oldMembership.getDomains())
            {
                if (!domainNames.contains(oldDomain.getName()))
                {
                    boolean empty = true;
                    for (int i = 0; i < membershipProviders.size(); i++)
                    {
                        if (!membershipProviders.get(i).isEmptyMembership(oldDomain.getElements().get(i)))
                        {
                            empty = false;
                            break;
                        }
                    }
                    
                    if (!empty)
                        domains.add(oldDomain);
                    else
                        removedDomains.add(oldDomain);
                }
            }
        }
        
        IClusterMembership newMembership = new ClusterMembership(part.getDelta().getId(), domains);
        if (oldMembership == null)
            clusterMembershipManager.installMembership(newMembership);
        else
            clusterMembershipManager.changeMembership(newMembership, new ClusterMembershipChange(newDomains, domainsChanges, removedDomains));
        
        onInstalled(part.getRoundId(), newMembership, part.getDelta());
    }
    
    protected void onInstalled(long roundId, IClusterMembership newMembership, ClusterMembershipDelta coreDelta)
    {
    }
}
