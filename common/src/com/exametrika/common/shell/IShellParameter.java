/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.List;

/**
 * The {@link IShellParameter} is a shell parameter.
 * 
 * @author Medvedev-A
 */
public interface IShellParameter
{
    /**
     * Returns map key, where parsed parameters will be placed.
     *
     * @return map key, where parsed parameters will be placed
     */
    String getKey();
    
    /**
     * Returns list of parameter names.
     *
     * @return list of parameter names
     */
    List<String> getNames();
    
    /**
     * Returns parameter format.
     *
     * @return parameter format
     */
    String getFormat();
    
    /**
     * Returns parameter description.
     *
     * @return parameter description
     */
    String getDescription();
    
    /**
     * Returns parameter short description to be used in completer.
     *
     * @return parameter short description or null if not set
     */
    String getShortDescription();
    
    /**
     * Does parameter have argument?
     *
     * @return true if parameter has argument
     */
    boolean hasArgument();
    
    /**
     * Returns parameter converter. Must not be specified if parameter does not have an argument. If converter
     * is not specified, {@link String} value type parameter is assumed. Parameters without arguments always have
     * null as parameter value.
     *
     * @return parameter converter or null if converter is not set
     */
    IShellParameterConverter getConverter();
    
    /**
     * Is parameter unique? Parameter must be unique if it does not have an argument.
     *
     * @return true if parameter is unique
     */
    boolean isUnique();
    
    /**
     * Is parameter required?
     *
     * @return true if parameter is required
     */
    boolean isRequired();
    
    /**
     * Returns parameter default value. Must not be specified if parameter does not have an argument or
     * parameter is required. If default value has type {@link String} and converter is specified, converter is used
     * to convert default value
     *
     * @return parameter default value or null if default value is not set
     */
    Object getDefaultValue();
    
    /**
     * Returns parameter completer. Must not be specified if parameter does not have an argument.
     *
     * @return parameter completer or null if default completion is used
     */
    IShellParameterCompleter getCompleter();
    
    /**
     * Returns parameter highlighter. Must not be specified if parameter does not have an argument.
     *
     * @return parameter highlighter or null if default highlighting is used
     */
    IShellParameterHighlighter getHighlighter();
    
    /**
     * Returns parameter usage.
     *
     * @param colorized if true colorized output will be used
     * @return parameter usage
     */
    String getUsage(boolean colorized);
}