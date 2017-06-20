/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import com.exametrika.api.groups.cluster.WorkerNodeFactoryParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.impl.groups.cluster.channel.WorkerGroupSubChannelFactory;
import com.exametrika.impl.groups.cluster.membership.IGroupProtocolSubStackFactory;

public class TestWorkerGroupSubChannelFactory extends WorkerGroupSubChannelFactory
{
    public TestWorkerGroupSubChannelFactory(TestWorkerNodeFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    @Override
    protected IGroupProtocolSubStackFactory createGroupProtocolSubStackFactory(IMessageFactory messageFactory,
        String channelName, ISerializationRegistry serializationRegistry, WorkerNodeParameters nodeParameters,
        WorkerNodeFactoryParameters nodeFactoryParameters)
    {
        return new TestGroupProtocolSubStackFactory(channelName, messageFactory, localNodeProvider, 
            clusterMembershipManager, serializationRegistry,
            nodeFactoryParameters, nodeParameters);
    }
}
