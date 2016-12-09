package com.exametrika.common.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Strings;

/**
 * The {@link JsonWriter} is JSON text writer. Writer supports full JSON specification and optional text formatting.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public final class JsonWriter implements IJsonHandler, Closeable
{
    private final Writer writer;
    private final int indent;
    private final NumberFormat format;
    private boolean comma;
    private int indentLevel;
    private boolean nested;

    public JsonWriter(Writer writer)
    {
        this(writer, 0, createDefaultNumberFormat());
    }
    
    public JsonWriter(Writer writer, int indent)
    {
        this(writer, indent, createDefaultNumberFormat());
    }
    
    public JsonWriter(Writer writer, int indent, NumberFormat format)
    {
        Assert.notNull(writer);
        Assert.notNull(format);

        this.writer = writer;
        this.indent = indent;
        this.format = format;
    }
    
    @Override
    public void startText()
    {
    }
    
    @Override
    public void endText()
    {
    }

    @Override
    public void startObject()
    {
        if (comma)
            write(',');
        
        if (indent > 0 && indentLevel > 0)
        {
            write('\n');
            writeIndent();
        }
        
        write('{');
        comma = false;
        indentLevel++;
    }

    @Override
    public void endObject()
    {
        indentLevel--;
        
        if (indent > 0)
        {
            write('\n');
            writeIndent();
        }
        
        write('}');
        comma = true;
        nested = true;
    }

    @Override
    public void startArray()
    {
        if (comma)
            write(',');
        
        if (indent > 0 && indentLevel > 0)
        {
            write('\n');
            writeIndent();
        }
        
        write('[');
        comma = false;
        indentLevel++;
        nested = false;
    }

    @Override
    public void endArray()
    {
        indentLevel--;
        
        if (nested && indent > 0)
        {
            write('\n');
            writeIndent();
        }

        write(']');
        comma = true;
        nested = true;
    }

    @Override
    public void key(String key)
    {
        Assert.notNull(key);

        if (comma)
            write(',');

        if (indent > 0)
        {
            write('\n');
            writeIndent();
        }
        
        writeQuoted(key);
        write(':');
        
        if (indent > 0)
            write(' ');
        
        comma = false;
    }

    @Override
    public void value(Object value)
    {
        if (comma)
        {
            write(',');
            
            if (indent > 0)
                write(' ');
        }
        
        if (value instanceof Double)
        {
            Double d = (Double)value;
            if (!d.isInfinite() && !d.isNaN())
                write(format.format(value));
            else
                write("null");
        }
        else if (value instanceof Float)
        {
            Float d = (Float)value;
            if (!d.isInfinite() && !d.isNaN())
                write(format.format(value));
            else
                write("null");
        }
        else if (value instanceof Number || value instanceof Boolean)
            write(value.toString());
        else if (value instanceof String)
            writeQuoted((String)value);
        else if (value != null)
            writeQuoted(value.toString());
        else
            write("null");
        
        comma = true;
    }
    
    @Override
    public void close()
    {
        IOs.close(writer);
    }

    private void writeQuoted(String string)
    {
        if (string == null || string.length() == 0)
        {
            write("\"\"");
            return;
        }

        char c = 0;
        String hhhh;

        write('"');
        for (int i = 0; i < string.length(); i++)
        {
            char b = c;
            c = string.charAt(i);
            switch (c)
            {
            case '\\':
            case '"':
                write('\\');
                write(c);
                break;
            case '/':
                if (b == '<')
                    write('\\');

                write(c);
                break;
            case '\b':
                write("\\b");
                break;
            case '\t':
                write("\\t");
                break;
            case '\n':
                write("\\n");
                break;
            case '\f':
                write("\\f");
                break;
            case '\r':
                write("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100'))
                {
                    hhhh = "000" + Integer.toHexString(c);
                    write("\\u" + hhhh.substring(hhhh.length() - 4));
                }
                else
                    write(c);
            }
        }
        write('"');
    }

    private void write(String value)
    {
        try
        {
            writer.write(value);
        }
        catch (IOException e)
        {
            throw new JsonException(e);
        }
    }
    
    private void write(char c)
    {
        try
        {
            writer.write(c);
        }
        catch (IOException e)
        {
            throw new JsonException(e);
        }
    }
    
    private void writeIndent()
    {
        write(Strings.duplicate(' ', indent * indentLevel));
    }
    
    private static NumberFormat createDefaultNumberFormat()
    {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setGroupingUsed(false);
        format.setMaximumIntegerDigits(30);
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(0);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
