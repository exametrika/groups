/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;



/**
 * The {@link CommandLineParser} is a parser for command line.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CommandLineParser
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int INDENT = 4;
    private String title;
    private String usage;
    private IParameterValidator validator;
    private final List<Parameter> parameters = new ArrayList<Parameter>();
    private Parameter unnamedParameter;

    /**
     * The {@link IParameterConverter} is used to convert string parameter representation to actual parameter value.
     * 
     * @author Medvedev-A
     */
    public interface IParameterConverter
    {
        /**
         * Converts string parameter representation to actual parameter value.
         *
         * @param value string parameter representation
         * @return parameter value
         * @exception InvalidArgumentException if string value can not be converter to parameter value
         */
        Object convert(String value);
    }
    
    /**
     * The {@link IParameterValidator} is used to perform additional parameter validation logic.
     * 
     * @author Medvedev-A
     */
    public interface IParameterValidator
    {
        /**
         * Validates command line parameters.
         *
         * @param parameters parameters to validate
         * @exception InvalidArgumentException if parameters are not valid
         */
        void validate(Map<String, Object> parameters);
    }
    
    /**
     * String to integer converter.
     * 
     * @author andreym
     */
    public static class IntegerConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return Integer.valueOf(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.intConversionError(value));
            }
        }
    }
    
    /**
     * String to long converter.
     * 
     * @author andreym
     */
    public static class LongConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return Long.valueOf(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.longConversionError(value));
            }
        }
    }
    
    /**
     * String to double converter.
     * 
     * @author andreym
     */
    public static class DoubleConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return Double.valueOf(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.doubleConversionError(value));
            }
        }
    }
    
    /**
     * String to boolean converter.
     * 
     * @author andreym
     */
    public static class BooleanConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return Boolean.valueOf(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.booleanConversionError(value));
            }
        }
    }
    
    /**
     * String to {@link Date} converter.
     * 
     * @author andreym
     */
    public static class DateConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return DateFormat.getDateInstance().parse(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.dateConversionError(value));
            }
        }
    }
    
    /**
     * String to {@link File} converter.
     * 
     * @author andreym
     */
    public static class FileConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return new File(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.fileConversionError(value));
            }
        }
    }
    
    /**
     * String to {@link URI} converter.
     * 
     * @author andreym
     */
    public static class URIConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return new URI(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.uriConversionError(value));
            }
        }
    }
    
    /**
     * String to {@link InetAddress} converter.
     * 
     * @author andreym
     */
    public static class InetAddressConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                return InetAddress.getByName(Strings.unquote(value));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.inetAddressConversionError(value));
            }
        }
    }
    
    /**
     * String to {@link InetSocketAddress} converter.
     * 
     * @author andreym
     */
    public static class InetSocketAddressConverter implements IParameterConverter
    {
        @Override
        public Object convert(String value)
        {
            try
            {
                value = Strings.unquote(value);
                
                int pos = value.indexOf(':');
                if (pos == -1)
                    throw new InvalidArgumentException(messages.inetSocketAddressConversionError(value));
                
                return new InetSocketAddress(InetAddress.getByName(value.substring(0, pos)), 
                    Integer.valueOf(value.substring(pos + 1)));
            }
            catch (Exception e)
            {
                throw new InvalidArgumentException(messages.inetSocketAddressConversionError(value));
            }
        }
    }
    
    /**
     * Sets program title.
     *
     * @param title program title
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    /**
     * Sets program usage.
     *
     * @param usage program usage
     */
    public void setUsage(String usage)
    {
        this.usage = usage;
    }
    
    /**
     * Sets validator.
     *
     * @param validator parameter validator 
     */
    public void setValidator(IParameterValidator validator)
    {
        this.validator = validator;
    }
    
    /**
     * Defines command line parameter without argument.
     *
     * @param key parameter key
     * @param paramNames list of parameter names
     * @param format parameter format
     * @param description parameter description
     * @param required is parameter required?
     */
    public void defineParameter(String key, String[] paramNames, String format, String description, boolean required)
    {
        defineParameter(key, paramNames, format, description, required, false, true, null, null);
    }
    
    /**
     * Defines command line parameter.
     *
     * @param key parameter key
     * @param paramNames list of parameter names
     * @param format parameter format
     * @param description parameter description
     * @param required is parameter required
     * @param hasArgument does parameter have an argument?
     * @param unique is parameter unique? Parameter must be unique if it does not have an argument
     * @param converter parameter converter. Must not be specified if parameter does not have an argument. If converter
     * is not specified, {@link String} value type parameter is assumed. Parameters without arguments always have
     * null as parameter value
     * @param defaultValue parameter default value. Must not be specified if parameter does not have an argument or
     * parameter is required. If default value has type {@link String} and converter is specified, converter is used
     * to convert default value
     */
    public void defineParameter(String key, String[] paramNames, String format, String description, boolean required, 
        boolean hasArgument, boolean unique, IParameterConverter converter, Object defaultValue)
    {
        Assert.notNull(key);
        Assert.notNull(paramNames);
        Assert.notNull(format);
        Assert.notNull(description);
        
        if (!hasArgument && (converter != null || !unique || defaultValue != null))
            throw new InvalidArgumentException(messages.noArgumentConverterNonUniqueDefaultValueError(key));
        if (required && defaultValue != null)
            throw new InvalidArgumentException(messages.requiredDefaultValueError(key));
        
        Parameter parameter = new Parameter(key, paramNames, format, description, hasArgument, converter, unique, required, defaultValue);
        parameters.add(parameter);
    }
    
    /**
     * Defines unnamed command line parameter.
     *
     * @param key parameter key
     * @param format parameter format
     * @param description parameter description
     * @param required is parameter required
     * @param unique is parameter unique
     * @param converter parameter converter
     * @param defaultValue parameter default value
     */
    public void defineUnnamedParameter(String key, String format, String description, boolean required, 
        boolean unique, IParameterConverter converter, Object defaultValue)
    {
        Assert.notNull(key);
        Assert.notNull(format);
        Assert.notNull(description);

        if (required && defaultValue != null)
            throw new InvalidArgumentException(messages.requiredDefaultValueError(key));
        
        unnamedParameter = new Parameter(key, null, format, description, true, converter, unique, required, defaultValue);
    }
    
    /**
     * Parses program argument list and returns map of [parameter key:parameter value] pairs.
     *
     * @param args list of program arguments
     * @param parameters map of parameters to fill with [parameter key:parameter value] entries
     * @exception InvalidArgumentException if command line parameters are not valid
     */
    public void parse(String[] args, Map<String, Object> parameters)
    {
        try
        {
            parseParameters(Collections.toList(args), parameters);
        }
        catch (InvalidArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InvalidArgumentException(e);
        }
    }
    
    /**
     * Parses program argument list and returns map of [parameter key:parameter value] pairs. Writes program title to specified
     * print stream, when parse is successful. Writes program title, program usage and error to specified print stream when parse failed.
     *
     * @param args list of program arguments
     * @param parameters map of parameters to fill with [parameter key:parameter value] pairs
     * @param out print stream to write
     * @return true if argument list has been parsed successfully
     */
    public boolean parse(String[] args, Map<String, Object> parameters, PrintStream out)
    {
        Assert.notNull(parameters);
        Assert.notNull(out);
        
        try
        {
            printTitle(out);
            
            parseParameters(Collections.toList(args), parameters);
            
            return true;
        }
        catch (Exception e)
        {
            // Suppress exception
            printUsage(out);
            printError(out, e.getMessage());

            out.print("\n");

            return false;
        }
    }
    
    /**
     * Writes program title and program usage to specified print stream.
     *
     * @param out print stream to write
     */
    public void printUsage(PrintStream out)
    {
        Assert.notNull(out);
        
        if (usage != null)
        {
            if (title != null)
                out.print("\n\n");
            
            out.print(usage);
        }
        
        if (unnamedParameter != null)
        {
            if (title != null || usage != null)
                out.print("\n\n");
            
            printParameter(out, unnamedParameter);
        }
        
        if (!parameters.isEmpty())
        {
            if (title != null || usage != null || unnamedParameter != null)
                out.print("\n\n");
            
            boolean first = true;
            for (Parameter parameter : parameters)
            {
                if (first)
                    first = false;
                else
                    out.print("\n");
                
                printParameter(out, parameter);
            }
        }
    }

    public void printError(PrintStream out, String message)
    {
        if (title != null || usage != null || unnamedParameter != null || !parameters.isEmpty())
            out.print("\n\n");
        out.print(messages.errorMessage(message).getString());
    }

    private void printTitle(PrintStream out)
    {
        if (title != null)
            out.print(title);    
    }
    
    private void printParameter(PrintStream out, Parameter parameter)
    {
        out.print(indent(parameter.format, INDENT, true));
        
        if (parameter.format.length() < INDENT * 2)
        {
            out.print(getIndent(INDENT * 2 - parameter.format.length()));
            out.print(indent(parameter.description, INDENT * 3, false));
        }
        else
        {
            out.print("\n");
            out.print(indent(parameter.description, INDENT * 3, true));
        }
    }

    private void parseParameters(List<String> args, Map<String, Object> parameterMap)
    {
        Map<String, Object> parsedParameters = new LinkedHashMap<String, Object>();
        
        for (Parameter parameter : parameters)
            parseParameter(parameter, args, parsedParameters);
        
        if (unnamedParameter != null)
            parseParameter(unnamedParameter, args, parsedParameters);
        
        if (args.size() == 1)
            throw new InvalidArgumentException(messages.unrecognizedOption(args.get(0)));
        else if (args.size() > 1)
        {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String name : args)
            {
                if (first)
                    first = false;
                else
                    builder.append(", ");
                
                builder.append(name);
            }
            
            throw new InvalidArgumentException(messages.unrecognizedOptions(builder));
        }
        
        if (validator != null)
            validator.validate(parsedParameters);
        
        parameterMap.putAll(parsedParameters);
    }
    
    private void parseParameter(Parameter parameter, List<String> args, Map<String, Object> parameterMap)
    {
        List<Object> values = null;
        if (!parameter.unique)
            values = new ArrayList<Object>();
        
        Object object = null;
        
        while (true)
        {
            int i = findParameter(parameter.names, args);
            if (i != -1)
            {
                if (parameter.unique && object != null)
                    throw new InvalidArgumentException(messages.duplicateOptionFound(parameter.format));
                
                String value = args.remove(i);
                if (parameter.hasArgument && parameter.names != null)
                {
                    if (i >= args.size() || args.get(i).charAt(0) == '-')
                        throw new InvalidArgumentException(messages.optionArgumentNotFound(parameter.format));
                    value = args.remove(i);
                }
                
                if (parameter.converter != null)
                    object = parameter.converter.convert(value);
                else
                    object = value;
                
                if (parameter.unique)
                    parameterMap.put(parameter.key, parameter.hasArgument ? object : null);
                else
                    values.add(object);
            }
            else 
            {
                if (parameter.unique)
                {
                    if (object != null)
                        break;
                    if (!parameter.required)
                    {
                        if (parameter.hasArgument)
                            parameterMap.put(parameter.key, parameter.getDefaultValue());
                        break;
                    }
                    
                    throw new InvalidArgumentException(messages.requiredOptionNotFound(parameter.format));
                }

                if (values.isEmpty() && !parameter.required)
                    values.add(parameter.getDefaultValue());
                if (values.isEmpty() && parameter.required)
                    throw new InvalidArgumentException(messages.requiredOptionNotFound(parameter.format));
                
                parameterMap.put(parameter.key, values);
                break;
            }
        }
    }
    
    private int findParameter(String[] names, List<String> args)
    {
        for (int i = 0; i < args.size(); i++)
        {
            String arg = args.get(i);
            if (names != null)
            {
                for (String name : names)
                {
                    if (arg.equals(name))
                        return i;
                }
            }
            else
            {
                if (arg.charAt(0) != '-')
                    return i;
            }
        }
        
        return -1;
    }
    
    private String getIndent(int count)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++)
            builder.append(' ');
        
        return builder.toString();
    }
    
    private String indent(String value, int count, boolean indentFirstLine)
    {
        String indent = getIndent(count);
        String[] parts = value.split("[\n]");
        
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String part : parts)
        {
            if (first)
            {
                first = false;
                
                if (indentFirstLine)
                    builder.append(indent);
            }
            else
            {
                builder.append('\n');
                builder.append(indent);
            }
            
            builder.append(part);
        }
        
        return builder.toString();
    }
    
    private static class Parameter
    {
        String key;
        String[] names;
        String format;
        String description;
        boolean hasArgument;
        IParameterConverter converter;
        boolean unique;
        boolean required;
        Object defaultValue;
        
        public Parameter(String key, String[] names, String format, String description, boolean hasArgument, 
            IParameterConverter converter, boolean unique, boolean required, Object defaultValue)
        {
            this.key = key;
            this.names = names;
            this.format = format;
            this.description = description;
            this.hasArgument = hasArgument;
            this.converter = converter;
            this.unique = unique;
            this.required = required;
            this.defaultValue = defaultValue;
        }
        
        public Object getDefaultValue()
        {
            if (converter != null && defaultValue instanceof String)
                return converter.convert((String)defaultValue);

            return defaultValue;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Error: {0}")
        ILocalizedMessage errorMessage(Object error);
        @DefaultMessage("Unrecognized option ''{0}'' specified.")
        ILocalizedMessage unrecognizedOption(Object option);
        @DefaultMessage("Unrecognized options ''{0}'' specified.")
        ILocalizedMessage unrecognizedOptions(Object options);
        @DefaultMessage("Required option ''{0}'' is not found.")
        ILocalizedMessage requiredOptionNotFound(Object format);
        @DefaultMessage("Argument of option ''{0}'' is not found.")
        ILocalizedMessage optionArgumentNotFound(Object option);
        @DefaultMessage("Duplicate option ''{0}'' is found.")
        ILocalizedMessage duplicateOptionFound(Object option);
        @DefaultMessage("Converter, non-uniqueness or default value must not be specified for parameter without argument ''{0}''.")
        ILocalizedMessage noArgumentConverterNonUniqueDefaultValueError(Object parameter);
        @DefaultMessage("Default value must not be specified for required parameter ''{0}''.")
        ILocalizedMessage requiredDefaultValueError(Object parameter);
        @DefaultMessage("Can not convert from ''{0}'' to integer value.")
        ILocalizedMessage intConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to long value.")
        ILocalizedMessage longConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to double value.")
        ILocalizedMessage doubleConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to boolean value.")
        ILocalizedMessage booleanConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to date value.")
        ILocalizedMessage dateConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to file value.")
        ILocalizedMessage fileConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to uri value.")
        ILocalizedMessage uriConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to network address value.")
        ILocalizedMessage inetAddressConversionError(Object value);
        @DefaultMessage("Can not convert from ''{0}'' to network socket address value.")
        ILocalizedMessage inetSocketAddressConversionError(Object value);
    }
}
