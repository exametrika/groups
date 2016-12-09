/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.l10n.ILocalizedMessage;



/**
 * The {@link ScopeAlreadyAttachedException} is thrown when trying to attach a scope instance but 
 * another scope is already attached to current thread.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ScopeAlreadyAttachedException extends ComponentException
{
    public ScopeAlreadyAttachedException()
    {
        super();
    }

    public ScopeAlreadyAttachedException(ILocalizedMessage message) 
    {
        super(message);
    }

    public ScopeAlreadyAttachedException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public ScopeAlreadyAttachedException(Throwable cause) 
    {
        super(cause);
    }
}