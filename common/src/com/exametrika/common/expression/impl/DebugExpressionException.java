/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import com.exametrika.common.expression.ExpressionException;
import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link DebugExpressionException} is a debug expression exception.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class DebugExpressionException extends ExpressionException
{
    public DebugExpressionException()
    {
        super();
    }

    public DebugExpressionException(ILocalizedMessage message) 
    {
        super(message);
    }

    public DebugExpressionException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public DebugExpressionException(Throwable cause) 
    {
        super(cause);
    }
}