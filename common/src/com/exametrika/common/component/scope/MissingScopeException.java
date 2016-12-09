/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.l10n.ILocalizedMessage;



/**
 * The {@link MissingScopeException} is thrown when requested scope is not attached to current thread.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class MissingScopeException extends ComponentException
{
    public MissingScopeException()
    {
        super();
    }

    public MissingScopeException(ILocalizedMessage message) 
    {
        super(message);
    }

    public MissingScopeException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public MissingScopeException(Throwable cause) 
    {
        super(cause);
    }
}