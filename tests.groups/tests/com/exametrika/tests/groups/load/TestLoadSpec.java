/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.text.MessageFormat;

import com.exametrika.common.utils.Assert;

public final class TestLoadSpec
{
    private final SizeType messageSizeType;
    private final int messageSize;
    private final SizeType stateSizeType;
    private final int stateSize;
    private final SendFrequencyType sendFrequencyType;
    private final double sendFrequency;
    private final SendType sendType;
    private final SendSourceType sendSourceType;
    
    public enum SizeType
    {
        SET,
        SMALL,
        MEDIUM,
        LARGE,
    }
    
    public enum SendFrequencyType
    {
        SET,
        MAXIMUM
    }
    
    public enum SendType
    {
        DIRECT,
        PULLABLE,
    }
    
    public enum SendSourceType
    {
        SINGLE_NODE,
        ALL_NODES
    }

    public TestLoadSpec(SizeType messageSizeType, int messageSize, SizeType stateSizeType, int stateSize, 
        SendFrequencyType sendFrequencyType, double sendFrequency, SendType sendType, SendSourceType sendSourceType)
    {
        Assert.notNull(messageSizeType);
        Assert.notNull(stateSizeType);
        Assert.notNull(sendFrequencyType);
        Assert.notNull(sendType);
        Assert.notNull(sendSourceType);
        
        this.messageSizeType = messageSizeType;
        this.messageSize = messageSize;
        this.stateSizeType = stateSizeType;
        this.stateSize = stateSize;
        this.sendFrequencyType = sendFrequencyType;
        this.sendFrequency = sendFrequency;
        this.sendType = sendType;
        this.sendSourceType = sendSourceType;
    }

    public SizeType getMessageSizeType()
    {
        return messageSizeType;
    }

    public int getMessageSize()
    {
        return messageSize;
    }

    public SizeType getStateSizeType()
    {
        return stateSizeType;
    }

    public int getStateSize()
    {
        return stateSize;
    }
    
    public SendFrequencyType getSendFrequencyType()
    {
        return sendFrequencyType;
    }

    public double getSendFrequency()
    {
        return sendFrequency;
    }

    public SendType getSendType()
    {
        return sendType;
    }

    public SendSourceType getSendSourceType()
    {
        return sendSourceType;
    }
    
    @Override
    public String toString()
    {
        return MessageFormat.format("message size: {0), state size: {1}, frequency: {2}, type: {3}, source: {4}", 
            messageSizeType, stateSizeType, sendFrequencyType, sendType, sendSourceType);
    }
}
