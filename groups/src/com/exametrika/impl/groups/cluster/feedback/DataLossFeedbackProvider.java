/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.IGroupState.State;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;
import com.exametrika.spi.groups.cluster.state.IDataLossObserver;

/**
 * The {@link DataLossFeedbackProvider} is a data loss feedback provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class DataLossFeedbackProvider implements IFeedbackProvider, IDataLossFeedbackService
{
    public static final UUID ID = UUID.fromString("091e054f-6cad-4312-b9e6-bb908d73ad43");
    private final Map<UUID, DataLossInfo> dataLossInfos = new LinkedHashMap<UUID, DataLossInfo>();
    private final IDataLossObserver dataLossObserver;
    private final IClusterMembershipService membershipService;
    private final IGroupFeedbackService groupFeedbackService;
    
    public DataLossFeedbackProvider()
    {
        this.dataLossObserver = null;
        this.membershipService = null;
        this.groupFeedbackService = null;
    }
    
    public DataLossFeedbackProvider(IDataLossObserver dataLossObserver, IClusterMembershipService membershipService,
        IGroupFeedbackService groupFeedbackService)
    {
        this.dataLossObserver = dataLossObserver;
        this.membershipService = membershipService;
        this.groupFeedbackService = groupFeedbackService;
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new DataLossFeedbackDataSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(DataLossFeedbackDataSerializer.ID);
    }

    @Override
    public UUID getId()
    {
        return ID;
    }

    @Override
    public IExchangeData getData(boolean force)
    {
        List<IDataLossState> states = null;
        for (Map.Entry<UUID, DataLossInfo> entry : dataLossInfos.entrySet())
        {
            if (force || entry.getValue().modified)
            {
                if (states == null)
                    states = new ArrayList<IDataLossState>();
                
                states.add(entry.getValue().state);
            }
        }
        
        if (states != null)
            return new DataLossFeedbackData(states);
        else
            return null;
    }

    @Override
    public void setData(IExchangeData data)
    {
        Assert.notNull(data);
        
        DataLossFeedbackData feedbackData = (DataLossFeedbackData)data;
        for (IDataLossState state : feedbackData.getStates())
            updateDataLossState(state);
    }

    @Override
    public void onClusterMembershipChanged(IClusterMembership membership)
    {
        Assert.notNull(membership);
        
        for (Iterator<Map.Entry<UUID, DataLossInfo>> it = dataLossInfos.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<UUID, DataLossInfo> entry = it.next();
            IDataLossState state = entry.getValue().state;
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
    public void updateDataLossState(IDataLossState state)
    {
        Assert.notNull(state);
        
        DataLossInfo info = new DataLossInfo();
        info.state = state;
        info.modified = true;
        
        dataLossInfos.put(state.getId(), info);
        
        if (dataLossObserver != null)
        {
            IClusterMembership membership = membershipService.getMembership();
            if (membership != null)
            {
                IDomainMembership domainMembership = membership.findDomain(state.getDomain());
                if (domainMembership != null)
                {
                    GroupsMembership groupsMembership = domainMembership.findElement(GroupsMembership.class);
                    IGroup group = groupsMembership.findGroup(state.getId());
                    if (group != null)
                    {
                        IGroupState groupState = groupFeedbackService.findGroupState(group.getId());
                        if (groupState != null && (groupState.getMembershipId() > 1 || groupState.getState() == State.NORMAL))
                            dataLossObserver.onDataLoss(group);
                    }
                }
            }
        }
    }
    
    private static class DataLossInfo
    {
        private IDataLossState state;
        private boolean modified;
    }
}
