/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.property;

import java.io.File;

import com.exametrika.common.config.IConfigurationParser;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;


/**
 * The {@link Properties} contains different utility methods for work with properties.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Properties
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    /**
     * Expands properties. Each property in text defined as ${property_name}.
     *
     * @param path path
     * @param parser configuration parser. Can be null
     * @param propertyResolver property resolver
     * @param check if true all properties must be resolved, if false unresolved properties added as placeholders
     * @param escape if true properties are escaped
     */
    public static void expandProperties(File path, IConfigurationParser parser, IPropertyResolver propertyResolver, boolean check, 
        boolean escape)
    {
        Assert.notNull(path);
        Assert.isTrue(path.exists());
        
        if (path.isFile())
            expandPropertiesFile(path, parser, propertyResolver, check, escape);
        else
            expandPropertiesDir(path, parser, propertyResolver, check, escape);
    }
    
    /**
     * Expands properties. Each property in text defined as ${property_name}.
     *
     * @param parser configuration parser. Can be null
     * @param propertyResolver property resolver
     * @param str string
     * @param check if true all properties must be resolved, if false unresolved properties added as placeholders
     * @param escape if true properties are escaped
     * @return string with expanded properties
     */
    public static String expandProperties(IConfigurationParser parser, IPropertyResolver propertyResolver, String str, boolean check, 
        boolean escape)
    {
        return expandProperties(parser, propertyResolver, str, check, escape, "${", "}");
    }
        
    /**
     * Expands properties. Each property in text defined as ${property_name}.
     *
     * @param parser configuration parser. Can be null
     * @param propertyResolver property resolver
     * @param str string
     * @param check if true all properties must be resolved, if false unresolved properties added as placeholders
     * @param escape if true properties are escaped
     * @param prefix property prefix
     * @param suffix property suffix
     * @return string with expanded properties
     */
    public static String expandProperties(IConfigurationParser parser, IPropertyResolver propertyResolver, String str, boolean check, 
        boolean escape, String prefix, String suffix)
    {
        StringBuilder builder = new StringBuilder();
        
        boolean expanded = false;
        int curPos = 0;
        
        while (curPos < str.length())
        {
            int startPos = str.indexOf(prefix, curPos);
            if (startPos == -1)
            {
                if (expanded)
                    builder.append(str.substring(curPos));
                break;
            }
            int endPos = str.indexOf(suffix, startPos + prefix.length());
            if (endPos == -1)
            {
                if (expanded)
                    builder.append(str.substring(curPos));
                break;
            }
            
            String propertyName = str.substring(startPos + prefix.length(), endPos);
            
            String propertyFlag = null;
            int pos = propertyName.indexOf(';');
            if (pos != -1)
            {
                propertyFlag = propertyName.substring(pos + 1);
                propertyName = propertyName.substring(0, pos);
            }
            
            String propertyDefaultValue = null;
            pos = propertyName.indexOf('=');
            if (pos != -1)
            {
                propertyDefaultValue = propertyName.substring(pos + 1);
                propertyName = propertyName.substring(0, pos);
            }
            
            String propertyValue = propertyResolver.resolveProperty(propertyName);
            if (propertyValue == null)
            {
                if (propertyDefaultValue != null)
                    propertyValue = propertyDefaultValue;
                else if (check)
                    throw new PropertyNotFoundException(messages.propertyNotFound(propertyName));
                else
                    propertyValue = str.substring(startPos, endPos + suffix.length());
            }
            
            if ("noquotes".equals(propertyFlag))
            {
                startPos--;
                endPos++;
            }
            
            builder.append(str.substring(curPos, startPos));
            if (parser == null || !escape || "literal".equals(propertyFlag))
                builder.append(propertyValue);
            else
                builder.append(parser.escape(propertyValue));
            expanded = true;
            curPos = endPos + suffix.length();
        }
     
        if (expanded)
            return builder.toString();
        else
            return str;
    }

    private static void expandPropertiesDir(File dir, IConfigurationParser parser, IPropertyResolver propertyResolver, boolean check, 
        boolean escape)
    {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            File path = files[i];
            if (path.isFile())
                expandPropertiesFile(path, parser, propertyResolver, check, escape);
            else
                expandPropertiesDir(path, parser, propertyResolver, check, escape);
        }
    }

    private static void expandPropertiesFile(File file, IConfigurationParser parser, IPropertyResolver propertyResolver, boolean check, 
        boolean escape)
    {
        String value = Files.read(file);
        value = expandProperties(parser, propertyResolver, value, check, escape);
        Files.write(file, value);
    }
    
    private Properties()
    {
    }
    
    private interface IMessages
    {
        @DefaultMessage("Value is not specified for property ''{0}''.")
        ILocalizedMessage propertyNotFound(String propertyName);
    }
}
