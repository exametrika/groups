/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.converters;

import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;

/**
 * String to long converter.
 * 
 * @author andreym
 */
public class LongConverter implements IShellParameterConverter
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
            throw new InvalidArgumentException(e);
        }
    }
}