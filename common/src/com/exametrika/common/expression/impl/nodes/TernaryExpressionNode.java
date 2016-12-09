/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link TernaryExpressionNode} is a ternary expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TernaryExpressionNode implements IExpressionNode
{
    private final IExpressionNode conditionExpression;
    private final IExpressionNode firstExpression;
    private final IExpressionNode secondExpression;
        
    public TernaryExpressionNode(IExpressionNode conditionExpression, IExpressionNode firstExpression, 
        IExpressionNode secondExpression)
    {
        Assert.notNull(conditionExpression);
        Assert.notNull(firstExpression);
        Assert.notNull(secondExpression);
        
        this.conditionExpression = conditionExpression;
        this.firstExpression = firstExpression;
        this.secondExpression = secondExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        boolean condition = context.getConversionProvider().asBoolean(conditionExpression.evaluate(context, self));
        if (condition)
            return firstExpression.evaluate(context, self);
        else
            return secondExpression.evaluate(context, self);
    }
    
    @Override
    public String toString()
    {
        return conditionExpression.toString() + " ? " + firstExpression.toString() + " : " + secondExpression.toString();
    }
}
