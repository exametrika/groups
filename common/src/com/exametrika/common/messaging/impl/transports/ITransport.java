/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link ITransport} represents a transport.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITransport extends ISender, IPullableSender, ILifecycle, IFlowController<IAddress>
{
    /**
     * Returns local node transport address. Must be called after transport has been started.
     *
     * @return local node transport address
     */
    IAddress getLocalNode();
}
