/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link GroupExpressionNode} is a group expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupExpressionNode implements IExpressionNode
{
    private final IExpressionNode expression;
    
    public GroupExpressionNode(IExpressionNode expression)
    {
        Assert.notNull(expression);
        
        this.expression = expression;
    }

    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        return expression.evaluate(context, self);
    }
    
    @Override
    public String toString()
    {
        return "(" + expression.toString() + ")";
    }
}
