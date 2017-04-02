/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import com.exametrika.api.groups.cluster.IGroupChannel;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.expression.impl.ExpressionCondition;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.TrueCondition;
import com.exametrika.impl.groups.simulator.messages.ActionMessage;
import com.exametrika.impl.groups.simulator.messages.ActionResponseMessage;

/**
 * The {@link SimExecutor} represents a simulator executor.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimExecutor
{
    private final SimAgentChannel agentChannel;
    private final CompileContext compileContext;
    private IGroupChannel groupChannel;
    private long delayPeriod;
    private boolean oneTimeDelay;
    private ICondition<IMessage> suspendCondition = new TrueCondition<IMessage>();
    private ICondition<IMessage> logFilter;
    private IExpression logExpression;
    private boolean logEnabled;
    private volatile IMessage message;
    private boolean suspended;
   
    public SimExecutor(SimAgentChannel agentChannel)
    {
        Assert.notNull(agentChannel);
        
        this.agentChannel = agentChannel;
        this.compileContext = Expressions.createCompileContext(null);
        Times.setTest(Times.getCurrentTime());
    }
    
    public void setGroupChannel(IGroupChannel groupChannel)
    {
        Assert.notNull(groupChannel);
        Assert.isNull(this.groupChannel);
        
        this.groupChannel = groupChannel;
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
    
    public void print(String expressionStr)
    {
        IMessage currentMessage = this.message;
        if (currentMessage != null)
        {
            Object result;
            if (expressionStr != null)
                result = Expressions.evaluate(expressionStr, currentMessage, null);
            else
                result = currentMessage;
            
            agentChannel.send(new ActionResponseMessage("print", result.toString()));
        }
    }
    
    public synchronized void log(String filter, String expression, boolean enabled)
    {
        if (expression != null)
            logExpression = Expressions.compile(expression, compileContext);
        else
            logFilter = null;
        
        if (expression != null)
            logExpression = Expressions.compile(expression, compileContext);
        else
            logExpression = null;
        
        logEnabled = enabled;
    }
    
    public void intercept(IMessage message)
    {
        long delayPeriod;
        synchronized (this)
        {
            if (logEnabled)
            {
                if (logFilter == null || logFilter.evaluate(message))
                {
                    Object result;
                    if (logExpression != null)
                        result = logExpression.execute(message, null);
                    else
                        result = message;
                    
                    agentChannel.send(new ActionResponseMessage("log", result.toString()));
                }
            }
            
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
    }
    
    public synchronized void onActionReceived(ActionMessage message)
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
            print((String)message.getParameters().get("expression"));
        else if (message.getActionName().equals("log"))
            log((String)message.getParameters().get("filter"), (String)message.getParameters().get("expression"),
                !Boolean.TRUE.equals(message.getParameters().get("off")));
        else if (message.getActionName().equals("time"))
            Times.setTest((long)message.getParameters().get("value"));
        else if (message.getActionName().equals("kill"))
        {
            groupChannel.stop();
            agentChannel.stop();
        }
    }
    
    public void onDisconnected()
    {
    }
}
