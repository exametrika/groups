/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.Map;

/**
 * The {@link IShellCommandExecutor} is an executor of shell command.
 * 
 * @author Medvedev-A
 */
public interface IShellCommandExecutor
{
    /**
     * Executes command with specified parameters.
     *
     * @param context context
     * @param parameters parameters
     * @return result of command execution or null
     */
    Object execute(IShellContext context, Map<String, Object> parameters);
}