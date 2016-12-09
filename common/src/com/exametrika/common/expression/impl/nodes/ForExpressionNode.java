/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;





/**
 * The {@link ForExpressionNode} is a for expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ForExpressionNode implements IExpressionNode
{
    private final VariableExpressionNode variableExpression;
    private final IExpressionNode iterableExpression;
    private final IExpressionNode expression;
        
    public ForExpressionNode(VariableExpressionNode variableExpression, IExpressionNode iterableExpression, IExpressionNode expression)
    {
        Assert.notNull(variableExpression);
        Assert.notNull(iterableExpression);
        Assert.notNull(expression);
        
        this.variableExpression = variableExpression;
        this.iterableExpression = iterableExpression;
        this.expression = expression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Iterable iterable = context.getCollectionProvider().getIterable(iterableExpression.evaluate(context, self));
        for (Object value : iterable)
        {
            variableExpression.setValue(context, value);
            Object result = expression.evaluate(context, self);
            if (context.isReturnRequested())
                return result;
            else if (context.isBreakRequested())
            {
                context.setBreakRequested(false);
                break;
            }
            else if (context.isContinueRequested())
            {
                context.setContinueRequested(false);
                continue;
            }
        }
        
        return null;
    }
    
    @Override
    public String toString()
    {
        return "for (" + variableExpression.toString() + " : " + iterableExpression + ")\n{\n" + Strings.indent(expression.toString(), 4) + "\n}";
    }
}
