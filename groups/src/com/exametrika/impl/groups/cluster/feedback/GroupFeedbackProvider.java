/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackProvider;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;

/**
 * The {@link GroupFeedbackProvider} is a group feedback provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupFeedbackProvider implements IFeedbackProvider, IGroupFeedbackService
{
    public static final UUID ID = UUID.fromString("6fc5d415-96d0-4f51-bb61-89f7e5f1ac29");
    private final Map<UUID, GroupInfo> groupInfos = new LinkedHashMap<UUID, GroupInfo>();
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new GroupFeedbackDataSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(GroupFeedbackDataSerializer.ID);
    }

    @Override
    public UUID getId()
    {
        return ID;
    }

    @Override
    public IExchangeData getData(boolean force)
    {
        List<IGroupState> states = null;
        for (Map.Entry<UUID, GroupInfo> entry : groupInfos.entrySet())
        {
            if (force || entry.getValue().modified)
            {
                if (states == null)
                    states = new ArrayList<IGroupState>();
                
                states.add(entry.getValue().state);
            }
        }
        
        if (states != null)
            return new GroupFeedbackData(states);
        else
            return null;
    }

    @Override
    public void setData(IExchangeData data)
    {
        Assert.notNull(data);
        
        GroupFeedbackData feedbackData = (GroupFeedbackData)data;
        for (IGroupState state : feedbackData.getStates())
            updateGroupState(state);
    }

    @Override
    public void onClusterMembershipChanged(IClusterMembership membership)
    {
        Assert.notNull(membership);
        
        for (Iterator<Map.Entry<UUID, GroupInfo>> it = groupInfos.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<UUID, GroupInfo> entry = it.next();
            IGroupState state = entry.getValue().state;
            IDomainMembership domainMembership = membership.findDomain(state.getDomain());
            if (domainMembership != null)
            {
                GroupsMembership groupsMembership = domainMembership.findElement(GroupsMembership.class);
                if (groupsMembership.findGroup(state.getId()) != null)
                    continue;
            }
            
            it.remove();
        }
    }

    @Override
    public Set<IGroupState> getGroupStates()
    {
        Set<IGroupState> states = new LinkedHashSet<IGroupState>();
        for (GroupInfo info : groupInfos.values())
            states.add(info.state);
        
        return states;
    }

    @Override
    public IGroupState findGroupState(UUID id)
    {
        Assert.notNull(id);
        
        GroupInfo info = groupInfos.get(id);
        if (info != null)
            return info.state;
        else
            return null;
    }

    @Override
    public void updateGroupState(IGroupState state)
    {
        Assert.notNull(state);
        
        GroupInfo info = new GroupInfo();
        info.state = state;
        info.modified = true;
        
        groupInfos.put(state.getId(), info);
    }
    
    private static class GroupInfo
    {
        private IGroupState state;
        private boolean modified;
    }
}
