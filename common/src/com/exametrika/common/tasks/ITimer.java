/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

/**
 * The {@link ITimer} represents a timer which periodically calls specified listeners.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ITimer
{
    /**
     * Returns timer period in milliseconds.
     *
     * @return timer period in milliseconds.
     */
    long getPeriod();
    
    /**
     * Sets timer period in milliseconds.
     *
     * @param period timer period in milliseconds.
     */
    void setPeriod(long period);
    
    /**
     * Suspends timer execution.
     */
    void suspend();

    /**
     * Resumes timer execution.
     */
    void resume();
    
    /**
     * Notifies timer thread to execute single timer run without waiting timer period.
     */
    void signal();
    
    /**
     * Adds timer listener.
     *
     * @param listener timer listener
     */
    void addTimerListener(ITimerListener listener);
    
    /**
     * Removes timer listener.
     *
     * @param listener timer listener
     */
    void removeTimerListener(ITimerListener listener);
    
    /**
     * Removes all timer listeners.
     */
    void removeAllTimerListeners();
}
