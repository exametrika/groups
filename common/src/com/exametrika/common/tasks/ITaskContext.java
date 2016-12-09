/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

import java.util.Map;


/**
 * The {@link ITaskContext} represents a context of currently active tasks. 
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author AndreyM
 */
public interface ITaskContext
{
    /**
     * Returns active task specific parameters.
     *
     * @return active task specific parameters
     */
    public Map<String, Object> getParameters();
}
