/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link InvalidConfigurationException} is thrown when configuration is not valid.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class InvalidConfigurationException extends SystemException
{
    public InvalidConfigurationException()
    {
        super();
    }

    public InvalidConfigurationException(ILocalizedMessage message) 
    {
        super(message);
    }

    public InvalidConfigurationException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public InvalidConfigurationException(Throwable cause) 
    {
        super(cause);
    }
}