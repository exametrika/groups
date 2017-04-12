/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.exametrika.common.utils.Pair;

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
    
    /**
     * Parses specified arguments.
     *
     * @param args arguments
     * @return map of parsed arguments or null if there are some errors, errors and usage are printed to standard output
     */
    List<Pair<IShellCommand, Map<String, Object>>> parse(String[] args);
    
    /**
     * Returns shell context.
     *
     * @return shell context
     */
    IShellContext getContext();
    
    /**
     * Returns terminal writer.
     *
     * @return terminal writer
     */
    PrintWriter getWriter();
    
    /**
     * Flushes terminal.
     */
    void flush();
    
    /**
     * Executes command with specified parameters.
     *
     * @param commandName command name
     * @param parameters parameters
     * @return result of command execution or null
     */
    Object executeCommand(String commandName, Map<String, Object> parameters);
    
    /**
     * Executes script. Script text contains string respresentations of commands, each command must be on separate line.
     * If command is multiline, interim lines of multiline command must end with '\'. 
     * Strings started with '#' are comments and ignored in command processing
     *
     * @param scriptText script text
     */
    void executeScript(String scriptText);
}