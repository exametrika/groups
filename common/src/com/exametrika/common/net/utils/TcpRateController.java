/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.util.ArrayDeque;
import java.util.Deque;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpRateController;
import com.exametrika.common.net.ITcpRateControllerFactory;
import com.exametrika.common.utils.Assert;




/**
 * The {@link TcpRateController} is a channel rate controller.
 *
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class TcpRateController implements ITcpRateController, ITcpRateControllerFactory
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger = Loggers.get(TcpRateController.class);
    private final boolean lockRead;
    private final int maxReadUnlockRate;
    private final int minReadLockRate;
    private final boolean lockWrite;
    private final int maxWriteUnlockRate;
    private final int minWriteLockRate;
    private final boolean totalControl;
    private final long trackPeriod;
    private final int pointCount;
    private volatile boolean readLocked;
    private volatile boolean writeLocked;
    private long startTime;
    private long readCount;
    private long writeCount;
    private ITcpChannel channel;
    private boolean disabled;
    private double readRateSum;
    private double writeRateSum;
    private Deque<Double> readRates = new ArrayDeque<Double>();
    private Deque<Double> writeRates = new ArrayDeque<Double>();

    /**
     * Creates a new object.
     *
     * @param lockRead if true read rate control is applied
     * @param maxReadUnlockRate maximal read rate in bytes per second which unlocks channel when it is locked
     * @param minReadLockRate minimal read rate in bytes per second which locks channel when it is unlocked
     * @param lockWrite if true write rate control is applied
     * @param maxWriteUnlockRate maximal write rate in bytes per second which unlocks channel when it is locked
     * @param minWriteLockRate minimal write rate in bytes per second which locks channel when it is unlocked
     * @param totalControl if true all rates are total rates for all currently connected channels, if false all rates are for single channel
     * @param trackPeriod period in milliseconds of updating rate statistics
     * @param pointCount number of points of rate statistics
     */
    public TcpRateController(boolean lockRead, int maxReadUnlockRate, int minReadLockRate, boolean lockWrite, 
        int maxWriteUnlockRate, int minWriteLockRate, boolean totalControl, long trackPeriod, int pointCount)
    {
        this(lockRead, maxReadUnlockRate, minReadLockRate, lockWrite, maxWriteUnlockRate, minWriteLockRate, totalControl, 
            trackPeriod, pointCount, null);
    }
    
    @Override
    public ITcpRateController createController(ITcpChannel channel)
    {
        Assert.notNull(channel);
        
        return new TcpRateController(lockRead, maxReadUnlockRate, minReadLockRate, lockWrite, maxWriteUnlockRate, 
            minWriteLockRate, totalControl, trackPeriod, pointCount, channel);
    }

    @Override
    public void onTimer(long currentTime)
    {
        if (disabled)
            return;
        
        if (startTime != 0)
        {
            if (currentTime - startTime < trackPeriod)
                return;
            
            Assert.checkState(channel != null);
            if (channel.isAdmin())
            {
                disabled = true;
                return;
            }
            
            int channelCount = 1;
            if (totalControl)
            {
                channelCount = channel.getDispatcher().getChannelCount();
                if (channelCount == 0)
                    return;
            }
            
            if (lockRead)
            {
                double currentReadRate = readCount * channelCount * 1000 / (currentTime - startTime);
                readRates.offer(currentReadRate);
                double lastReadRate = readRates.poll();
                
                readRateSum += currentReadRate;
                readRateSum -= lastReadRate;
                
                double readRate = readRateSum / pointCount;
                
                if (readLocked)
                {
                    if (readRate <= maxReadUnlockRate)
                    {
                        readLocked = false;
                        channel.updateReadStatus();
                        
                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.channelReadUnlocked(readRate / channelCount));
                    }
                }
                else
                {
                    if (readRate >= minReadLockRate)
                    {
                        readLocked = true;
                        channel.updateReadStatus();
                        
                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.channelReadLocked(readRate / channelCount));
                    }
                }
                
                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, channel.getMarker(), messages.channelReadRate(readRate / channelCount));
            }
            
            if (lockWrite)
            {
                double currentWriteRate = writeCount * channelCount * 1000 / (currentTime - startTime);
                writeRates.offer(currentWriteRate);
                double lastWriteRate = writeRates.poll();
                
                writeRateSum += currentWriteRate;
                writeRateSum -= lastWriteRate;
                
                double writeRate = writeRateSum / pointCount;
                
                if (writeLocked)
                {
                    if (writeRate <= maxWriteUnlockRate)
                    {
                        writeLocked = false;
                        channel.updateWriteStatus();
                        
                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.channelWriteUnlocked(writeRate / channelCount));
                    }
                }
                else
                {
                    if (writeRate >= minWriteLockRate)
                    {
                        writeLocked = true;
                        channel.updateWriteStatus();
                        
                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.channelWriteLocked(writeRate / channelCount));
                    }
                }
                
                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, channel.getMarker(), messages.channelWriteRate(writeRate / channelCount));
            }
        }
                
        startTime = currentTime;
        readCount = 0;
        writeCount = 0;
    }

    @Override
    public boolean canRead()
    {
        return !readLocked;
    }
    
    @Override
    public boolean canWrite()
    {
        return !writeLocked;
    }

    @Override
    public void incrementReadCount(long readCount)
    {
        this.readCount += readCount;
    }
    
    @Override
    public void incrementWriteCount(long writeCount)
    {
        this.writeCount += writeCount;
    }
    
    private TcpRateController(boolean lockRead, int maxReadUnlockRate, int minReadLockRate, boolean lockWrite, 
        int maxWriteUnlockRate, int minWriteLockRate, boolean totalControl, long trackPeriod, int pointCount, ITcpChannel channel)
    {
        Assert.isTrue(pointCount > 0);
        
        this.lockRead = lockRead;
        this.maxReadUnlockRate = maxReadUnlockRate;
        this.minReadLockRate = minReadLockRate;
        this.lockWrite = lockWrite;
        this.maxWriteUnlockRate = maxWriteUnlockRate;
        this.minWriteLockRate = minWriteLockRate;
        this.totalControl = totalControl;
        this.trackPeriod = trackPeriod;
        this.pointCount = pointCount;
        
        for (int i = 0; i < pointCount; i++)
        {
            readRates.offer(0.0);
            writeRates.offer(0.0);
        }
        
        if (channel != null)
        {
            this.channel = channel;
            
            if (channel.isAdmin())
                disabled = true;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Channel read locked at rate {0}.")
        ILocalizedMessage channelReadLocked(double d);
        @DefaultMessage("Channel read unlocked at rate {0}.")
        ILocalizedMessage channelReadUnlocked(double d);
        @DefaultMessage("Channel read rate {0}.")
        ILocalizedMessage channelReadRate(double d);
        @DefaultMessage("Channel write locked at rate {0}.")
        ILocalizedMessage channelWriteLocked(double d);
        @DefaultMessage("Channel write unlocked at rate {0}.")
        ILocalizedMessage channelWriteUnlocked(double d);
        @DefaultMessage("Channel write rate {0}.")
        ILocalizedMessage channelWriteRate(double d);
    }
}
