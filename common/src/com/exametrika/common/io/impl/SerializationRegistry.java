/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.ISerializer;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link SerializationRegistry} is an implementation of {@link ISerializationRegistry}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SerializationRegistry implements ISerializationRegistry
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(SerializationRegistry.class);
    private final ISerializationRegistryExtension extension;
    private final Map<UUID, SerializationInfo> infosById = new ConcurrentHashMap<UUID, SerializationInfo>();
    private final Map<Class<?>, SerializationInfo> infosByClass = new ConcurrentHashMap<Class<?>, SerializationInfo>();

    /**
     * Creates a new object.
     */
    public SerializationRegistry()
    {
        this(null);
    }
    
    /**
     * Creates a new object.
     * 
     * @param extension regsitry extension. Can be null
     */
    public SerializationRegistry(ISerializationRegistryExtension extension)
    {
        this.extension = extension;
    }
    
    @Override
    public ISerializationInfo getInfo(UUID id)
    {
        Assert.notNull(id);
        
        if (extension != null)
        {
            ISerializationInfo info = extension.findInfo(id);
            if (info != null)
                return info;
        }
        
        SerializationInfo info = infosById.get(id);
        if (info != null)
            return info;
        
        throw new InvalidArgumentException(messages.infoNotFound(id));
    }
    
    @Override
    public ISerializationInfo getInfo(Class<?> serializableClass)
    {
        Assert.notNull(serializableClass);
        
        if (extension != null)
        {
            ISerializationInfo info = extension.findInfo(serializableClass);
            if (info != null)
                return info;
        }
        
        SerializationInfo info = infosByClass.get(serializableClass);
        if (info != null)
            return info;
        
        throw new InvalidArgumentException(messages.infoNotFound(serializableClass));
    }

    @Override
    public void register(ISerializationRegistrar registrar)
    {
        Assert.notNull(registrar);
        
        registrar.register(this);
    }
    
    @Override
    public synchronized void register(UUID id, Class<?> serializableClass, ISerializer serializer)
    {
        Assert.notNull(id);
        Assert.notNull(serializableClass);
        Assert.notNull(serializer);
        
        SerializationInfo info = infosById.get(id);
        if (info != null)
        {
            if (!info.serializableClass.equals(serializableClass))
                throw new InvalidArgumentException(messages.infoDifferentClass(id, serializableClass, info.serializableClass));
            
            Assert.isTrue(infosByClass.get(serializableClass) == info);
            
            info.refCount++;
            return;
        }
        else
            Assert.isTrue(!infosByClass.containsKey(serializableClass));
        
        info = new SerializationInfo(id, serializableClass, serializer);
        infosByClass.put(serializableClass, info);
        infosById.put(id, info);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.infoRegistered(id, serializableClass));
    }
    
    @Override
    public void unregister(ISerializationRegistrar registrar)
    {
        Assert.notNull(registrar);
        
        registrar.unregister(this);
    }
    
    @Override
    public synchronized void unregister(UUID id)
    {
        Assert.notNull(id);
        
        SerializationInfo info = infosById.get(id);
        if (info != null)
        {
            info.refCount--;
            if (info.refCount == 0)
            {
                infosById.remove(id);
                infosByClass.remove(info.getSerializableClass());
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.infoUnregistered(info.getId(), info.getSerializableClass()));
            }
        }
    }
    
    private static class SerializationInfo implements ISerializationInfo
    {
        private final UUID id;
        private final Class<?> serializableClass;
        private final ISerializer serializer;
        private int refCount = 1;

        public SerializationInfo(UUID id, Class<?> serializableClass, ISerializer serializer)
        {
            this.id = id;
            this.serializableClass = serializableClass;
            this.serializer = serializer;
        }

        public Class<?> getSerializableClass()
        {
            return serializableClass;
        }
        
        @Override
        public UUID getId()
        {
            return id;
        }
        
        @Override
        public ISerializer getSerializer()
        {
            return serializer;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Serialization info is not found for id ''{0}''.")
        ILocalizedMessage infoNotFound(UUID id);
        @DefaultMessage("Serialization info is not found for class ''{0}''.")
        ILocalizedMessage infoNotFound(Class serializableClass);
        @DefaultMessage("Could not register serialization info with id ''{0}'' and class ''{1}''. Serialization info for specified identifier has different class ''{2}''.")
        ILocalizedMessage infoDifferentClass(UUID id, Class serializableClass, Class existingClass);
        @DefaultMessage("Serialization info with id ''{0}'' and class ''{1}'' is registered.")
        ILocalizedMessage infoRegistered(UUID id, Class serializableClass);
        @DefaultMessage("Serialization info with id ''{0}'' and class ''{1}'' is unregistered.")
        ILocalizedMessage infoUnregistered(UUID id, Class serializableClass);        
    }
}
