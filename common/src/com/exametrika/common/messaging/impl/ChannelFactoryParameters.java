/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

/**
 * The {@link ChannelFactoryParameters} is a channel factory parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class ChannelFactoryParameters
{
    public long selectionPeriod = 100;
    public long cleanupPeriod = 1000;
    public long nodeCleanupPeriod = 30000;
    public int compressionLevel = 5;
    public int streamingMaxFragmentSize = 10000; 
    public long heartbeatTrackPeriod = 500;
    public long heartbeatStartPeriod = 10000;
    public long heartbeatPeriod = 1000;
    public long heartbeatFailureDetectionPeriod = 60000;
    public long transportChannelTimeout = 10000;
    public long transportMaxChannelIdlePeriod = 600000;
    public Integer transportReceiveThreadCount = null;
    public int transportMaxUnlockSendQueueCapacity = 700000;
    public int transportMinLockSendQueueCapacity = 1000000;
    public int transportMaxPacketSize = 1000000;
    public long transportMinReconnectPeriod = 60000;
    public int compartmentMaxUnlockQueueCapacity = 7000000;
    public int compartmentMinLockQueueCapacity = 10000000;
    public int maxBundlingMessageSize = 10000;
    public int maxBundlingPeriod = 100;
    public int maxBundleSize = 1000000;
    public boolean receiveMessageList = false;
    public int sendQueueIdlePeriod = 600000;
    
    public ChannelFactoryParameters()
    {
        this(false);
    }
    
    public ChannelFactoryParameters(boolean debug)
    {
        int timeMultiplier = !debug ? 1 : 1000; 
        heartbeatFailureDetectionPeriod *= timeMultiplier;
        heartbeatStartPeriod *= timeMultiplier;
        transportChannelTimeout *= timeMultiplier;
        transportMaxChannelIdlePeriod *= timeMultiplier;
    }
}