/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.net.ITcpRateControllerFactory;
import com.exametrika.common.net.utils.ITcpPacketDiscardPolicy;
import com.exametrika.common.net.utils.TcpNameFilter;

/**
 * The {@link ChannelParameters} is a channel parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class ChannelParameters
{
    public String channelName;
    public boolean clientPart;
    public boolean serverPart;
    public IReceiver receiver;
    public InetAddress bindAddress;
    public Integer portRangeStart;
    public Integer portRangeEnd;
    public boolean secured;
    public String keyStorePath;
    public String keyStorePassword;
    public ITcpConnectionFilter connectionFilter;
    public ITcpRateControllerFactory rateController;
    public TcpNameFilter adminFilter;
    public List<ISerializationRegistrar> serializationRegistrars = new ArrayList<ISerializationRegistrar>();
    public boolean multiThreaded = false;
    public ITcpPacketDiscardPolicy<IMessage> discardPolicy;
}