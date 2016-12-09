/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;





/**
 * The {@link IfExpressionNode} is a if expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class IfExpressionNode implements IExpressionNode
{
    private final IExpressionNode conditionExpression;
    private final IExpressionNode firstExpression;
    private final IExpressionNode secondExpression;
        
    public IfExpressionNode(IExpressionNode conditionExpression, IExpressionNode firstExpression, 
        IExpressionNode secondExpression)
    {
        Assert.notNull(conditionExpression);
        Assert.notNull(firstExpression);
        
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
        else if (secondExpression != null)
            return secondExpression.evaluate(context, self);
        else
            return null;
    }
    
    @Override
    public String toString()
    {
        return "if (" + conditionExpression.toString() + ")\n{\n" + Strings.indent(firstExpression.toString(), 4) + "\n}" +
            (secondExpression != null ? ("else\n{\n" + Strings.indent(secondExpression.toString(), 4) + "\n}"): "");
    }
}
