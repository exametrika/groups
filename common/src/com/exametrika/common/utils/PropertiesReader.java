/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;



/**
 * The {@link PropertiesReader} is used to read java properties files in different encodings.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class PropertiesReader
{
    private final boolean spaceSeparator;
    
    public PropertiesReader()
    {
        spaceSeparator = true;
    }
    
    /**
     * Creates a new object.
     *
     * @param spaceSeparator true if space, tab or form feed are key:value separators
     */
    public PropertiesReader(boolean spaceSeparator)
    {
        this.spaceSeparator = spaceSeparator;
    }
    
    /**
     * Reades java properties from specified reader.
     *
     * @param reader reader to read
     * @return java properties map
     * @throws IOException if an error occurred when reading from the reader
     * @throws InvalidArgumentException if the reader contains a malformed Unicode escape sequence.
     */
    public HashMap<String, String> read(Reader reader) throws IOException
    {
        Assert.notNull(reader);
        
        HashMap<String, String> map = new LinkedHashMap<String, String>();
        char[] convtBuf = new char[1024];
        LineReader lr = new LineReader(reader);

        int limit;
        int keyLen;
        int valueStart;
        char c;
        boolean hasSep;
        boolean precedingBackslash;

        while ((limit = lr.readLine()) >= 0)
        {
            c = 0;
            keyLen = 0;
            valueStart = limit;
            hasSep = false;

            precedingBackslash = false;
            while (keyLen < limit)
            {
                c = lr.lineBuf[keyLen];
                // need check if escaped.
                if ((c == '=' || c == ':') && !precedingBackslash)
                {
                    valueStart = keyLen + 1;
                    hasSep = true;
                    break;
                }
                else if (spaceSeparator && (c == ' ' || c == '\t' || c == '\f') && !precedingBackslash)
                {
                    valueStart = keyLen + 1;
                    break;
                }
                if (c == '\\')
                {
                    precedingBackslash = !precedingBackslash;
                }
                else
                {
                    precedingBackslash = false;
                }
                keyLen++;
            }
            while (valueStart < limit)
            {
                c = lr.lineBuf[valueStart];
                if (c != ' ' && c != '\t' && c != '\f')
                {
                    if (!hasSep && (c == '=' || c == ':'))
                    {
                        hasSep = true;
                    }
                    else
                    {
                        break;
                    }
                }
                valueStart++;
            }
            String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
            String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
            map.put(key, value);
        }

        return map;
    }

    private String loadConvert(char[] in, int off, int len, char[] convtBuf)
    {
        if (convtBuf.length < len)
        {
            int newLen = len * 2;
            if (newLen < 0)
            {
                newLen = Integer.MAX_VALUE;
            }
            convtBuf = new char[newLen];
        }
        char aChar;
        char[] out = convtBuf;
        int outLen = 0;
        int end = off + len;

        while (off < end)
        {
            aChar = in[off++];
            if (aChar == '\\')
            {
                aChar = in[off++];
                if (aChar == 'u')
                {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++)
                    {
                        aChar = in[off++];
                        switch (aChar)
                        {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            value = (value << 4) + aChar - '0';
                            break;
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                            value = (value << 4) + 10 + aChar - 'a';
                            break;
                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
                            value = (value << 4) + 10 + aChar - 'A';
                            break;
                        default:
                            throw new InvalidArgumentException();
                        }
                    }
                    out[outLen++] = (char)value;
                }
                else
                {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    out[outLen++] = aChar;
                }
            }
            else
            {
                out[outLen++] = aChar;
            }
        }
        return new String(out, 0, outLen);
    }

    class LineReader
    {
        char[] lineBuf = new char[1024];
        Reader reader;

        public LineReader(Reader reader)
        {
            this.reader = reader;
        }

        int readLine() throws IOException
        {
            int len = 0;
            char c = 0;

            boolean skipWhiteSpace = true;
            boolean isCommentLine = false;
            boolean isNewLine = true;
            boolean appendedLineBegin = false;
            boolean precedingBackslash = false;
            boolean skipLF = false;

            while (true)
            {
                int i = reader.read();
                if (i == -1)
                {
                    if (len == 0 || isCommentLine)
                    {
                        return -1;
                    }
                    return len;
                }

                c = (char)i;
                if (skipLF)
                {
                    skipLF = false;
                    if (c == '\n')
                    {
                        continue;
                    }
                }
                if (skipWhiteSpace)
                {
                    if (c == ' ' || c == '\t' || c == '\f')
                    {
                        continue;
                    }
                    if (!appendedLineBegin && (c == '\r' || c == '\n'))
                    {
                        continue;
                    }
                    skipWhiteSpace = false;
                    appendedLineBegin = false;
                }
                if (isNewLine)
                {
                    isNewLine = false;
                    if (c == '#' || c == '!')
                    {
                        isCommentLine = true;
                        continue;
                    }
                }

                if (c != '\n' && c != '\r')
                {
                    lineBuf[len++] = c;
                    if (len == lineBuf.length)
                    {
                        int newLength = lineBuf.length * 2;
                        if (newLength < 0)
                        {
                            newLength = Integer.MAX_VALUE;
                        }
                        char[] buf = new char[newLength];
                        System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
                        lineBuf = buf;
                    }
                    // flip the preceding backslash flag
                    if (c == '\\')
                    {
                        precedingBackslash = !precedingBackslash;
                    }
                    else
                    {
                        precedingBackslash = false;
                    }
                }
                else
                {
                    // reached EOL
                    if (isCommentLine || len == 0)
                    {
                        isCommentLine = false;
                        isNewLine = true;
                        skipWhiteSpace = true;
                        len = 0;
                        continue;
                    }

                    if (precedingBackslash)
                    {
                        len -= 1;
                        // skip the leading whitespace characters in following line
                        skipWhiteSpace = true;
                        appendedLineBegin = true;
                        precedingBackslash = false;
                        if (c == '\r')
                        {
                            skipLF = true;
                        }
                    }
                    else
                    {
                        return len;
                    }
                }
            }
        }
    }
}
