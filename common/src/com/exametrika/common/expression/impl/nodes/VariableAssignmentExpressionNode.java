/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link VariableAssignmentExpressionNode} is an variable assignment expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class VariableAssignmentExpressionNode implements IExpressionNode
{
    private final VariableExpressionNode variableExpression;
    private final IExpressionNode expression;
    
    public VariableAssignmentExpressionNode(VariableExpressionNode variableExpression, IExpressionNode expression)
    {
        Assert.notNull(variableExpression);
        Assert.notNull(expression);
        
        this.variableExpression = variableExpression;
        this.expression = expression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        variableExpression.setValue(context, expression.evaluate(context, self));
        return variableExpression.evaluate(context, self);
    }
    
    @Override
    public String toString()
    {
        return variableExpression.toString() + " = " + expression.toString();
    }
}
