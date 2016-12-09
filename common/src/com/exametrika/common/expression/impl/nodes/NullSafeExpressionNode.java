/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link NullSafeExpressionNode} is a null-safe (?.) track access expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NullSafeExpressionNode implements IExpressionNode
{
    private final IExpressionNode nextExpression;
        
    public NullSafeExpressionNode(IExpressionNode nextExpression)
    {
        Assert.notNull(nextExpression);
        
        this.nextExpression = nextExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        if (self != null)
            return nextExpression.evaluate(context, self);
        else
            return null;
    }
    
    @Override
    public String toString()
    {
        return "?." + nextExpression.toString();
    }
}
