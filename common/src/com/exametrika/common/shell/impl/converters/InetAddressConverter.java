/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.converters;

import java.net.InetAddress;

import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;

/**
 * String to {@link InetAddress} converter.
 * 
 * @author andreym
 */
public class InetAddressConverter implements IShellParameterConverter
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
            throw new InvalidArgumentException(e);
        }
    }
}