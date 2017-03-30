/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.TrueCondition;
import com.exametrika.impl.groups.simulator.messages.ActionMessage;
import com.exametrika.impl.groups.simulator.messages.ActionResponseMessage;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.impl.ExpressionCondition;

/**
 * The {@link SimExecutor} represents a simulator executor.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimExecutor
{
    private final SimAgentChannel channel;
    private final CompileContext compileContext;
    private long delayPeriod;
    private boolean oneTimeDelay;
    private ICondition<IMessage> suspendCondition = new TrueCondition<IMessage>();
    private volatile IMessage message;
    private boolean suspended;
    private boolean log;
    
    public SimExecutor(SimAgentChannel channel)
    {
        Assert.notNull(channel);
        
        this.channel = channel;
        this.compileContext = Expressions.createCompileContext(null);
        Times.setTest(Times.getCurrentTime());
    }
    
    public synchronized boolean isSuspended()
    {
        return suspended;
    }
    
    public synchronized IMessage getMessage()
    {
        return message;
    }
    
    public synchronized void suspend(ICondition<IMessage> suspendCondition)
    {
        Assert.notNull(suspendCondition);
        
        this.suspendCondition = suspendCondition;
        notify();
    }
    
    public synchronized void resume()
    {
        suspendCondition = null;
        notify();
    }
    
    public synchronized void delay(long delayPeriod, boolean oneTimeDelay)
    {
        this.delayPeriod = delayPeriod;
        this.oneTimeDelay = oneTimeDelay;
    }
    
    public void intercept(IMessage message)
    {
        if (log)
            channel.send(new ActionResponseMessage(message.toString()));
            
        long delayPeriod;
        synchronized (this)
        {
            this.message = message;
            try
            {
                while (suspendCondition != null && suspendCondition.evaluate(message))
                {
                    suspended = true;
                    wait();
                }
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
            finally
            {
                suspended = false;
            }
            
            delayPeriod = this.delayPeriod;
            if (oneTimeDelay)
            {
                this.delayPeriod = 0;
                this.oneTimeDelay = false;
            }
        }
        
        if (delayPeriod != 0)
            Threads.sleep(delayPeriod);
    }
    
    public void onTimer(long currentTime)
    {
        // TODO:
    }
    
    public synchronized void onActionReceive(ActionMessage message)
    {
        if (message.getActionName().equals("start"))
        {
            delay((long)message.getParameters().get("delay"), false);
            resume();
        }
        else if (message.getActionName().equals("stop"))
        {
            String expression = (String)message.getParameters().get("condition");
            ICondition<IMessage> condition;
            if (expression != null)
                condition = new ExpressionCondition<IMessage>(Expressions.compile(expression, compileContext));
            else
                condition = new TrueCondition<IMessage>();
            
            suspend(condition);
        }
        else if (message.getActionName().equals("suspend"))
            suspend(new TrueCondition<IMessage>());
        else if (message.getActionName().equals("resume"))
            resume();
        else if (message.getActionName().equals("delay"))
            delay((long)message.getParameters().get("period"), Boolean.TRUE.equals(message.getParameters().get("oneTime")));
        else if (message.getActionName().equals("print"))
        {
            IMessage currentMessage = this.message;
            if (currentMessage != null)
                channel.send(new ActionResponseMessage(currentMessage.toString()));
        }
        else if (message.getActionName().equals("log"))
        {
            
        }
        else if (message.getActionName().equals("time"))
            Times.setTest((long)message.getParameters().get("value"));
        else if (message.getActionName().equals("kill"))
            System.exit(1);
    }
    
    public void onDisconnected()
    {
        // TODO:
    }
}
