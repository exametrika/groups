/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

/**
 * The {@link WorkerNodeFactoryParameters} is a worker node factory parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class WorkerNodeFactoryParameters extends NodeFactoryParameters
{
    public long nodeOrphanPeriod = 600000;
    public int maxSubStackPendingMessageCount = 1000;
    public long groupSubStackRemoveDelay = 300000;
    public int maxGroupMembershipHistorySize = 10;
    
    public WorkerNodeFactoryParameters()
    {
        super(false);
    }
    
    public WorkerNodeFactoryParameters(boolean debug)
    {
        super(debug);
    }
}