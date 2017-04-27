/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.NoDeliveryHandler;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.impl.NoFlowController;
import com.exametrika.impl.groups.cluster.feedback.IDataLossObserver;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.spi.groups.IDiscoveryStrategy;
import com.exametrika.spi.groups.IPropertyProvider;
import com.exametrika.spi.groups.SystemPropertyProvider;

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
    public IDeliveryHandler deliveryHandler = new NoDeliveryHandler();
    public IFlowController<RemoteFlowId> localFlowController = new NoFlowController<RemoteFlowId>();
    public IDataLossObserver dataLossObserver;
}