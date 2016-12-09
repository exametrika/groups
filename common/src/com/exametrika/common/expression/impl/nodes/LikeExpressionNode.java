/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;





/**
 * The {@link LikeExpressionNode} is a like pattern (glob or regexp) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LikeExpressionNode implements IExpressionNode
{
    private final IExpressionNode valueExpression;
    private final IExpressionNode patternExpression;
    private volatile Map<String, Pattern> patterns = Collections.emptyMap();

    public LikeExpressionNode(IExpressionNode valueExpression, IExpressionNode patternExpression)
    {
        Assert.notNull(valueExpression);
        Assert.notNull(patternExpression);
        
        this.valueExpression = valueExpression;
        this.patternExpression = patternExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object value = valueExpression.evaluate(context, self);
        if (value == null)
            return false;
        
        String str = value.toString();
        String patternStr = patternExpression.evaluate(context, self).toString();
        Assert.notNull(patternStr);
        
        Map<String, Pattern> patterns = this.patterns;
        Pattern pattern = patterns.get(patternStr);
        if (pattern == null)
            pattern = loadPattern(patternStr);
        
        return pattern.matcher(str).matches();
    }
    
    @Override
    public String toString()
    {
        return valueExpression.toString() + " like " + patternExpression.toString();
    }
    
    private synchronized Pattern loadPattern(String patternStr)
    {
        Pattern pattern = patterns.get(patternStr);
        if (pattern == null)
        {
            pattern = Strings.createFilterPattern(patternStr, false);
            Map<String, Pattern> patterns = new HashMap<String, Pattern>(this.patterns);
            patterns.put(patternStr, pattern);
            this.patterns = patterns;
        }
        
        return pattern;
    }
}
