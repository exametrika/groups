/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.property;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link PropertyNotFoundException} is thrown when property is not found.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class PropertyNotFoundException extends ComponentException
{
    public PropertyNotFoundException()
    {
        super();
    }

    public PropertyNotFoundException(ILocalizedMessage message) 
    {
        super(message);
    }

    public PropertyNotFoundException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public PropertyNotFoundException(Throwable cause) 
    {
        super(cause);
    }
}