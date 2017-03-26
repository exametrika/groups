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
import org.jline.utils.AttributedStyle;

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
        String leftPrompt = context.getPath() + ">";
        String rightPrompt = new AttributedStringBuilder()
            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
            .append(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
            .append("\n")
            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
            .append(LocalTime.now().format(new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .toFormatter()))
            .toAnsi(); 
        return new String[]{leftPrompt, rightPrompt};
    }
}
