/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;



/**
 * The {@link MessageFlags} defines different message flags, used in communication protocols.
 * 
 * @author medvedev
 */
public interface MessageFlags
{
    /** Message must be sent with high priority if possible. */
    int HIGH_PRIORITY = 1 << 0;
    
    /** Message can be send with low priority. */
    int LOW_PRIORITY = 1 << 1;
    
    /** Message can be delivered in parallel with other messages from the same sender. */
    int PARALLEL = 1 << 2;
    
    /** In order to minimize latency message transmission can not be delayed. Using this flag significantly reduces throughput. */
    int NO_DELAY = 1 << 3;
    
    /** Message can not be compressed. */
    int NO_COMPRESS = 1 << 4;
}
