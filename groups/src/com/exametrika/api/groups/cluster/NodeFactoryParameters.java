/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.messaging.impl.ChannelFactoryParameters;

/**
 * The {@link NodeFactoryParameters} is a node factory parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class NodeFactoryParameters extends ChannelFactoryParameters
{
    public long discoveryPeriod = 500;
    public long groupFormationPeriod = 10000;
    public long failureUpdatePeriod = 500;
    public long failureHistoryPeriod = 600000;
    public int maxShunCount = 3;
    public long flushTimeout = 300000;
    public long membershipTrackPeriod = 1000;
    public long gracefulExitTimeout = 10000;
    public long maxStateTransferPeriod = Integer.MAX_VALUE;
    public long stateSizeThreshold = 100000;// TODO: прописать остальные значения по умолчанию
    public long saveSnapshotPeriod = 1000;
    public long transferLogRecordPeriod = 1000;
    public int transferLogMessagesCount = 2;
    public int minLockQueueCapacity = 10000000;
    public int maxUnlockQueueCapacity = 100000;
    public long dataExchangePeriod = 200;
    public int maxBundlingMessageSize = 10000;
    public long maxBundlingPeriod = 100;
    public int maxBundleSize = 10000;
    public int maxTotalOrderBundlingMessageCount = 10;
    public long maxUnacknowledgedPeriod = 100;
    public int maxUnacknowledgedMessageCount = 100;
    public long maxIdleReceiveQueuePeriod = 600000;
    public long checkStatePeriod = 600000;
    
    public NodeFactoryParameters()
    {
        super(false);
    }
    
    public NodeFactoryParameters(boolean debug)
    {
        super(debug);
        
        int timeMultiplier = !debug ? 1 : 1000;
        flushTimeout *= timeMultiplier;
        gracefulExitTimeout *= timeMultiplier;
    }
}