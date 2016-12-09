/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link IChannel} represents a message-oriented communication channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IChannel extends ISender, IBulkSender, IPullableSender, IConnectionProvider, ILifecycle
{
    /**
     * Returns compartment.
     *
     * @return compartment
     */
    ICompartment getCompartment();
    
    /**
     * Returns channel message factory.
     *
     * @return channel message factory
     */
    IMessageFactory getMessageFactory();
    
    /**
     * Returns live node provider.
     *
     * @return live node provider
     */
    ILiveNodeProvider getLiveNodeProvider();
    
    /**
     * Returns channel observer.
     *
     * @return channel observer
     */
    IChannelObserver getChannelObserver();
    
    /**
     * Returns flow controller which is used to control incoming message traffic. Flow controller are used in single threaded
     * mode only. Flow control must be performed from {@link IReceiver#receive(IMessage)} call.
     *
     * @return flow controller
     */
    IFlowController<IAddress> getFlowController();
}
