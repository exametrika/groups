/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.expression.impl.ParseContext;
import com.exametrika.common.utils.Assert;





/**
 * The {@link FunctionExpressionNode} is a function expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FunctionExpressionNode implements IExpressionNode
{
    private final ParseContext parseContext;
    private final IExpressionNode bodyExpression;

    public FunctionExpressionNode(ParseContext parseContext, IExpressionNode bodyExpression)
    {
        Assert.notNull(parseContext);
        Assert.notNull(bodyExpression);
        
        this.parseContext = parseContext;
        this.bodyExpression = bodyExpression;
    }
    
    public ParseContext getParseContext()
    {
        return parseContext;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        return bodyExpression.evaluate(context, self);
    }
    
    @Override
    public String toString()
    {
        return "function";
    }
}
