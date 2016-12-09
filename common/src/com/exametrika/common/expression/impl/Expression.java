/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;


/**
 * The {@link Expression} is an implementation of {@link IExpression}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Expression implements IExpression
{
    private final String text;
    private final IExpressionNode compiledExpression;
    private final int variableCount;
    private final CompileContext compileContext;

    public Expression(String text, IExpressionNode compiledExpression, CompileContext compileContext, int variableCount)
    {
        Assert.notNull(text);
        Assert.notNull(compiledExpression);
        Assert.notNull(compileContext);
        
        this.text = text;
        this.compiledExpression = compiledExpression;
        this.compileContext = compileContext;
        this.variableCount = variableCount;
    }
    
    @Override
    public String getText()
    {
        return text;
    }

    @Override
    public <T> T execute(Object context, Map<String, ? extends Object> variables)
    {
        ExpressionContext expressionContext = new ExpressionContext(context, variables, compileContext.getCollectionProvider(), 
            compileContext.getClassResolver(), compileContext.getConversionProvider(), variableCount);
        return (T)compiledExpression.evaluate(expressionContext, null);
    }
    
    @Override
    public String toString()
    {
        return text;
    }
}
