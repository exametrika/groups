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
 * The {@link SelectionExpressionNode} is a collection selection expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SelectionExpressionNode implements IExpressionNode
{
    private final IExpressionNode expression;
    private final Operation operation;

    public enum Operation
    {
        FIRST, 
        LAST, 
        ALL 
    }
    
    public SelectionExpressionNode(IExpressionNode expression, Operation operation)
    {
        Assert.notNull(expression);
        Assert.notNull(operation);
        
        this.expression = expression;
        this.operation = operation;
    }

    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        switch (operation)
        {
        case FIRST:
            {
                if (self == null)
                    return null;
                
                Iterable iterable = context.getCollectionProvider().getIterable(self);
                for (Object element : iterable)
                {
                    boolean selection = context.getConversionProvider().asBoolean(expression.evaluate(context, element));
                    if (selection)
                        return element;
                }
                
                return null;
            }
        case LAST:
        {
            if (self == null)
                return null;
            
            Iterable iterable = context.getCollectionProvider().getIterable(self);
            Object lastElement = null;
            for (Object element : iterable)
            {
                boolean selection = context.getConversionProvider().asBoolean(expression.evaluate(context, element));
                if (selection)
                    lastElement = element;
            }
            
            return lastElement;
        }
        case ALL:
            {
                if (self == null)
                    return Collections.emptyList();
                
                Iterable iterable = context.getCollectionProvider().getIterable(self);
                List<Object> list = new ArrayList<Object>();
                for (Object element : iterable)
                {
                    boolean selection = context.getConversionProvider().asBoolean(expression.evaluate(context, element));
                    if (selection)
                        list.add(element);
                }

                return list;
            }
        default:
            return Assert.error();
        }
    }
    
    @Override
    public String toString()
    {
        return toString(operation) + "[" + expression.toString() + "]";
    }

    private String toString(Operation operation)
    {
        switch (operation)
        {
        case ALL:
            return "?";
        case FIRST:
            return "&";
        case LAST:
            return "^";
        default:
            return Assert.error();
        }
    }
}
