/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.impl.NoFlowController;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;
import com.exametrika.spi.groups.cluster.channel.IPropertyProvider;
import com.exametrika.spi.groups.cluster.channel.SystemPropertyProvider;
import com.exametrika.spi.groups.cluster.discovery.IDiscoveryStrategy;
import com.exametrika.spi.groups.cluster.state.IDataLossObserver;

/**
 * The {@link NodeParameters} is a node parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class NodeParameters extends ChannelParameters
{
    public IPropertyProvider propertyProvider = new SystemPropertyProvider();
    public IDiscoveryStrategy discoveryStrategy;
    public IFlowController<RemoteFlowId> localFlowController = new NoFlowController<RemoteFlowId>();
    public IDataLossObserver dataLossObserver;
    public IChannelReconnector channelReconnector;
}