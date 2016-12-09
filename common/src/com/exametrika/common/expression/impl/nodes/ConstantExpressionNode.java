/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;





/**
 * The {@link ConstantExpressionNode} is a constant expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ConstantExpressionNode implements IExpressionNode
{
    private final Object value;
    
    public ConstantExpressionNode(Object value)
    {
        this.value = value;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        return value;
    }
    
    @Override
    public String toString()
    {
        if (value == null)
            return "null";
        else if (Boolean.TRUE.equals(value))
            return "true";
        else if (Boolean.FALSE.equals(value))
            return "false";
        else if (value instanceof String)
            return "\"" + value + "\"";
        else
            return value.toString();
    }
}
