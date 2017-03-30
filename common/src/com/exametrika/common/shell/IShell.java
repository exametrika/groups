/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.List;

/**
 * The {@link IShell} is an interactive command line shell.
 * 
 * @author Medvedev-A
 */
public interface IShell extends Runnable
{
    /**
     * Returns shell title.
     *
     * @return shell title
     */
    String getTitle();
    
    /**
     * Returns list of shell commands.
     *
     * @return list of shell commands
     */
    List<IShellCommand> getCommands();
    
    /**
     * Returns default unnamed shell command.
     *
     * @return default unnamed shell command or null if default command is not set
     */
    IShellCommand getDefaultCommand();
    
    /**
     * Finds command by name.
     *
     * @param name command name
     * @return command or null if command is not found
     */
    IShellCommand findCommand(String name);
    
    /**
     * Returns shell command parser.
     *
     * @return shell command parser
     */
    IShellCommandParser getParser();
    
    /**
     * Returns hierarchical name separator character.
     *
     * @return hierarchical name separator character
     */
    char getNameSeparator();
    
    /**
     * Is colors usage disabled?
     *
     * @return true if colors usage is disabled
     */
    boolean isNoColors();
    
    /**
     * Returns usage.
     *
     * @param colorized if true colorized output will be used
     * @return usage
     */
    String getUsage(boolean colorized);
}