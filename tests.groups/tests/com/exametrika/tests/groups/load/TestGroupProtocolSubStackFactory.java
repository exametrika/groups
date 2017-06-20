/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.WorkerNodeFactoryParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.impl.groups.cluster.channel.GroupProtocolSubStackFactory;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;

public class TestGroupProtocolSubStackFactory extends GroupProtocolSubStackFactory
{
    public TestGroupProtocolSubStackFactory(String channelName, IMessageFactory messageFactory,
        LocalNodeProvider localNodeProvider, IClusterMembershipService clusterMembershipService,
        ISerializationRegistry serializationRegistry, WorkerNodeFactoryParameters factoryParameters,
        WorkerNodeParameters parameters)
    {
        super(channelName, messageFactory, localNodeProvider, clusterMembershipService, serializationRegistry,
            factoryParameters, parameters);
    }

    @Override
    protected void createProtocols(WorkerGroupFailureDetectionProtocol failureDetectionProtocol,
        List<AbstractProtocol> protocols, List<IFlushParticipant> flushParticipants)
    {
        TestWorkerNodeParameters parameters = (TestWorkerNodeParameters)this.parameters;
        TestWorkerNodeFactoryParameters factoryParameters = (TestWorkerNodeFactoryParameters)this.factoryParameters;
        TestGroupFailureGenerationProtocol failureGenerationProtocol = new TestGroupFailureGenerationProtocol(channelName, messageFactory,
            parameters.failureSpecs, factoryParameters.failureGenerationProcessPeriod, false);
        failureGenerationProtocol.setFailureDetector(failureDetectionProtocol);
        flushParticipants.add(failureGenerationProtocol);
        protocols.add(failureGenerationProtocol);
    }
}
