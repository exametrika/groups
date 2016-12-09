/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

import com.exametrika.common.utils.ICondition;


/**
 * The {@link IActivationCondition} can be used as activation condition in a {@link ITaskScheduler}. 
 * 
 * @param <T> task type
 * @see ITaskScheduler
 * @see ICondition
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IActivationCondition<T> extends ICondition<Long>
{
    /**
     * Can condition be activated. This method supersedes {@link #evaluate}.
     *
     * @param currentTime current time
     * @param context task context
     * @return true if condition can be activated
     */
    boolean canActivate(long currentTime, ITaskContext context);
    
    /**
     * Called by scheduler for active task and gives possibility to interrupt task.
     *
     * @param currentTime current time
     */
    void tryInterrupt(long currentTime);
    
    /**
     * Called when task of this activation condition has been completed.
     *
     * @param context task context
     */
    void onCompleted(ITaskContext context);
}
