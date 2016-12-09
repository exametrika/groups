/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link TrackExpressionNode} is a . track access expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TrackExpressionNode implements IExpressionNode
{
    private final boolean simple;
    private final IExpressionNode prevExpression;
    private final IExpressionNode nextExpression;
        
    public TrackExpressionNode(boolean simple, IExpressionNode prevExpression, IExpressionNode nextExpression)
    {
        Assert.notNull(prevExpression);
        Assert.notNull(nextExpression);
        
        this.simple = simple;
        this.prevExpression = prevExpression;
        this.nextExpression = nextExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object result = prevExpression.evaluate(context, self);
        return nextExpression.evaluate(context, result);
    }
    
    @Override
    public String toString()
    {
        return prevExpression + (simple ? "." : "") + nextExpression;
    }
}
