/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.converters;

import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;

/**
 * String to double converter.
 * 
 * @author andreym
 */
public class DoubleConverter implements IShellParameterConverter
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
            throw new InvalidArgumentException(e);
        }
    }
}