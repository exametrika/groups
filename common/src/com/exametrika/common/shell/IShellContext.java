/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

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
}