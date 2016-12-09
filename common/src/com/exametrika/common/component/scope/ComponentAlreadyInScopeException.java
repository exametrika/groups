/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.l10n.ILocalizedMessage;



/**
 * The {@link ComponentAlreadyInScopeException} is thrown when trying to add component in scope but 
 * another component already exists in scope.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ComponentAlreadyInScopeException extends ComponentException
{
    public ComponentAlreadyInScopeException()
    {
        super();
    }

    public ComponentAlreadyInScopeException(ILocalizedMessage message) 
    {
        super(message);
    }

    public ComponentAlreadyInScopeException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public ComponentAlreadyInScopeException(Throwable cause) 
    {
        super(cause);
    }
}