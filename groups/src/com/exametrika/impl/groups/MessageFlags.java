/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups;



/**
 * The {@link MessageFlags} defines different message flags, used in communication protocols.
 * 
 * @author medvedev
 */
public interface MessageFlags extends com.exametrika.common.messaging.impl.MessageFlags
{
    /** Message is a node shunning notification.*/
    int SHUN = 1 << 20;
    
    /** Message is a state transfer request.*/
    int STATE_TRANSFER_REQUEST = 1 << 21;
    
    /** Message does not preserve total order. */
    int UNORDERED = 1 << 22;
    
    /** Message is a worker group discovery request. */
    int GROUP_DISCOVERY_REQUEST = 1 << 23;
    
    /** Message is a group state hash request. */
    int STATE_HASH_REQUEST = 1 << 24;
}
