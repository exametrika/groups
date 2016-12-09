/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link ResourceNotFoundException} is thrown when resource is not found.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ResourceNotFoundException extends ComponentException
{
    public ResourceNotFoundException()
    {
        super();
    }

    public ResourceNotFoundException(ILocalizedMessage message) 
    {
        super(message);
    }

    public ResourceNotFoundException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public ResourceNotFoundException(Throwable cause) 
    {
        super(cause);
    }
}