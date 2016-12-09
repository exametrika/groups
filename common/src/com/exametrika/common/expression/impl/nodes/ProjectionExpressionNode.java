/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link ProjectionExpressionNode} is a collection projection expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ProjectionExpressionNode implements IExpressionNode
{
    private final IExpressionNode expression;
    
    public ProjectionExpressionNode(IExpressionNode expression)
    {
        Assert.notNull(expression);
        
        this.expression = expression;
    }

    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        if (self == null)
            return Collections.emptyList();
        
        Iterable iterable = context.getCollectionProvider().getIterable(self);
        
        List<Object> list = new ArrayList<Object>();
        for (Object element : iterable)
        {
            Object projection = expression.evaluate(context, element);
            list.add(projection);
        }

        return list;
    }
    
    @Override
    public String toString()
    {
        return "![" + expression.toString() + "]";
    }
}
