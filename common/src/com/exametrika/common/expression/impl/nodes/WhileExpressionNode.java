/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;





/**
 * The {@link WhileExpressionNode} is a while expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WhileExpressionNode implements IExpressionNode
{
    private final IExpressionNode conditionExpression;
    private final IExpressionNode expression;
        
    public WhileExpressionNode(IExpressionNode conditionExpression, IExpressionNode expression)
    {
        Assert.notNull(conditionExpression);
        Assert.notNull(expression);
        
        this.conditionExpression = conditionExpression;
        this.expression = expression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        while (context.getConversionProvider().asBoolean(conditionExpression.evaluate(context, self)))
        {
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
        return "while (" + conditionExpression.toString() + ")\n{\n" + Strings.indent(expression.toString(), 4) + "\n}";
    }
}
