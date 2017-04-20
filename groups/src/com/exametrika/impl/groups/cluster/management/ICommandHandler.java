/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.management;

/**
 * The {@link ICommandHandler} represents a group command handler.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface ICommandHandler
{
    /**
     * Does handler support given command?
     *
     * @param command command
     * @return true if handler supports given command
     */
    boolean supports(ICommand command);
    
    /**
     * Executes command on local node.
     *
     * @param command command
     */
    void execute(ICommand command);
}