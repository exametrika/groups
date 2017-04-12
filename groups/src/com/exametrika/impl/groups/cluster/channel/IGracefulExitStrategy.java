/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;


/**
 * The {@link IGracefulExitStrategy} is used to check that communication channel can be gracefully exited. 
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGracefulExitStrategy
{
    /**
     * Called when communication channel is about to be gracefully exit the cluster. Requests strategy that communication channel can be exited.
     * Strategy can allow or deny channel closure. If strategy allows channel closure it can disable certain functionality,
     * which can prevent gracefull channel closure. 
     *
     * @return true if channel closure is allowed
     */
    boolean requestExit();
}
