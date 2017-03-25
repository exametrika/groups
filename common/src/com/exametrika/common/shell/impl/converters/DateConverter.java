/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.converters;

import java.text.DateFormat;
import java.util.Date;

import com.exametrika.common.shell.IParameterConverter;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;

/**
 * String to {@link Date} converter.
 * 
 * @author andreym
 */
public class DateConverter implements IParameterConverter
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
            throw new InvalidArgumentException(e);
        }
    }
}