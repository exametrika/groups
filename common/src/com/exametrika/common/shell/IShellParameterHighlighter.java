/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

/**
 * The {@link IShellParameterHighlighter} is used to highlight parameter value.
 * 
 * @author Medvedev-A
 */
public interface IShellParameterHighlighter
{
    /**
     * Highlights string parameter representation by inserting ansi escape sequences.
     *
     * @param context context
     * @param value string parameter representation
     * @return highlighted parameter value
     */
    String highlight(IShellContext context, String value);
}