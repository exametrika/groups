/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link UnaryExpressionNode} is an unary expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class UnaryExpressionNode implements IExpressionNode
{
    private final IExpressionNode expression;
    private final Operation operation;

    public enum Operation
    {
        PLUS, 
        MINUS, 
        NOT, 
        BNOT
    }
    
    public UnaryExpressionNode(IExpressionNode expression, Operation operation)
    {
        Assert.notNull(expression);
        Assert.notNull(operation);
        
        this.expression = expression;
        this.operation = operation;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object value = this.expression.evaluate(context, self);
        Assert.notNull(value);
        
        switch (operation)
        {
        case PLUS:
            if (value instanceof Double)
                return +((Double)value);
            else
                return +((Number)value).longValue();
        case MINUS:
            if (value instanceof Double)
                return -((Double)value);
            else
                return -((Number)value).longValue();
        case NOT:
            return !context.getConversionProvider().asBoolean(value);
        case BNOT:
            return ~((Number)value).longValue();
        default:
            return Assert.error();
        }
    }
    
    @Override
    public String toString()
    {
        return toString(operation) + expression.toString();
    }

    private String toString(Operation operation)
    {
        switch (operation)
        {
        case PLUS:
            return "+";
        case MINUS:
            return "-";
        case NOT:
            return "!";
        case BNOT:
            return "~";
        default:
            return Assert.error();
        }
    }
}
