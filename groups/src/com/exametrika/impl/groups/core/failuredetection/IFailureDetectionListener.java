/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.failuredetection;

import com.exametrika.api.groups.core.INode;


/**
 * The {@link IFailureDetectionListener} is used to notify about detected failures.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFailureDetectionListener
{
    /**
     * Called if failure has been detected for specified member.
     * 
     * @param member group member whose failure has been detected
     */
    void onMemberFailed(INode member);
    
    /**
     * Called if specified member intentionally has left the group.
     * 
     * @param member left group member
     */
    void onMemberLeft(INode member);
}
