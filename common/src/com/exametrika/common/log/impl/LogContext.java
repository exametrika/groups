/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LoggingContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;

/**
 * The {@link LogContext} is a context with set of functions used in log filters and log templates.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev_A
 */
public final class LogContext
{
    private static final IMessages messages = Messages.get(IMessages.class);
    public static final String n = System.getProperty("line.separator");
    public static final String separator = n; 
    
    public static final int s = 0;
    public static final int shortStyle = 0;
    public static final int m = 1;
    public static final int mediumStyle = 1;
    public static final int l = 2;
    public static final int longStyle = 2;
    public static final int f = 3;
    public static final int fullStyle = 3;

    public static final int ms = 4;
    public static final int milliseconds = 5;
    public static final int sec = 6;
    public static final int seconds = 7;
    public static final int min = 8;
    public static final int minutes = 9;
    public static final int h = 10;
    public static final int hours = 11;
    public static final int d = 12;
    public static final int days = 13;
    
    public static final int normal = 0;
    public static final int bright = 1;
    public static final int dim = 2;
    public static final int underline = 4;  
    public static final int blink = 5;
    public static final int negative = 7;
    public static final int strikethrough = 9;
    public static final int black = 30;
    public static final int red = 31;
    public static final int green = 32;
    public static final int yellow = 33;
    public static final int blue = 34;
    public static final int magenta = 35;
    public static final int cyan = 36;
    public static final int white = 37;
    public static final int bgBlack = 40;
    public static final int bgRed = 41;
    public static final int bgGreen = 42;
    public static final int bgYellow = 43;
    public static final int bgBlue = 44;
    public static final int bgMagenta = 45;
    public static final int bgCyan = 46;
    public static final int bgWhite = 47;
    public static final int highlight = 100;

    private static final String ESC_PREFIX = "\u001b[";
    private static final int CALLER_STACK_OFFSET = 3;

    private LogEvent event;
    private boolean colorize;
    private final Map<String, Pattern> filterPatterns = new HashMap<String, Pattern>();
    private final DateFormat dateFormat = DateFormat.getDateInstance();
    private final DateFormat[] dateFormats = new DateFormat[4];
    private final DateFormat timeFormat = DateFormat.getTimeInstance();
    private final DateFormat[] timeFormats = new DateFormat[4];
    private final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
    private final DateFormat[] dateTimeFormats = new DateFormat[16];
    private final Map<String, DateFormat> dateFormatsMap = new HashMap<String, DateFormat>();

