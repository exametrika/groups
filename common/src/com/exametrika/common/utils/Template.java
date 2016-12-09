/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * The {@link Template} is a base class for templates handled by {@link TemplateEngine}.
 * 
 * @author AndreyM
 */
public class Template
{
    protected final Writer writer;

    public Template()
    {
        this(new StringWriter());
    }
    
    public Template(Writer writer)
    {
        Assert.notNull(writer);
        
        this.writer = writer;
    }
    
    public final Writer getWriter()
    {
        return writer;
    }

    @Override
    public String toString()
    {
        return writer.toString();
    }
    
    protected final String delimit(List elements, String delimiter, String prefix, int indent, int width, boolean indentFirst)
    {
        StringBuilder builder = new StringBuilder();
        
        if (!elements.isEmpty())
            builder.append(prefix);
        
        boolean first = true;
        for (Object element : elements)
        {
            if (first)
                first = false;
            else
                builder.append(delimiter);
            
            builder.append(element);
        }
        
        return Strings.wrap(builder.toString(), indent, width, delimiter, indentFirst);
    }
}
