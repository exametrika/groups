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
     * Does parameter have argument?
     *
     * @return true if parameter has argument
     */
    boolean hasArgument();
    
    /**
     * Returns parameter converter.
     *
     * @return parameter converter or null if converter is not set
     */
    IShellParameterConverter getConverter();
    
    /**
     * Is parameter unique?
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
     * Returns parameter default value.
     *
     * @return parameter default value or null if default value is not set
     */
    Object getDefaultValue();
    
    /**
     * Returns parameter completer.
     *
     * @return parameter completer or null if default completion is used
     */
    IShellParameterCompleter getCompleter();
    
    /**
     * Returns parameter highlighter.
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