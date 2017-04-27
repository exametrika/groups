/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.impl.NoDeliveryHandler;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

/**
 * The {@link WorkerNodeParameters} is a worker node parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class WorkerNodeParameters extends NodeParameters
{
    public IStateTransferFactory stateTransferFactory;
    public IDeliveryHandler deliveryHandler = new NoDeliveryHandler();
}