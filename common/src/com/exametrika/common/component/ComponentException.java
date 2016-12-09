/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link ComponentException} is a base exception for component framework.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ComponentException extends SystemException
{
    public ComponentException()
    {
        super();
    }

    public ComponentException(ILocalizedMessage message) 
    {
        super(message);
    }

    public ComponentException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public ComponentException(Throwable cause) 
    {
        super(cause);
    }
}