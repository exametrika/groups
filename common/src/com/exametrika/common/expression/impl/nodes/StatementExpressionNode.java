/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.util.List;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link StatementExpressionNode} is a variable expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StatementExpressionNode implements IExpressionNode
{
    private final List<IExpressionNode> expressions;
    
    public StatementExpressionNode(List<IExpressionNode> expressions)
    {
        Assert.notNull(expressions);
        
        this.expressions = expressions;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object lastResult = null;
        for (int i = 0; i < expressions.size(); i++)
        {
            if (context.isStopRequested())
                break;
            lastResult = expressions.get(i).evaluate(context, self);
        }
        
        return lastResult;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        
        boolean first = true;
        for (IExpressionNode expression : expressions)
        {
            if (first)
                first = false;
            else
                builder.append(";\n");
            
            builder.append(expression.toString());
        }
        
        return builder.toString();
    }
}
