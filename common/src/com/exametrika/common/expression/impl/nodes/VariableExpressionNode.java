/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;





/**
 * The {@link VariableExpressionNode} is a variable expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class VariableExpressionNode implements IExpressionNode
{
    private final String name;
    private final int slotIndex;
    
    public VariableExpressionNode(String name, int slotIndex)
    {
        this.name = name;
        this.slotIndex = slotIndex;
    }
    
    public void setValue(ExpressionContext context, Object value)
    {
        context.setVariableValue(slotIndex, value);
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        return context.getVariableValue(slotIndex);
    }
    
    @Override
    public String toString()
    {
        return '$' + name;
    }
}