    LogContext()
    {
        int[] styles = new int[]{DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
        for (int i = 0; i < 4; i++)
        {
            dateFormats[i] = DateFormat.getDateInstance(styles[i]);
            timeFormats[i] = DateFormat.getTimeInstance(styles[i]);
        }
        
        for (int i = 0; i < 4; i++)
            for (int k = 0; k < 4; k++)
        {
            dateTimeFormats[i * 4 + k] = DateFormat.getDateTimeInstance(styles[i], styles[k]);
        }
    }
    
    void setEvent(LogEvent event)
    {
        this.event = event;
    }
    
    void setColorize(boolean value)
    {
        colorize = value;
    }

    public LogEvent getEvent()
    {
        return event;
    }

    public String getLogger()
    {
        return event.getLogger();
    }

    public String logger(int length)
    {
        Assert.checkState(length >= 0);

        return Strings.shorten(event.getLogger(), length);
    }

    public String getLevel()
    {
        return event.getLevel().toString();
    }

    public String getMarker()
    {
        if (event.getMarker() != null)
            return event.getMarker().toString();
        else
            return "";
    }

    public String getMessage()
    {
        if (event.getMessage() != null)
            return event.getMessage().toString();
        else
            return "";
    }

    public String getException()
    {
        return getExceptionStackTrace(Integer.MAX_VALUE, false, messages.causedBy().toString());
    }
    
    public String exception(int size)
    {
        return getExceptionStackTrace(size, false, messages.causedBy().toString());
    }
    
    public String getRootException()
    {
        return getExceptionStackTrace(Integer.MAX_VALUE, true, messages.causes().toString());
    }
    
    public String rootException(int size)
    {
        return getExceptionStackTrace(size, true, messages.causes().toString());
    }

    public String getThread()
    {
        return event.getThread();
    }

    public Map<String, Object> getContext()
    {
        return LoggingContext.getContext();
    }
    
    public String getClassName()
    {
        StackTraceElement caller = getCallerStackTraceElement();
        if (caller != null)
            return caller.getClassName();
        else
            return ""; 
    }
    
    public String className(int length)
    {
        StackTraceElement caller = getCallerStackTraceElement();
        if (caller != null)
            return Strings.shorten(caller.getClassName(), length);
        else
            return "";
    }
    
    public String getMethod()
    {
        StackTraceElement caller = getCallerStackTraceElement();
        if (caller != null)
            return caller.getMethodName();
        else
            return ""; 
    }
    
    public String getFile()
    {
        StackTraceElement caller = getCallerStackTraceElement();
        if (caller != null)
            return caller.getFileName();
        else
            return ""; 
    }
    
    public String getLine()
    {
        StackTraceElement caller = getCallerStackTraceElement();
        if (caller != null)
            return Integer.toString(caller.getLineNumber());
        else
            return ""; 
    }
    
    public String getCaller()
    {
        return caller(Integer.MAX_VALUE);
    }

    public String caller(int size)
    {
        if (event.getStackTrace() != null)
        {
            StringBuilder builder = new StringBuilder();
            printStackTrace(builder, event.getStackTrace(), CALLER_STACK_OFFSET, size);
            return builder.toString();
        }
        else
            return "";
    }

    public String getRelative()
    {
        return Long.toString(getRelativeTime()) + messages.milliseconds();
    }
    
    public String relative(int units)
    {
        long time = getRelativeTime();
        
        StringBuilder builder = new StringBuilder();
        switch (units)
        {
        case d:
        case days:
            builder.append(time / 79800000).append(messages.days()).append(' ');
            time %= 79800000;
        case h:
        case hours:
            builder.append(time / 3600000).append(messages.hours()).append(' ');
            time %= 3600000;
        case m:
        case min:
        case minutes:
            builder.append(time / 60000).append(messages.minutes()).append(' ');
            time %= 60000;
        case s:
        case sec:
        case seconds:
            builder.append(time / 1000).append(messages.seconds()).append(' ');
            time %= 1000;
        case ms:
        case milliseconds:
            builder.append(time).append(messages.milliseconds());
            break;
        default:
            Assert.isTrue(false);
        }
        return builder.toString();
    }
    
    public String getDate()
    {
        return dateFormat.format(new Date(event.getTime()));
    }
    
    public String date(int style)
    {
        return dateFormats[style].format(new Date(event.getTime()));
    }
    
    public String date(String pattern)
    {
        return getDateFormat(pattern).format(new Date(event.getTime()));
    }
    
    public String getTime()
    {
        return timeFormat.format(new Date(event.getTime()));
    }
    
    public String time(int style)
    {
        return timeFormats[style].format(new Date(event.getTime()));
    }
    
    public String getDateTime()
    {
        return dateTimeFormat.format(new Date(event.getTime()));
    }
    
    public String dateTime(int dateStyle, int timeStyle)
    {
        return dateTimeFormats[dateStyle * 4 + timeStyle].format(new Date(event.getTime()));
    }
    
    public String pad(String value, int length)
    {
        if (value == null)
            value = "";
        
        if (length > 0)
        {
            if (value.length() < length)
                return value + Strings.duplicate(' ', length - value.length());
            else
                return value;
        }
        else
        {
            length = -length;
            
            if (value.length() < length)
                return Strings.duplicate(' ', length - value.length()) + value;
            else
                return value;
        }
    }
    
    public String truncate(String value, int length)
    {
        if (value == null)
            value = "";
        
        if (length > 0)
        {
            if (value.length() > length)
                return value.substring(0, length);
            else
                return value;
        }
        else
        {
            length = -length;
            
            if (value.length() > length)
                return value.substring(value.length() - length);
            else
                return value;
        }
    }
    
    public String norm(String value, int padLength, int truncateLength)
    {
        return truncate(pad(value, padLength), truncateLength);
    }
    
    public boolean filter(String pattern, String value)
    {
        if (value == null)
            value = "";
        
        Pattern filterPattern = filterPatterns.get(pattern);
        if (filterPattern == null)
        {
            filterPattern = Strings.createFilterPattern(pattern, false);
            filterPatterns.put(pattern, filterPattern);
        }

        return filterPattern.matcher(value).matches();
    }

    public String color(int... colors)
    {
        if (!colorize)
            return "";

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < colors.length; i++)
        {
            if (first)
                first = false;
            else
                builder.append(';');

            int color = colors[i];
            if (color == highlight)
                builder.append(getHighlighColor());
            else
                builder.append(color);
        }
        
        return ESC_PREFIX + builder + "m";
    }
    
