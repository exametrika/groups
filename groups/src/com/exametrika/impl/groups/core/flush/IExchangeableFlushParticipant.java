/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.Map;

import com.exametrika.api.groups.core.INode;
import com.exametrika.impl.groups.core.exchange.IExchangeData;


/**
 * The {@link IExchangeableFlushParticipant} is a flush participant which supports data exchange on message stabilization flush phase.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IExchangeableFlushParticipant extends IFlushParticipant
{
    /**
     * Returns local exchange data.
     *
     * @return returns local exchange data or null if data are absent.
     */
    IExchangeData getLocalData();
    
    /**
     * Sets remote exchange data from healthy nodes of prepared new membership.
     *
     * @param data exchange data from remote nodes from healthy nodes of prepared new membership.
     * Exchange data can be null.
     */
    void setRemoteData(Map<INode, IExchangeData> data);
}
