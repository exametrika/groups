/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.jline.utils.AttributedStringBuilder;

import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellPromptProvider;



/**
 * The {@link DefaultShellPromptProvider} is a default shell prompt provider.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class DefaultShellPromptProvider implements IShellPromptProvider
{
    @Override
    public String[] getPrompt(IShellContext context)
    {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        if (!context.getShell().isNoColors())
            builder.style(ShellStyles.LEFT_PROMPT_STYLE);
        builder.append(context.getPath() + ">");
        String leftPrompt = builder.toAnsi();

        builder = new AttributedStringBuilder();
        if (!context.getShell().isNoColors())
            builder.style(ShellStyles.RIGHT_PROMPT_STYLE);
        builder .append(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
            .append("\n")
            .append(LocalTime.now().format(new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .toFormatter()));
        String rightPrompt = builder.toAnsi();
        
        return new String[]{leftPrompt, rightPrompt};
    }
}
