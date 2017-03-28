/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.List;

/**
 * The {@link IShellCommand} is a shell command.
 * 
 * @author Medvedev-A
 */
public interface IShellCommand
{
    /**
     * Returns command name. Command name can be hierarchical, where each name segment is separated by {@link IShell#getNameSeparator()}.
     *
     * @return command name
     */
    String getName();
    
    /**
     * Returns command description.
     *
     * @return command description
     */
    String getDescription();
    
    /**
     * Returns command validator.
     *
     * @return command validator or null if validator is not set
     */
    IShellParameterValidator getValidator();
    
    /**
     * Returns named command parameters.
     *
     * @return named command parameters
     */
    List<IShellParameter> getNamedParameters();
    
    /**
     * Returns unnamed positional command parameters.
     *
     * @return unnamed positional command parameters
     */
    List<IShellParameter> getPositionalParameters();
    
    /**
     * Returns default unnamed (last) command parameter.
     *
     * @return default unnamed (last) command parameter or null if parameter is not set
     */
    IShellParameter getDefaultParameter();
    
    /**
     * Returns command executor.
     *
     * @return command executor
     */
    IShellCommandExecutor getExecutor();
    
    /**
     * Returns command usage.
     *
     * @param colorized if true colorized output will be used
     * @return command usage
     */
    String getUsage(boolean colorized);
}