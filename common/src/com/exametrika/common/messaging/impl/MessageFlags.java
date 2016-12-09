/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;



/**
 * The {@link MessageFlags} defines different message flags, used in communication protocols.
 * 
 * @author medvedev
 */
public interface MessageFlags extends com.exametrika.common.messaging.MessageFlags
{
    /** Message is a heartbeat request.*/
    int HEARTBEAT_REQUEST = 1 << 10;
    
    /** Message is a heartbeat response.*/
    int HEARTBEAT_RESPONSE = 1 << 11;
}
