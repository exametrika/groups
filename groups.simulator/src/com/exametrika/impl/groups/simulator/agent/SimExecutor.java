/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import java.util.LinkedHashMap;
import java.util.Map;

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
import com.exametrika.impl.groups.simulator.channel.SimGroupChannel;
import com.exametrika.impl.groups.simulator.messages.SimActionMessage;
import com.exametrika.impl.groups.simulator.messages.SimActionResponseMessage;

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
    private SimGroupChannel groupChannel;
    private long delayPeriod;
    private boolean oneTimeDelay;
    private Map<String, ICondition<IMessage>> suspendConditions = new LinkedHashMap<String, ICondition<IMessage>>();
    private Map<String, LogInfo> logFilters = new LinkedHashMap<String, LogInfo>();
    private volatile IMessage message;
   
    public SimExecutor(SimAgentChannel agentChannel)
    {
        Assert.notNull(agentChannel);
        
        this.agentChannel = agentChannel;
        this.compileContext = Expressions.createCompileContext(null);
        Times.setTest(Times.getCurrentTime());
    }
    
    public void setGroupChannel(SimGroupChannel groupChannel)
    {
        Assert.notNull(groupChannel);
        Assert.isNull(this.groupChannel);
        
        this.groupChannel = groupChannel;
    }
    
    public void intercept(IMessage message)
    {
        long delayPeriod;
        synchronized (this)
        {
            for (Map.Entry<String, LogInfo> entry : logFilters.entrySet())
            {
                LogInfo info = entry.getValue();
                if (!info.enabled)
                    continue;
                
                if (info.filter.evaluate(message))
                {
                    Object result;
                    if (info.expression != null)
                        result = info.expression.execute(message, null);
                    else
                        result = message;
                    
                    agentChannel.send(new SimActionResponseMessage("log", "Log: " + entry.getKey() + ", result: " + result.toString()));
                }
            }
            
            this.message = message;
            try
            {
                for (Map.Entry<String, ICondition<IMessage>> entry : suspendConditions.entrySet())
                {
                    ICondition<IMessage> suspendCondition = entry.getValue();
                    while (suspendCondition.evaluate(message))
                    {
                        agentChannel.send(new SimActionResponseMessage("stop", "Stop: " + entry.getKey()));
                        wait();
                    }
                }
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
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
    
    public synchronized void onActionReceived(SimActionMessage message)
    {
        if (message.getActionName().equals("start"))
        {
            this.delayPeriod = (long)message.getParameters().get("delay");
            this.oneTimeDelay = false;
            
            suspendConditions.remove("");
            notify();
        }
        else if (message.getActionName().equals("stop"))
        {
            String name = (String)message.getParameters().get("name");
            if (message.getParameters().containsKey("remove"))
            {
                suspendConditions.remove(name);
                notify();
            }
            else
            {
                String expression = (String)message.getParameters().get("condition");
                ICondition<IMessage> condition;
                if (expression != null)
                    condition = new ExpressionCondition<IMessage>(Expressions.compile(expression, compileContext));
                else
                    condition = new TrueCondition<IMessage>();
                
                suspendConditions.put(name, condition);
                notify();
            }
        }
        else if (message.getActionName().equals("suspend"))
        {
            suspendConditions.put("", new TrueCondition<IMessage>());
            notify();
        }
        else if (message.getActionName().equals("resume"))
        {
            suspendConditions.remove("");
            notify();
        }
        else if (message.getActionName().equals("delay"))
        {
            this.delayPeriod = (long)message.getParameters().get("period");
            this.oneTimeDelay = Boolean.TRUE.equals(message.getParameters().get("oneTime"));
        }
        else if (message.getActionName().equals("print"))
        {
            IMessage currentMessage = this.message;
            if (currentMessage != null)
            {
                String expressionStr = (String)message.getParameters().get("expression");
                Object result;
                if (expressionStr != null)
                    result = Expressions.evaluate(expressionStr, currentMessage, null);
                else
                    result = currentMessage;
                
                agentChannel.send(new SimActionResponseMessage("print", result.toString()));
            }
        }
        else if (message.getActionName().equals("log"))
        {
            String name = (String)message.getParameters().get("name");
            if (message.getParameters().containsKey("remove"))
                logFilters.remove(name);
            else
            {
                String filter = (String)message.getParameters().get("filter");
                String expression = (String)message.getParameters().get("expression");
                boolean enabled = !Boolean.TRUE.equals(message.getParameters().get("off"));
                
                LogInfo info = new LogInfo();
                
                if (filter != null)
                    info.filter = new ExpressionCondition<IMessage>(Expressions.compile(filter, compileContext));
                else
                    info.filter = null;
                
                if (expression != null)
                    info.expression = Expressions.compile(expression, compileContext);
                else
                    info.expression = null;
                info.enabled = enabled;
                
                logFilters.put(name, info);
            }
        }
        else if (message.getActionName().equals("time"))
            Times.setTest(Times.getSystemCurrentTime() + (long)message.getParameters().get("delta"));
        else if (message.getActionName().equals("kill"))
        {
            groupChannel.stop();
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    agentChannel.stop();
                }
            }).start();
        }
    }
    
    public void onDisconnected()
    {
        Times.clearTest();
    }
    
    private static class LogInfo
    {
        private ICondition<IMessage> filter;
        private IExpression expression;
        private boolean enabled;
    }
}
