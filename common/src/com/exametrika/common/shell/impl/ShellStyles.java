/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import org.jline.utils.AttributedStyle;



/**
 * The {@link ShellStyles} defines a shell styles.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellStyles
{
    public static final AttributedStyle ERROR_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold();
    public static final AttributedStyle WARNING_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold();
    public static final AttributedStyle APPLICATION_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
    public static final AttributedStyle COMMAND_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold();
    public static final AttributedStyle PARAMETER_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold();
    public static final AttributedStyle PARAMETER_ARGUMENT_STYLE = AttributedStyle.DEFAULT;
    public static final AttributedStyle LEFT_PROMPT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    public static final AttributedStyle RIGHT_PROMPT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    public static final AttributedStyle DEFAULT_STYLE = AttributedStyle.DEFAULT;
   
    private ShellStyles()
    {
    }
}
