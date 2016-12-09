/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.threadlocal;

/**
 * The {@link IThreadComponentManager} is a manager of thread components.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IThreadComponentManager
{
    /**
     * Creates thread-local components for current thread.
     */
    void createThreadComponents();
    
    /**
     * Destroys thread-local components for current thread.
     */
    void destroyThreadComponents();
    
    /**
     * Adds thread component listener.
     *
     * @param listener thread component listener to add
     */
    void addThreadComponentListener(IThreadComponentListener listener);
    
    /**
     * Removes thread component listener.
     *
     * @param listener thread component listener to remove
     */
    void removeThreadComponentListener(IThreadComponentListener listener);
    
    /**
     * Removes all thread component listeners.
     */
    void removeAllThreadComponentListeners();
}
