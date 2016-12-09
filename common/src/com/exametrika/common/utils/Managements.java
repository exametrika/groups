/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;

/**
 * The {@link Managements} contains different utility methods for work with JMX.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Managements
{
    private static final ILogger logger = Loggers.get(Managements.class);

    /**
     * Returns full name by type name and instance name.
     *
     * @param typeName type name
     * @param instanceName instance name
     * @return full name
     */
    public static String getFullName(String typeName, String instanceName)
    {
        return typeName + ",name=" + instanceName;
    }
    
    /**
     * Registers specified MBean instance with specified name.
     *
     * @param name name
     * @param instance instance
     */
    public static void register(String name, Object instance)
    {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer(); 
        
        try
        {
            ObjectName objectName = 
                    new ObjectName(name); 
             
            if (!server.isRegistered(objectName))
                server.registerMBean(instance, objectName);
        }
        catch (Exception e)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }

    /**
     * Registers specified MBean instance with specified type and instance names.
     *
     * @param typeName type name
     * @param instanceName instance name
     * @param instance instance
     */
    public static void register(String typeName, String instanceName, Object instance)
    {
        register(getFullName(typeName, instanceName), instance);
    }
    
    /**
     * Unregisters MBean instance with specified name.
     *
     * @param name name
     */
    public static void unregister(String name)
    {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer(); 
        
        try
        {
            ObjectName objectName = 
                    new ObjectName(name); 
             
            if (server.isRegistered(objectName))
                server.unregisterMBean(objectName);
        }
        catch (Exception e)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }

    /**
     * Unregisters MBean instance with specified type and instance names.
     *
     * @param typeName type name
     * @param instanceName instance name
     */
    public static void unregister(String typeName, String instanceName)
    {
        unregister(getFullName(typeName, instanceName));
    }
    
    private Managements()
    {
    }
}
