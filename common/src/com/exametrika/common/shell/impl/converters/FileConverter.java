/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.converters;

import java.io.File;

import com.exametrika.common.shell.IParameterConverter;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;

/**
 * String to {@link File} converter.
 * 
 * @author andreym
 */
public class FileConverter implements IParameterConverter
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
            throw new InvalidArgumentException(e);
        }
    }
}