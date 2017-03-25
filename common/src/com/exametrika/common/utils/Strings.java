/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The {@link Strings} contains different utility methods for string manipulation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Strings
{
    private static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    
    /**
     * Is string empty or null?
     *
     * @param value value
     * @return true if string is empty or null
     */
    public static boolean isEmpty(String value)
    {
        if (value == null || value.isEmpty())
            return true;
        else
            return false;
    }
    
    /**
     * Returns non-null value.
     *
     * @param value value
     * @return value
     */
    public static String notNull(String value)
    {
        if (value == null)
            return "";
        else
            return value;
    }
    
    /**
     * Removes double quotes from string.
     * 
     * @param value string
     * @return unquoted string
     */
    public static String unquote(String value)
    {
        if (value.length() <= 2)
            return value;
        if (value.charAt(0) == '\"' && value.charAt(value.length() - 1) == '\"')
            return value.substring(1, value.length() - 1);

        return value;
    }

    /**
     * Indents each new line of specified string on specified number of spaces.
     * 
     * @param value string to indent
     * @param count number of spaces to indent each line
     * @return indented string
     * @exception InvalidArgumentException if count < 0
     */
    public static String indent(String value, int count)
    {
        Assert.notNull(value);

        if (count < 0)
            throw new InvalidArgumentException();

        String indent = duplicate(' ', count);
        return indent(value, indent);
    }
    
    /**
     * Indents each new line of specified string on specified number of spaces.
     * 
     * @param value string to indent
     * @param indent indent string to be inserted on each line
     * @return indented string
     */
    public static String indent(String value, String indent)
    {
        return indent(value, indent, true);
    }
    
    /**
     * Indents each new line of specified string on specified number of spaces.
     * 
     * @param value string to indent
     * @param indent indent string to be inserted on each line
     * @param indentFirst if true first lune is indented
     * @return indented string
     */
    public static String indent(String value, String indent, boolean indentFirst)
    {
        Assert.notNull(value);
        Assert.notNull(indent);

        StringBuilder builder = new StringBuilder();

        boolean newLine = indentFirst;

        for (int i = 0; i < value.length(); i++)
        {
            char ch = value.charAt(i);
            if (ch == '\r' || ch == '\n')
                newLine = true;
            else if (newLine)
            {
                builder.append(indent);
                newLine = false;
            }

            builder.append(ch);
        }

        return builder.toString();
    }

    /**
     * Wraps specified string into multiline of specified width and indents result on specified number of spaces.
     * 
     * @param value string to wrap and indent
     * @param indent number of spaces to indent each line
     * @param width maximal line width of resulting string
     * @return wrapped string
     * @exception InvalidArgumentException if indent or width <= 0
     */
    public static String wrap(String value, int indent, int width)
    {
        Assert.notNull(value);

        if (indent < 0 || width <= 0)
            throw new InvalidArgumentException();

        String indentString = duplicate(' ', indent);

        StringBuilder builder = new StringBuilder();

        boolean newLine = true;
        int count = 0;

        for (int i = 0; i < value.length(); i++)
        {
            if (newLine)
            {
                if (i > 0)
                    builder.append('\n');

                builder.append(indentString);
                newLine = false;
                count = 0;
            }

            char ch = value.charAt(i);
            if (ch == '\r' || ch == '\n')
            {
                newLine = true;
                continue;
            }
            else
            {
                if (count == width - 1)
                    newLine = true;

                builder.append(ch);
                count++;
            }
        }

        return builder.toString();

    }

    /**
     * Wraps specified string into multiline of specified width and indents result on specified number of spaces.
     * 
     * @param value string to wrap and indent
     * @param indent number of spaces to indent each line
     * @param width maximal line width of resulting string
     * @param delimiters line is wrapped on delimiters and specified line width
     * @param indentFirst if true first line is indented, if false is not indented
     * @return wrapped string
     * @exception InvalidArgumentException if indent or width <= 0
     */
    public static String wrap(String value, int indent, int width, String delimiters, boolean indentFirst)
    {
        Assert.notNull(value);
        Assert.notNull(delimiters);

        if (indent < 0 || width <= 0)
            throw new InvalidArgumentException();

        String indentString = duplicate(' ', indent);

        StringBuilder builder = new StringBuilder();

        boolean newLine = true;
        int count = 0;
        int lastDelimiter = -1;
        int lastWritten = -1;

        for (int i = 0; i < value.length(); i++)
        {
            if (newLine)
            {
                if (i > 0)
                    builder.append('\n');

                if (indentFirst || i > 0)
                    builder.append(indentString);
                newLine = false;
                count = 0;
            }

            char ch = value.charAt(i);
            if (ch == '\r' || ch == '\n')
            {
                builder.append(value.substring(lastWritten + 1, i));
                lastDelimiter = -1;
                lastWritten = i;

                newLine = true;
                continue;
            }
            else
            {
                if (delimiters.indexOf(ch) != -1)
                    lastDelimiter = i;

                if (lastDelimiter != -1 && count >= width - 1)
                {
                    if (lastDelimiter != -1)
                        i = lastDelimiter;

                    builder.append(value.substring(lastWritten + 1, i + 1));
                    lastDelimiter = -1;
                    lastWritten = i;

                    newLine = true;
                    continue;
                }

                count++;
            }
        }

        builder.append(value.substring(lastWritten + 1, value.length()));

        return builder.toString();
    }

    /**
     * Makes string by duplicating specified character by specified number of times.
     * 
     * @param value value to duplicate
     * @param count duplicate count
     * @return duplicated string
     * @exception InvalidArgumentException if count < 0
     */
    public static String duplicate(char value, int count)
    {
        if (count < 0)
            throw new InvalidArgumentException();

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < count; i++)
            builder.append(value);

        return builder.toString();
    }

    /**
     * Converts collection to string, making each element on separate line and indenting it.
     * 
     * @param <T> collection type
     * @param collection collection
     * @param indent if true result is indented
     * @return string
     */
    public static <T> String toString(Collection<T> collection, boolean indent)
    {
        Assert.notNull(collection);

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (T element : collection)
        {
            if (first)
                first = false;
            else
                builder.append('\n');

            builder.append(element.toString());
        }
        if (indent)
            return indent(builder.toString(), 4).toString();
        else
            return builder.toString();
    }

    /**
     * Converts glob pattern to regular expression pattern. Glob pattern can have:
     * <li> ? - matches any one character
     * <li> * - matches any number of characters
     * <li> {a,b,c} matches any one of a, b or c (extended pattern)
     * <li> [abc] matches any character in the set a, b or c (extended pattern)
     * <li> [^abc] matches any character not in the set a, b or c (extended pattern)
     * <li> [a-z] matches any character in the range a to z, inclusive (extended pattern)
     * <li> some reg-exp constructs starting with \ (i.e. \d,...)
     * <li> \ escape characters
     *
     * @param str glob pattern
     * @param extended if true extended glob patterns are supported 
     * @return regular expression pattern
     */
    public static String globToRegEx(String str, boolean extended)
    {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        int inCurlies = 0;
        for (int i = 0; i < str.length(); i++)
        {
            char ch = str.charAt(i);
            switch (ch)
            {
            case '*':
                if (escaping)
                    builder.append("\\*");
                else
                    builder.append(".*");
                escaping = false;
                break;
            case '?':
                if (escaping)
                    builder.append("\\?");
                else
                    builder.append('.');
                escaping = false;
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '$':
            case '@':
            case '%':
                builder.append('\\');
                builder.append(ch);
                escaping = false;
                break;
            case '\\':
                if (escaping)
                {
                    builder.append("\\\\");
                    escaping = false;
                }
                else
                    escaping = true;
                break;
            case '{':
                if (extended)
                {
                    if (escaping)
                        builder.append("\\{");
                    else
                    {
                        builder.append('(');
                        inCurlies++;
                    }
                }
                else
                    builder.append("\\{");
                escaping = false;
                break;
            case '}':
                if (extended)
                {
                    if (inCurlies > 0 && !escaping)
                    {
                        builder.append(')');
                        inCurlies--;
                    }
                    else if (escaping)
                        builder.append("\\}");
                    else
                        builder.append("}");
                }
                else
                    builder.append("\\}");
                escaping = false;
                break;
            case ',':
                if (extended)
                {
                    if (inCurlies > 0 && !escaping)
                    {
                        builder.append('|');
                    }
                    else if (escaping)
                        builder.append("\\,");
                    else
                        builder.append(",");
                }
                else
                    builder.append("\\,");
                break;
            case '[':
                if (extended)
                {
                    if (escaping)
                        builder.append("\\[");
                    else
                        builder.append('[');
                }
                else
                    builder.append("\\[");
                escaping = false;
                break;
            case ']':
                if (extended)
                {
                    if (escaping)
                        builder.append("\\]");
                    else
                        builder.append(']');
                }
                else
                    builder.append("\\]");
                escaping = false;
                break;
            default:
                if (escaping)
                    builder.append('\\');
                escaping = false;
                builder.append(ch);
            }
        }
        return builder.toString();
    }
    
    /**
     * Creates filter expression pattern.
     *
     * @param filterExpression filter expression. Filter expression has the following format:
     * glob_pattern | #reg_exp_pattern | ##extended_glob_pattern. Where:
     * <li> glob_pattern in format {@link Strings#globToRegEx}
     * <li> reg_exp_pattern - regular expression pattern   
     * @param caseSensitive true if pattern is case sensitive
     * @return regexp pattern
     */
    public static Pattern createFilterPattern(String filterExpression, boolean caseSensitive)
    {
        if (filterExpression.startsWith("##"))
            filterExpression = Strings.globToRegEx(filterExpression.substring(2), true);
        else if (!filterExpression.isEmpty() && filterExpression.charAt(0) == '#')
            filterExpression = filterExpression.substring(1);
        else
            filterExpression = Strings.globToRegEx(filterExpression, false);
        
        return Pattern.compile(filterExpression, (!caseSensitive ? Pattern.CASE_INSENSITIVE : 0) | Pattern.MULTILINE | Pattern.DOTALL);
    }

    /**
     * Creates filter expression pattern.
     *
     * @param filterExpression filter expression. Filter expression has the following format:
     * glob_pattern | #reg_exp_pattern. Where:
     * <li> glob_pattern in format {@link Strings#globToRegEx}
     * <li> reg_exp_pattern - regular expression pattern   
     * @param caseSensitive true if pattern is case sensitive
     * @return filter condition
     */
    public static ICondition<String> createFilterCondition(String filterExpression, boolean caseSensitive)
    {
        return new StringCondition(createFilterPattern(filterExpression, caseSensitive));
    }
    
    public static String md5Hash(List<String> values)
    {
        Assert.notNull(values);
        
        try 
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (String value : values)
            {
                byte[] buffer = value.getBytes("UTF-8");
                digest.update(buffer);
            }
            
            return digestToString(digest.digest());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates string from specified digest by converting each byte to hex number.
     *
     * @param digest digest
     * @return result string
     */
    public static String digestToString(byte[] digest)
    {
        StringBuffer builder = new StringBuffer();
        for (int i = 0; i < digest.length; i++)
        {
            builder.append(hexDigits[(digest[i] >>> 4) & 0xF]);
            builder.append(hexDigits[digest[i] & 0xF]);
        }
        
        return builder.toString();
    }

    /**
     * Truncates specified string from the end or the begging of the string.
     *
     * @param value truncated value
     * @param length truncation length, if length > 0 value is truncated from the beginning, if length < 0 value is truncated from the end
     * @param ellipsis if true ellipsis is used to mark omissions
     * @return truncated value
     */
    public static String truncate(String value, int length, boolean ellipsis)
    {
        if (length > 0)
        {
            if (value.length() > length)
                return value.substring(0, length) + (ellipsis ? "..." : "");
            else
                return value;
        }
        else
        {
            length = -length;
            
            if (value.length() > length)
                return  (ellipsis ? "..." : "") + value.substring(value.length() - length);
            else
                return value;
        }
    }
    
    /**
     * Shortens name, separated by periods.
     *
     * @param name name with periods
     * @param length resulting length of name with possible collapsed beginning segments. Last segment is never collapsed. 0 means
     * all beginning segments are collapsed
     * @return shortened name
     */
    public static String shorten(String name, int length)
    {
        if (length >= name.length())
            return name;
        
        int pos = -1;
        for (int i = name.length() - 1; i >= 0; i--)
        {
            char ch = name.charAt(i);
            if (ch == '.')
                pos = i;

            if (pos != -1 && (name.length() - i) >= length)
                break;
        }

        pos++;

        if (length == 0)
            return name.substring(pos);

        StringBuilder builder = new StringBuilder();
        boolean segmentStart = true;
        for (int i = 0; i < pos; i++)
        {
            char ch = name.charAt(i);
            if (ch == '.')
                segmentStart = true;
            else if (segmentStart)
            {
                builder.append(ch);
                builder.append('.');
                segmentStart = false;
            }
        }

        builder.append(name.substring(pos));
        return builder.toString();
    }
    
    private Strings()
    {
    }
    
    private static class StringCondition implements ICondition<String>
    {
        private final Pattern pattern;
        
        public StringCondition(Pattern pattern)
        {
            Assert.notNull(pattern);
            
            this.pattern = pattern;
        }
        
        @Override
        public boolean evaluate(String value)
        {
            return pattern.matcher(value).matches();
        }
    }
}
