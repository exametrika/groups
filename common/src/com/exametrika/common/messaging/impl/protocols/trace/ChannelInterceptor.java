/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.trace;

import com.exametrika.common.json.Json;
import com.exametrika.common.utils.SlotAllocator;




/**
 * The {@link ChannelInterceptor} is a channel interceptor.
 * 
 * @threadsafety Implementations of this class and its methods are thread safe.
 * @author AndreyM
 */
public class ChannelInterceptor
{
    public static ChannelInterceptor INSTANCE = new ChannelInterceptor();
    public final SlotAllocator slotAllocator;
    
    public ChannelInterceptor()
    {
        slotAllocator = new SlotAllocator();
    }
    
    public ChannelInterceptor(ChannelInterceptor interceptor)
    {
        slotAllocator = interceptor.slotAllocator;
    }
    
    public int onStarted(String channelName)
    {
        return slotAllocator.allocate("channels." + channelName, 
            "exa.messaging", Json.object().put("channel", channelName).toObject()).id;
    }
    
    public void onStopped(int id)
    {
        slotAllocator.free(id);
    }
    
    public void onMessageSent(int id, int messageSize)
    {
    }
    
    public void onMessageReceived(int id, int messageSize)
    {
    }
    
    public void onNodeConnected(int id, String nodeName)
    {
    }
    
    public void onNodeFailed(int id, String nodeName)
    {
    }
    
    public void onNodeDisconnected(int id, String nodeName)
    {
    }
}
