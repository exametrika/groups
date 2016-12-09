/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link ArrayExpressionNode} is an array expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ArrayExpressionNode implements IExpressionNode
{
    private final List<IExpressionNode> elementExpressions;
    
    public ArrayExpressionNode(List<IExpressionNode> elementExpressions)
    {
        Assert.notNull(elementExpressions);
        
        this.elementExpressions = elementExpressions;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        List<Object> array = new ArrayList<Object>(elementExpressions.size());
        
        for (int i = 0; i < elementExpressions.size(); i++)
            array.add(elementExpressions.get(i).evaluate(context, self));
        
        return array;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        
        boolean first = true;
        for (IExpressionNode expression : elementExpressions)
        {
            if (first)
                first = false;
            else
                builder.append(", ");
            
            builder.append(expression.toString());
        }
        
        builder.append(']');
        return builder.toString();
    }
}
