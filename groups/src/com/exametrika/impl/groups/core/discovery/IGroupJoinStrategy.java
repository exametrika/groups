/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.List;

import com.exametrika.common.messaging.IAddress;

/**
 * The {@link IGroupJoinStrategy} is used to customize process of joining new node to the group.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupJoinStrategy
{
    /**
     * Called on joining node when membership response has been received from one of the group members. 
     * Strategy must send {@link GroupJoinMessagePart} to current group coordinator in order to join the group.
     *
     * @param healthyMembers list of healthy group members. First member is current group coordinator
     */
    void onGroupDiscovered(List<IAddress> healthyMembers);
    
    /**
     * Called on joining node when all known healthy group members have failed.
     */
    void onGroupFailed();
}
