/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;

/**
 * The {@link CleanupManager} represents a cleanup manager.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CleanupManager implements ICleanupManager
{
    private final List<AbstractProtocol> protocols;
    private final ILiveNodeProvider liveNodeProvider;
    private final long cleanupPeriod;
    private final long nodeCleanupPeriod;
    private ITimeService timeService;
    private long lastCleanupTime;
    private final Map<IAddress, CleanupInfo> cleanupInfos = new LinkedHashMap<IAddress, CleanupInfo>();

    public CleanupManager(List<AbstractProtocol> protocols, ILiveNodeProvider liveNodeProvider,
        long cleanupPeriod, long nodeCleanupPeriod)
    {
        Assert.notNull(protocols);
        Assert.notNull(liveNodeProvider);
        
        this.protocols = protocols;
        this.liveNodeProvider = liveNodeProvider;
        this.cleanupPeriod = cleanupPeriod;
        this.nodeCleanupPeriod = nodeCleanupPeriod;
    }
    
    public void setTimeService(ITimeService timeService)
    {
        Assert.notNull(timeService);
        Assert.isNull(this.timeService);
        
        this.timeService = timeService;
    }
    
    public void onTimer(long currentTime)
    {
        if (lastCleanupTime != 0 && currentTime - lastCleanupTime < cleanupPeriod)
            return;
        
        lastCleanupTime = currentTime;
        
        for (AbstractProtocol protocol : protocols)
            protocol.cleanup(this, liveNodeProvider, currentTime);
        
        for (Iterator<Map.Entry<IAddress, CleanupInfo>> it = cleanupInfos.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<IAddress, CleanupInfo> entry = it.next();
            if (currentTime > entry.getValue().startTime + nodeCleanupPeriod)
                it.remove();
            else
                break;
        }
    }

    @Override
    public boolean canCleanup(IAddress node)
    {
        Assert.isInstanceOf(UnicastAddress.class, node);
        
        if (liveNodeProvider.isLive(node))
        {
            cleanupInfos.remove(node);
            return false;
        }
        else
        {
            long currentTime = timeService.getCurrentTime();
            
            CleanupInfo info = cleanupInfos.get(node);
            if (info == null)
            {
                info = new CleanupInfo(currentTime);
                cleanupInfos.put(node, info);
            }
            
            if (currentTime > info.startTime + nodeCleanupPeriod)
                return true;
            else
                return false;
        }
    }
    
    private static class CleanupInfo
    {
        private final long startTime;
        
        public CleanupInfo(long startTime)
        {
            this.startTime = startTime;
        }
    }
}
