/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.concurrent.atomic.AtomicInteger;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.tasks.IDelayedTaskQueue;
import com.exametrika.common.utils.Assert;

/**
 * The {@link FailureModellingProtocol} is a protocol used to model different network failures such as message losses, message
 * reorderings, latencies, etc.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureModellingProtocol extends AbstractProtocol
{
    private final ILatencyModel latencyModel;
    private final ILossModel lossModel;
    private final IDelayedTaskQueue<Runnable> latencyQueue;
    private AtomicInteger totalSentMessagesCount = new AtomicInteger();
    private AtomicInteger sentMessagesCount = new AtomicInteger();
    private AtomicInteger lostSentMessagesCount = new AtomicInteger();
    private AtomicInteger totalReceivedMessagesCount = new AtomicInteger();
    private AtomicInteger receivedMessagesCount = new AtomicInteger();
    private AtomicInteger lostReceivedMessagesCount = new AtomicInteger();
    private volatile boolean started;

    public FailureModellingProtocol(String channelName, IMessageFactory messageFactory, ILatencyModel latencyModel,
        ILossModel lossModel, IDelayedTaskQueue<Runnable> latencyQueue)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(latencyModel);
        Assert.notNull(lossModel);
        Assert.notNull(latencyQueue);

        this.latencyModel = latencyModel;
        this.lossModel = lossModel;
        this.latencyQueue = latencyQueue;
    }
    
    /**
     * Returns total count of sent messages including actually sent and lost messages.
     *
     * @return total count of sent messages
     */
    public int getTotalSentMessagesCount()
    {
        return totalSentMessagesCount.get();
    }
    
    /**
     * Returns count of actually sent messages.
     *
     * @return count of actually sent messages
     */
    public int getSentMessagesCount()
    {
        return sentMessagesCount.get();
    }
    
    /**
     * Return count of sent and lost messages.
     *
     * @return count of sent and lost messages
     */
    public int getLostSentMessagesCount()
    {
        return lostSentMessagesCount.get();
    }
    
    /**
     * Returns total count of received messages including actually received and lost messages.
     *
     * @return total count of received messages
     */
    public int getTotalReceivedMessagesCount()
    {
        return totalReceivedMessagesCount.get();
    }
    
    /**
     * Returns count of actually received messages.
     *
     * @return count of actually received messages
     */
    public int getReceivedMessagesCount()
    {
        return receivedMessagesCount.get();
    }
    
    /**
     * Return count of received and lost messages.
     *
     * @return count of received and lost messages
     */
    public int getLostReceivedMessagesCount()
    {
        return lostReceivedMessagesCount.get();
    }
    
    public void resetStatistics()
    {
        totalSentMessagesCount.set(0);
        sentMessagesCount.set(0);
        lostSentMessagesCount.set(0);
        totalReceivedMessagesCount.set(0);
        receivedMessagesCount.set(0);
        lostReceivedMessagesCount.set(0);
    }
    
    @Override
    public void start()
    {
        super.start(); 
        
        started = true;
    }
    
    @Override
    public void stop()
    {
        started = false;
        super.stop();
    }
    
    @Override
    protected void doSend(final ISender sender, final IMessage message)
    {
        totalSentMessagesCount.incrementAndGet();
        
        if (!started || lossModel.canDropMessage(message))
        {
            lostSentMessagesCount.incrementAndGet();
            return;
        }

        final long latency = latencyModel.getLatency();
        if (latency != 0)
        {
            latencyQueue.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    sentMessagesCount.incrementAndGet();
                    sender.send(message);
                }
            }, latency);
        }
        else
        {
            sentMessagesCount.incrementAndGet();
            sender.send(message);
        }
    }

    @Override
    protected void doReceive(final IReceiver receiver, final IMessage message)
    {
        totalReceivedMessagesCount.incrementAndGet();
        
        if (!started || lossModel.canDropMessage(message))
        {
            lostReceivedMessagesCount.incrementAndGet();
            return;
        }

        final long latency = latencyModel.getLatency();
        if (latency != 0)
        {
            latencyQueue.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    receivedMessagesCount.incrementAndGet();
                    receiver.receive(message);
                }
            }, latency);
        }
        else
        {
            receivedMessagesCount.incrementAndGet();
            receiver.receive(message);
        }
    }
}
