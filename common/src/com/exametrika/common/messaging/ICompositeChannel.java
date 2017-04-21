/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link ICompositeChannel} represents a message-oriented composite communication channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompositeChannel extends ISender, IBulkSender, IPullableSender, IConnectionProvider, ILifecycle
{
    /**
     * Returns list of sub-channels.
     *
     * @return list of sub-channels
     */
    List<IChannel> getSubChannels();
    
    /**
     * Returns main sub-channel.
     *
     * @return main sub-channel
     */
    IChannel getMainSubChannel();
    
    /**
     * Returns compartment.
     *
     * @return compartment
     */
    ICompartment getCompartment();
    
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
}
