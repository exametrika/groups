/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;

/**
 * The {@link ExpressionCondition} is an expression condition.
 * 
 * @param <T> condition type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ExpressionCondition<T> implements ICondition<T>
{
    private final IExpression expression;

    public ExpressionCondition(IExpression expression)
    {
        Assert.notNull(expression);
        
        this.expression = expression;
    }
    
    @Override
    public boolean evaluate(T value)
    {
        return Boolean.TRUE.equals(expression.execute(value, null));
    }
}
