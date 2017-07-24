/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.spi.groups.cluster.state.ISimpleStateStore;

/**
 * The {@link CoreNodeParameters} is a core node parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class CoreNodeParameters extends NodeParameters
{
    public ISimpleStateStore stateStore;
    public ChannelParameters coreToWorkerChannelParameters = new ChannelParameters();
}