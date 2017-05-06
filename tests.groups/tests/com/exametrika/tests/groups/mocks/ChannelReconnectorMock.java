/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

public class ChannelReconnectorMock implements IChannelReconnector
{
    public boolean reconnectRequested;

    @Override
    public void reconnect()
    {
        reconnectRequested = true;
    }
}