/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ResourceAllocator} is an abstract implementation of {@link IResourceAllocator}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class ResourceAllocator implements IResourceAllocator, IResourceConsumer, ITimerListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ResourceAllocator.class);
    protected final String name;
    private final Map<Pattern, IAllocationPolicy> policies;
    private final IAllocationPolicy defaultPolicy;
    protected final ITimeService timeService;
    protected final IMarker marker;
    private final long quotaIncreaseDelay;
    protected final long initializePeriod;
    private final ConsumerNode root;
    private long nextQuotaIncreaseTime;
    protected long startTime;
    protected boolean initialized;
    
    public ResourceAllocator(String name, Map<String, IAllocationPolicy> policies, IAllocationPolicy defaultPolicy, 
        long quotaIncreaseDelay, long initializePeriod, ITimeService timeService)
    {
        Assert.notNull(name);
        Assert.notNull(policies);
        Assert.notNull(defaultPolicy);
        Assert.notNull(timeService);
        
        this.name = name;
        this.quotaIncreaseDelay = quotaIncreaseDelay;
        this.initializePeriod = initializePeriod;
        this.timeService = timeService;
        
        this.policies = new LinkedHashMap<Pattern, IAllocationPolicy>(policies.size());
        for (Map.Entry<String, IAllocationPolicy> entry : policies.entrySet())
            this.policies.put(Strings.createFilterPattern(entry.getKey(), false), entry.getValue());
        
        this.defaultPolicy = defaultPolicy;
        root = new ConsumerNode("", getAllocationPolicy(""), null);
        marker = Loggers.getMarker(name);
        
        if (initializePeriod > 0)
            startTime = timeService.getCurrentTime();
        else
            initialized = true;
    }
    
    public String getName()
    {
        return name;
    }
    
    @Override
    public final synchronized void register(String name, IResourceConsumer consumer)
    {
        Assert.notNull(name);
        Assert.notNull(consumer);
        
        String[] segments = parseName(name);
        root.add(0, segments, wrap(name, consumer));
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.consumerRegistered(name));
    }

    @Override
    public final synchronized void unregister(String name)
    {
        Assert.notNull(name);
        
        String[] segments = parseName(name);
        root.remove(0, segments);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.consumerUnregistered(name));
    }
    
    @Override
    public synchronized long getAmount()
    {
        return root.getAmount();
    }
    
    @Override
    public synchronized long getQuota()
    {
        return root.getQuota();
    }
    
    @Override
    public final synchronized void setQuota(long value)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.quotaSet(value));
        
        root.setQuota(value);
        
        nextQuotaIncreaseTime = timeService.getCurrentTime() + quotaIncreaseDelay;
    }
    
    @Override
    public void onTimer()
    {
        doOnTimer();
    }

    @Override
    public JsonObject getStatistics()
    {
        Json json = Json.object();
        root.buildStatistics(json);
        return json.toObject();
    }

    protected synchronized void doOnTimer()
    {
        long currentTime = timeService.getCurrentTime();
        if (nextQuotaIncreaseTime > 0 && currentTime >= nextQuotaIncreaseTime)
        {
            root.applyQuota();
            nextQuotaIncreaseTime = 0;
        }
        
        if (!initialized && currentTime >= startTime + initializePeriod)
            initialized = true;
        
        root.onTimer();
    }
    
    protected String[] parseName(String name)
    {
        return name.split("[.]");
    }
    
    private IAllocationPolicy getAllocationPolicy(String name)
    {
        for (Map.Entry<Pattern, IAllocationPolicy> entry : policies.entrySet())
        {
            if (entry.getKey().matcher(name).matches())
                return entry.getValue();
        }
        
        return defaultPolicy;
    }
    
    private IResourceConsumer wrap(String name, IResourceConsumer consumer)
    {
        if (!(consumer instanceof ResourceAllocator))
            return new DelayingResourceConsumerProxy(name, consumer);
        else
            return consumer;
    }
    
    private static String buildName(String[] segments, int count)
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < count; i++)
        {
            if (first)
                first = false;
            else
                builder.append(".");
            
            builder.append(segments[i]);
        }
        
        return builder.toString();
    }
    
    private class ConsumerNode extends CompositeConsumer
    {
        private final String name;
        private final ConsumerNode parent;
        private final Map<String, ConsumerNode> children = new LinkedHashMap<String, ConsumerNode>();

        public ConsumerNode(String name, IAllocationPolicy allocationPolicy, ConsumerNode parent)
        {
            super(new LinkedHashMap<String, IResourceConsumer>(), allocationPolicy);
            
            Assert.notNull(name);
            
            this.name = name;
            this.parent = parent;
        }
        
        public void applyQuota()
        {
            for (IResourceConsumer consumer : consumers.values())
            {
                if (consumer instanceof DelayingResourceConsumerProxy)
                    ((DelayingResourceConsumerProxy)consumer).applyQuota();
            }
            
            for (ConsumerNode child : children.values())
                child.applyQuota();
        }

        public void onTimer()
        {
            for (IResourceConsumer consumer : consumers.values())
            {
                if (consumer instanceof ResourceAllocator)
                    ((ResourceAllocator)consumer).onTimer();
            }
            
            for (ConsumerNode child : children.values())
                child.onTimer();
        }

        public void add(int pos, String[] segments, IResourceConsumer consumer)
        {
            if (pos >= segments.length)
                return;
            
            String segment = segments[pos];
            
            if (pos == segments.length - 1)
            {
                Assert.isTrue(!consumers.containsKey(segment));
                consumers.put(segment, consumer);
            }
            else
            {
                ConsumerNode node = children.get(segment);
                if (node == null)
                {
                    Assert.isTrue(!consumers.containsKey(segment));
                    node = new ConsumerNode(segment, getAllocationPolicy(buildName(segments, pos + 1)), this);
                    children.put(segment, node);
                    consumers.put(segment, node);
                }
                
                node.add(pos + 1, segments, consumer);
            }
        }
        
        public void remove(int pos, String[] segments)
        {
            if (pos >= segments.length)
                return;
            
            String segment = segments[pos];
            
            if (pos == segments.length - 1)
            {
                consumers.remove(segment);
                if (consumers.isEmpty() && parent != null)
                    parent.removeChild(name);
            }
            else
            {
                ConsumerNode node = children.get(segment);
                if (node != null)
                    node.remove(pos + 1, segments);
            }
        }

        public void buildStatistics(Json json)
        {
            if (name.isEmpty())
            {
                json = json.putObject("<root>");
                json.put("quota", getQuota())
                    .put("amount", getAmount());
            }

            json.put("allocationPolicy", allocationPolicy.getClass().getSimpleName());
            
            for (Map.Entry<String, IResourceConsumer> entry : consumers.entrySet())
            {
                IResourceConsumer consumer = entry.getValue();
                Json jsonConsumer = json.putObject(entry.getKey());
                jsonConsumer.put("quota", consumer.getQuota())
                    .put("amount", consumer.getAmount());
                
                if (consumer instanceof ConsumerNode)
                    ((ConsumerNode)consumer).buildStatistics(jsonConsumer);
            }
        }

        private void removeChild(String name)
        {
            children.remove(name);
            consumers.remove(name);
            
            if (consumers.isEmpty() && parent != null)
                parent.removeChild(this.name);
        }
    }
    
    private class DelayingResourceConsumerProxy implements IResourceConsumer
    {
        private final IResourceConsumer consumer;
        private long appliedQuota;
        private long quota;
        private final String name;

        public DelayingResourceConsumerProxy(String name, IResourceConsumer consumer)
        {
            Assert.notNull(name);
            Assert.notNull(consumer);
            
            this.name = name;
            this.consumer = consumer;
            this.quota = consumer.getQuota();
            this.appliedQuota = quota;
        }
        
        @Override
        public long getAmount()
        {
            if (initialized)
                return consumer.getAmount();
            else
                return quota;
        }

        @Override
        public long getQuota()
        {
            return quota;
        }

        @Override
        public void setQuota(long value)
        {
            quota = value;
            
            if (!initialized || quota <= appliedQuota)
            {
                appliedQuota = quota;
                consumer.setQuota(quota);
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.quotaSet(name, value));
            }
        }
        
        public void applyQuota()
        {
            if (quota > appliedQuota)
            {
                appliedQuota = quota;
                consumer.setQuota(quota);
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.quotaSet(name, quota));
            }
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Quota ''{1}'' has been set to resource consumer ''{0}''.")
        ILocalizedMessage quotaSet(String name, long value);

        @DefaultMessage("Quota ''{0}'' has been set to resource allocator.")
        ILocalizedMessage quotaSet(long value);

        @DefaultMessage("Resource consumer ''{0}'' has been unregistered.")
        ILocalizedMessage consumerUnregistered(String name);

        @DefaultMessage("Resource consumer ''{0}'' has been registered.")
        ILocalizedMessage consumerRegistered(String name);
    }
}
