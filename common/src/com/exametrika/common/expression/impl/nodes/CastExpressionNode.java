/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;





/**
 * The {@link CastExpressionNode} is a typecast expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CastExpressionNode implements IExpressionNode
{
    private final IExpressionNode instanceExpression;
    private final IExpressionNode classExpression;
    
    public CastExpressionNode(IExpressionNode instanceExpression, IExpressionNode classExpression)
    {
        Assert.notNull(instanceExpression);
        Assert.notNull(classExpression);
        
        this.instanceExpression = instanceExpression;
        this.classExpression = classExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object instance = this.instanceExpression.evaluate(context, self);
        if (instance == null)
            return null;
        
        String className = (String)this.classExpression.evaluate(context, self);
        Assert.notNull(className);
        
        Class clazz = context.getClassResolver().resolveClass(className);
        return context.getConversionProvider().cast(instance, clazz);
    }
    
    @Override
    public String toString()
    {
        return "@" + Strings.unquote(classExpression.toString()) + "@(" + instanceExpression.toString() + ")";
    }
}
