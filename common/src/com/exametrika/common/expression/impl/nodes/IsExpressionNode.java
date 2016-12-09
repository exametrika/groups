/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;





/**
 * The {@link IsExpressionNode} is a "is" (instanceof) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class IsExpressionNode implements IExpressionNode
{
    private final IExpressionNode instanceExpression;
    private final IExpressionNode classExpression;
    
    public IsExpressionNode(IExpressionNode instanceExpression, IExpressionNode classExpression)
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
            return false;
        
        String className = (String)this.classExpression.evaluate(context, self);
        Assert.notNull(className);
        
        Class clazz = context.getClassResolver().resolveClass(className);
        return clazz.isInstance(instance);
    }
    
    @Override
    public String toString()
    {
        return instanceExpression.toString() + " is @" + Strings.unquote(classExpression.toString()) + "@";
    }
}
