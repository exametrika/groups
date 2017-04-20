/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.management;

import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link ICommandManager} represents a group command manager.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface ICommandManager
{
    /**
     * Executes command in a group.
     *
     * @param command command
     * @param completionHandler completion handler to be called when command is guaranteed to be
     * executed on all group nodes or null if completion handler is not set
     */
    void execute(ICommand command, ICompletionHandler<ICommand> completionHandler);
}