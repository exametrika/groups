/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplate;
import com.exametrika.common.utils.Assert;


/**
 * The {@link Template} is an implementation of {@link ITemplate}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Template implements ITemplate
{
    private final String text;
    private final IExpressionNode compiledExpression;
    private final int variableCount;
    private final CompileContext compileContext;

    public Template(String text, IExpressionNode compiledExpression, CompileContext compileContext, int variableCount)
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
    public String execute(Object context, Map<String, ? extends Object> variables)
    {
        ExpressionContext expressionContext = new ExpressionContext(context, variables, compileContext.getCollectionProvider(), 
            compileContext.getClassResolver(), compileContext.getConversionProvider(), variableCount);
        return (String)compiledExpression.evaluate(expressionContext, null);
    }

    @Override
    public String toString()
    {
        return text;
    }
}
