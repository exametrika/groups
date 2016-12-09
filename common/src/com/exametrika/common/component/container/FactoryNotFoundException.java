/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.l10n.ILocalizedMessage;



/**
 * The {@link FactoryNotFoundException} is thrown when component factory is not found in component container.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class FactoryNotFoundException extends ComponentException
{
    public FactoryNotFoundException()
    {
        super();
    }

    public FactoryNotFoundException(ILocalizedMessage message) 
    {
        super(message);
    }

    public FactoryNotFoundException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public FactoryNotFoundException(Throwable cause) 
    {
        super(cause);
    }
}