    private String getExceptionStackTrace(int size, boolean root, String caption)
    {
        if (event.getException() != null)
        {
            StringBuilder builder = new StringBuilder();
            
            List<Throwable> exceptions = getExceptionList(event.getException(), root);
            for (int i = 0; i < exceptions.size(); i++)
            {
                Throwable exception = exceptions.get(i);
                int count = size;
                
                if (i > 0)
                    builder.append(caption);
                
                if ((!root && i > 0) || (root && i < exceptions.size() - 1))
                {
                    int m = getPrintStackTraceSize(!root ? exceptions.get(i - 1) : exceptions.get(i + 1), 
                        exception);
                    
                    if (count > m)
                        count = m;
                }
                
                builder.append(exception).append(n);
                
                printStackTrace(builder, exception.getStackTrace(), 0, count);
            }
            
            return builder.toString();
        }
        else
            return "";
    }

    private void printStackTrace(StringBuilder builder, StackTraceElement[] trace, int offset, int count)
    {
        count = Math.min(trace.length, count + offset);
        
        for (int i = offset; i < count; i++)
            builder.append(messages.at()).append(trace[i]).append(n);
        
        if (count < trace.length)
            builder.append("\t... ").append(trace.length - count).append(messages.more()).append(n);
    }
    
    private List<Throwable> getExceptionList(Throwable e, boolean root)
    {
        List<Throwable> list = new ArrayList<Throwable>();
        list.add(e);
        
        while (e.getCause() != null)
        {
            e = e.getCause();
            list.add(e);
        }
        
        if (root)
            Collections.reverse(list);
        
        return list;
    }
    
    private int getPrintStackTraceSize(Throwable prevException, Throwable exception)
    {
        StackTraceElement[] trace = exception.getStackTrace();
        StackTraceElement[] prevTrace = prevException.getStackTrace();

        int m = trace.length - 1;
        int n = prevTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(prevTrace[n]))
        {
            m--;
            n--;
        }
        
        return m + 1;
    }
    
    private StackTraceElement getCallerStackTraceElement()
    {
        if (event.getStackTrace() != null)
        {
            StackTraceElement[] trace = event.getStackTrace();
            if (trace.length >= CALLER_STACK_OFFSET)
                return trace[CALLER_STACK_OFFSET];
        }

        return null;
    }
    
    private long getRelativeTime()
    {
        return event.getTime() - ManagementFactory.getRuntimeMXBean().getStartTime();
    }
    
    private DateFormat getDateFormat(String pattern)
    {
        DateFormat format = dateFormatsMap.get(pattern);
        if (format == null)
        {
            format = new SimpleDateFormat(pattern);
            dateFormatsMap.put(pattern, format);
        }
        
        return format;
    }
 
    private int getHighlighColor()
    {
        switch (event.getLevel())
        {
        case OFF:
        case ERROR:
            return red;
        case WARNING:
            return yellow;
        case INFO:
            return cyan;
        case DEBUG:
            return normal;
        case TRACE:
        case ALL:
            return dim;
        default:
            return Assert.error();
        }
    }

    private interface IMessages
    {
        @DefaultMessage("Caused by: ")
        ILocalizedMessage causedBy();
        @DefaultMessage("Causes: ")
        ILocalizedMessage causes();
        @DefaultMessage("\tat ")
        ILocalizedMessage at();
        @DefaultMessage(" more")
        ILocalizedMessage more();
        @DefaultMessage("d")
        ILocalizedMessage days();
        @DefaultMessage("h")
        ILocalizedMessage hours();
        @DefaultMessage("m")
        ILocalizedMessage minutes();
        @DefaultMessage("s")
        ILocalizedMessage seconds();
        @DefaultMessage("ms")
        ILocalizedMessage milliseconds();
    }
}