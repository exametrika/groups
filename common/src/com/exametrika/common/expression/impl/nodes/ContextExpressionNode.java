/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;





/**
 * The {@link ContextExpressionNode} is a "runtime context" (context) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ContextExpressionNode implements IExpressionNode
{
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        return context.getContext();
    }
    
    @Override
    public String toString()
    {
        return "$context";
    }
}
