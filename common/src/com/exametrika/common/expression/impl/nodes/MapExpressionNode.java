/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;





/**
 * The {@link MapExpressionNode} is a map expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MapExpressionNode implements IExpressionNode
{
    private final List<Pair<IExpressionNode, IExpressionNode>> elementExpressions;
    
    public MapExpressionNode(List<Pair<IExpressionNode, IExpressionNode>> elementExpressions)
    {
        Assert.notNull(elementExpressions);
        
        this.elementExpressions = elementExpressions;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Map<Object, Object> map = new LinkedHashMap<Object, Object>(elementExpressions.size());
        for (int i = 0; i < elementExpressions.size(); i++)
        {
            Object key = elementExpressions.get(i).getKey().evaluate(context, self);
            Object value = elementExpressions.get(i).getValue().evaluate(context, self);
            map.put(key, value);
        }
        
        return map;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        
        boolean first = true;
        for (Pair<IExpressionNode, IExpressionNode> pair : elementExpressions)
        {
            if (first)
                first = false;
            else
                builder.append(", ");
            
            builder.append(pair.getKey().toString());
            builder.append(" : ");
            builder.append(pair.getValue().toString());
        }
        
        builder.append('}');
        return builder.toString();
    }
}
