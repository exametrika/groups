/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.trace;

import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;

/**
 * The {@link TracingProtocol} represents a protocol that traces incoming and outgoing messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TracingProtocol extends AbstractProtocol
{
    public TracingProtocol(String channelName, IMessageFactory messageFactory)
    {
        this(channelName, null, messageFactory);
    }
    
    public TracingProtocol(String channelName, String loggerName, IMessageFactory messageFactory)
    {
        super(channelName, loggerName, messageFactory);
    }
}
