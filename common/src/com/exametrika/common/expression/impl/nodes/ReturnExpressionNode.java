/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;





/**
 * The {@link ReturnExpressionNode} is a return expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ReturnExpressionNode implements IExpressionNode
{
    private final IExpressionNode expression;
    
    public ReturnExpressionNode(IExpressionNode expression)
    {
        this.expression = expression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        context.setReturnRequested();
        if (expression != null)
            return expression.evaluate(context, self);
        else
            return null;
    }
    
    @Override
    public String toString()
    {
        return "return";
    }
}
