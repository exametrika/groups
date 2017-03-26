/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link IShellParameterConverter} is used to convert string parameter representation to actual parameter value.
 * 
 * @author Medvedev-A
 */
public interface IShellParameterConverter
{
    /**
     * Converts string parameter representation to actual parameter value.
     *
     * @param value string parameter representation
     * @return parameter value
     * @exception InvalidArgumentException if string value can not be converted to parameter value
     */
    Object convert(String value);
}