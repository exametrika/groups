/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.jdk;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.exametrika.common.io.ISerializer;
import com.exametrika.common.io.ISerializationRegistry.ISerializationInfo;
import com.exametrika.common.io.impl.ISerializationRegistryExtension;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link JdkSerializationRegistryExtension} is an serialization mapper that maps all {@link Serializable} objects to
 * {@link JdkSerializer}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JdkSerializationRegistryExtension implements ISerializationRegistryExtension, IJdkSerializationRegistry
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(JdkSerializationRegistryExtension.class);
    private static final ClassLoader NULL_CLASSLOADER = new ClassLoader(){};
    private final Map<UUID, SerializationInfo> infosById = new ConcurrentHashMap<UUID, SerializationInfo>();
    private final Map<ClassLoader, SerializationInfo> infosByClassLoader = new ConcurrentHashMap<ClassLoader, SerializationInfo>();

    @Override
    public ISerializationInfo findInfo(UUID id)
    {
        Assert.notNull(id);
        return infosById.get(id);
    }

    @Override
    public ISerializationInfo findInfo(Class<?> serializableClass)
    {
        if (!Serializable.class.isAssignableFrom(serializableClass))
            return null;
        
        ClassLoader classLoader = serializableClass.getClassLoader();
        if (classLoader == null)
            classLoader = NULL_CLASSLOADER;
        return infosByClassLoader.get(classLoader);
    }
    
    @Override
    public synchronized void register(UUID id, ClassLoader classLoader, ISerializer serializer)
    {
        Assert.notNull(id);
        Assert.notNull(serializer);
        
        if (classLoader == null)
            classLoader = NULL_CLASSLOADER;
        
        if (infosById.containsKey(id))
            throw new InvalidArgumentException(messages.infoIdNotUnique(id));
        if (infosByClassLoader.containsKey(classLoader))
            throw new InvalidArgumentException(messages.infoClassLoaderNotUnique(id));
        
        SerializationInfo info = new SerializationInfo(id, classLoader, serializer);
        infosByClassLoader.put(classLoader, info);
        infosById.put(id, info);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.infoRegistered(id));

    }
    
    @Override
    public synchronized void unregister(UUID id)
    {
        Assert.notNull(id);
        
        SerializationInfo info = infosById.get(id);
        if (info != null)
        {
            infosById.remove(id);
            infosByClassLoader.remove(info.getClassLoader());
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.infoUnregistered(info.getId()));
        }
    }

    private static class SerializationInfo implements ISerializationInfo
    {
        private final UUID id;
        private final ClassLoader classLoader;
        private final ISerializer serializer;

        public SerializationInfo(UUID id, ClassLoader classLoader, ISerializer serializer)
        {
            this.id = id;
            this.classLoader = classLoader;
            this.serializer = serializer;
        }
        
        public ClassLoader getClassLoader()
        {
            return classLoader;
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
        @DefaultMessage("Could not register serialization info with id ''{0}''. Serialization info with specified identifier already exists in registry.")
        ILocalizedMessage infoIdNotUnique(UUID id);
        @DefaultMessage("Could not register serialization info with id ''{0}''. Serialization info with specified class loader already exists in registry.")
        ILocalizedMessage infoClassLoaderNotUnique(UUID id);
        @DefaultMessage("Serialization info with id ''{0}'' is registered.")
        ILocalizedMessage infoRegistered(UUID id);
        @DefaultMessage("Serialization info with id ''{0}'' is unregistered.")
        ILocalizedMessage infoUnregistered(UUID id);        
    }
}
