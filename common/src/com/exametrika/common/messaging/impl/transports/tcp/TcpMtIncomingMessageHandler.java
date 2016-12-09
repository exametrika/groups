/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.ITaskListener;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpMtIncomingMessageHandler} represents a multithreaded handler of incoming messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpMtIncomingMessageHandler implements ITcpIncomingMessageHandler, ITaskQueue<TcpMtReceiveTask>, 
    ITaskHandler<TcpMtReceiveTask>, ITaskListener<TcpMtReceiveTask>
{
    private final TaskExecutor executor;
    private final ITaskQueue<TcpMtReceiveTask> queue;
    private final IReceiver receiver;
    private final ISerializationRegistry serializationRegistry;
    
    public TcpMtIncomingMessageHandler(int threadCount, String name, IReceiver receiver, ISerializationRegistry serializationRegistry)
    {
        Assert.notNull(name);
        Assert.notNull(receiver);
        Assert.notNull(serializationRegistry);
        
        TaskQueue<TcpMtReceiveTask> queue = new TaskQueue<TcpMtReceiveTask>();
        executor = new TaskExecutor<TcpMtReceiveTask>(threadCount, queue, this, "[" + name + "] tcp incoming message handler");
        executor.addTaskListener(this);
        this.queue = queue;
        this.receiver = receiver;
        this.serializationRegistry = serializationRegistry;
    }
    
    @Override
    public boolean offer(TcpMtReceiveTask task)
    {
        return queue.offer(task);
    }
    
    @Override
    public void put(TcpMtReceiveTask task)
    {
        queue.put(task);
    }
    
    @Override
    public void handle(TcpMtReceiveTask task)
    {
        TcpPacket packet = task.getPacket();
        IMessage message = MessageSerializers.deserialize(serializationRegistry, task.getConnection().getRemoteAddress(), 
            task.getConnection().getLocalAddress(), packet, TcpTransport.HEADER_OVERHEAD);
        receiver.receive(message);
    }
    
    @Override
    public void start()
    {
        executor.start();
    }
    
    @Override
    public void stop()
    {
        executor.stop();
    }
    
    @Override
    public void onTaskStarted(TcpMtReceiveTask task)
    {
    }
    
    @Override
    public void onTaskCompleted(TcpMtReceiveTask task)
    {
        task.onCompleted();
    }
    
    @Override
    public void onTaskFailed(TcpMtReceiveTask task, Throwable error)
    {
        task.onCompleted();
    }
}