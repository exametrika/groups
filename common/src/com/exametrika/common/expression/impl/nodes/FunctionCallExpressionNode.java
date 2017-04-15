/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.util.Map;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.expression.impl.ParseContext;
import com.exametrika.common.utils.Assert;





/**
 * The {@link FunctionCallExpressionNode} is a function call expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FunctionCallExpressionNode implements IExpressionNode
{
    private final VariableExpressionNode functionExpression;
    private final IExpressionNode argumentsExpression;

    public FunctionCallExpressionNode(VariableExpressionNode functionExpression, IExpressionNode argumentsExpression)
    {
        Assert.notNull(functionExpression);
        Assert.notNull(argumentsExpression);
        
        this.functionExpression = functionExpression;
        this.argumentsExpression = argumentsExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        FunctionExpressionNode function = (FunctionExpressionNode)functionExpression.evaluate(context, self);
        ParseContext parseContext = function.getParseContext();
        
        Object arguments = argumentsExpression.evaluate(context, self);
        
        ExpressionContext functionContext = new ExpressionContext(context.getRoot(), context.getContext(), 
            context.getCollectionProvider(), context.getClassResolver(), context.getConversionProvider(), 
                parseContext.getVariableCount());
        functionContext.setVariableValue(0, arguments);
        
        if (arguments instanceof Map)
        {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>)arguments).entrySet())
            {
                Integer slotIndex = parseContext.findVariable(entry.getKey().toString());
                if (slotIndex != null)
                    functionContext.setVariableValue(slotIndex, entry.getValue());
            }
        }
        
        return function.getBodyExpression().evaluate(functionContext, null);
    }
    
    @Override
    public String toString()
    {
        return functionExpression.toString() + "(" + argumentsExpression.toString() + ")";
    }
}
