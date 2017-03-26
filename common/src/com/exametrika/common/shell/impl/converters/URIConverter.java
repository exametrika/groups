/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.converters;

import java.net.URI;

import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;

/**
 * String to {@link URI} converter.
 * 
 * @author andreym
 */
public class URIConverter implements IShellParameterConverter
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
            throw new InvalidArgumentException(e);
        }
    }
}