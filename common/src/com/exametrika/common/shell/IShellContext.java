/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.Map;

/**
 * The {@link IShellContext} is a shell context.
 * 
 * @author Medvedev-A
 */
public interface IShellContext
{
    /**
     * Returns shell.
     *
     * @return shell
     */
    IShell getShell();
    
    /**
     * Returns current command name path.
     *
     * @return current command name path
     */
    String getPath();
    
    /**
     * Executes command with specified parameters.
     *
     * @param commandName command name
     * @param parameters parameters
     * @return result of command execution or null
     */
    Object executeCommand(String commandName, Map<String, Object> parameters);
    
    /**
     * Executes command line.
     *
     * @param commandLine command line
     * @return result of command execution or null
     */
    Object executeLine(String commandLine);
}