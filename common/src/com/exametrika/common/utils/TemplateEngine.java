/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link TemplateEngine} is a generator of templates based on {@link Template}. Template
 * is an ordinary java class with embedded delimiters { ...some text... } enclosed in multiline
 * java comments at the beginning of line that mark begin and end of raw text, lines with delimiters are eliminated from target template.  
 * Text can contain java expressions - ${expression}. 
 * 
 * @author AndreyM
 */
public final class TemplateEngine
{
    /**
     * Creates text of template java file. 
     *
     * @param templateText template text
     * @return text of template java file
     */
    public String createTemplate(String templateText)
    {
        Assert.notNull(templateText);
        
        Reader reader = null;
        try
        {
            reader = new StringReader(templateText);
            return createTemplate(reader);
        }
        finally
        {
            IOs.close(reader);
        }
    }

    /**
     * Creates text of template java file. 
     *
     * @param templateFile template file
     * @return text of template java file
     */
    public String createTemplate(File templateFile)
    {
        Assert.notNull(templateFile);
        
        Reader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(templateFile));
            return createTemplate(reader);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(reader);
        }
    }

    /**
     * Creates text of template java file. 
     *
     * @param templateUrl template URL
     * @return text of template java file
     */
    public String createTemplate(URL templateUrl)
    {
        Assert.notNull(templateUrl);
        
        InputStream stream = null;
        Reader reader = null;
        try
        {
            stream = templateUrl.openStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            return createTemplate(reader);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(reader);
            IOs.close(stream);
        }
    }

    /**
     * Creates text of template java file. 
     *
     * @param templateReader template reader
     * @return text of template java file
     */
    public String createTemplate(Reader templateReader)
    {
        Assert.notNull(templateReader);
        
        BufferedReader reader;
        if (templateReader instanceof BufferedReader)
            reader = (BufferedReader)templateReader;
        else
            reader = new BufferedReader(templateReader);

        StringWriter writer = new StringWriter();
        
        try
        {
            int indent = 0;
            boolean textStarted = false;
            boolean inMultiLineComment = false;
            while (true)
            {
                String line = reader.readLine();
                if (line == null)
                    break;
                
                if (!textStarted)
                {
                    if (line.startsWith("/*{"))
                        textStarted = true;
                    else
                    {
                        writer.write(line);
                        writer.write('\n');
                        
                        for (int i = 0; i < line.length(); i++)
                        {
                            char c = line.charAt(i);
                            if (!inMultiLineComment)
                            {
                                if (i < line.length() - 1)
                                {
                                    if (c == '/' && line.charAt(i + 1) == '*')
                                    {
                                        inMultiLineComment = true;
                                        i++;
                                        continue;
                                    }
                                    else if (c == '/' && line.charAt(i + 1) == '/')
                                        break;
                                }
                                
                                if (c == '{')
                                    indent++;
                                else if (c == '}')
                                    indent--;
                            }
                            else if (i < line.length() - 1 && c == '*' && line.charAt(i + 1) == '/')
                            {
                                inMultiLineComment = false;
                                i++;
                                continue;
                            }
                        }
                    }
                }
                else if(line.startsWith("}*/"))
                    textStarted = false;
                else 
                {
                    writer.write(Strings.indent("//" + line + '\n', indent * 4));
                    writer.write(Strings.duplicate(' ', indent * 4));
                    
                    List<String> elements = new ArrayList<String>();
                    int elementStart = 0;
                    boolean inExpression = false;
                    for (int i = 0; i < line.length(); i++)
                    {
                        char c = line.charAt(i);
                        
                        if (!inExpression)
                        {
                            if (i < line.length() - 1)
                            {
                                if (c == '$' && line.charAt(i + 1) == '{')
                                {
                                    String element = line.substring(elementStart, i);
                                    if (!element.isEmpty())
                                        elements.add('\"' + element + '\"');
                                    
                                    inExpression = true;
                                    i++;
                                    elementStart = i + 1;
                                    continue;
                                }
                            }
                        }
                        else if (c == '}')
                        {
                            inExpression = false;
                            String element = line.substring(elementStart, i);
                            if (!element.isEmpty())
                                elements.add(element);
                            
                            elementStart = i + 1;
                            continue;
                        }
                    }
                    
                    Assert.isTrue(!inExpression);
                    
                    String element = line.substring(elementStart, line.length());
                    if (!element.isEmpty())
                        elements.add('\"' + element + '\"');
                    
                    if (!elements.isEmpty())
                    {
                        writer.write("writer.write(");
                        
                        boolean first = true;
                        for (String e : elements)
                        {
                            if (first)
                                first = false;
                            else
                                writer.write(" + ");
                            
                            writer.write(e);
                        }
                        
                        writer.write(" + \'\\n\');\n");
                    }
                }
            }

            return writer.toString();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
