/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link ExpressionException} is a base expression exception.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ExpressionException extends SystemException
{
    public ExpressionException()
    {
        super();
    }

    public ExpressionException(ILocalizedMessage message) 
    {
        super(message);
    }

    public ExpressionException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public ExpressionException(Throwable cause) 
    {
        super(cause);
    }
}