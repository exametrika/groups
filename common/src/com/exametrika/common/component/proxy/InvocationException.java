/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.l10n.ILocalizedMessage;



/**
 * The {@link InvocationException} is thrown when exception occured in proxy call.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class InvocationException extends ComponentException
{
    public InvocationException()
    {
        super();
    }

    public InvocationException(ILocalizedMessage message) 
    {
        super(message);
    }

    public InvocationException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public InvocationException(Throwable cause) 
    {
        super(cause);
    }
}