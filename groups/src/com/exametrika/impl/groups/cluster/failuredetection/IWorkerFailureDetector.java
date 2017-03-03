/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.Set;

import com.exametrika.api.groups.core.INode;



/**
 * The {@link IWorkerFailureDetector} is a workerbnode failure detector.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IWorkerFailureDetector
{
    /**
     * Returns list of group members of current membership, whose failure has been detected.
     *
     * @return list of failed members
     */
    Set<INode> getFailedMembers();
    
    /**
     * Returns list of group members of current membership, which intentionally left the group.
     *
     * @return list of left members
     */
    Set<INode> getLeftMembers();
}
