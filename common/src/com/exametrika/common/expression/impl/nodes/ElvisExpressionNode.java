/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link ElvisExpressionNode} is an elvis (expr != null ? expr : default) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ElvisExpressionNode implements IExpressionNode
{
    private final IExpressionNode expression;
    private final IExpressionNode defaultExpression;
        
    public ElvisExpressionNode(IExpressionNode expression, IExpressionNode defaultExpression)
    {
        Assert.notNull(expression);
        Assert.notNull(defaultExpression);
        
        this.expression = expression;
        this.defaultExpression = defaultExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object result = expression.evaluate(context, self);
        if (result != null)
            return result;
        else
            return defaultExpression.evaluate(context, self);
    }
    
    @Override
    public String toString()
    {
        return expression.toString() + "?:" + defaultExpression.toString();
    }
}
