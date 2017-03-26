/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.Map;

import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link IShellParameterValidator} is used to perform additional parameter validation logic.
 * 
 * @author Medvedev-A
 */
public interface IShellParameterValidator
{
    /**
     * Validates command parameters.
     *
     * @param parameters parameters to validate
     * @exception InvalidArgumentException if parameters are not valid
     */
    void validate(Map<String, Object> parameters);
}