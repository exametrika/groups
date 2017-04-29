/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.channel;

import com.exametrika.api.groups.cluster.NodeParameters;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.impl.NoDeliveryHandler;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

/**
 * The {@link SimGroupParameters} is a test group parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class SimGroupParameters extends NodeParameters
{
    public boolean asyncStateTransfer = true;
    public IStateTransferFactory stateTransferFactory;
    public IDeliveryHandler deliveryHandler = new NoDeliveryHandler();
    public IChannelReconnector channelReconnector;
}