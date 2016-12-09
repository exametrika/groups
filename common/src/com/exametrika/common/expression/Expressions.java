/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.expression.impl.DebugContext;
import com.exametrika.common.expression.impl.Expression;
import com.exametrika.common.expression.impl.ExpressionParser;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.expression.impl.ParseContext;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link Expressions} contains different utility methods for work with expressions.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Expressions
{
    private static final String DEBUG_PROPERTY = "com.exametrika.expressions.debug";
    private static volatile DebugContext debugContext;
    
    static
    {
        if (Boolean.TRUE.equals(System.getProperty(DEBUG_PROPERTY)))
            debugContext = new DebugContext();
        else
            debugContext = null;
    }
    
    public static IDebugContext getDebugContext()
    {
        return debugContext;
    }
    
    public static void setDebugContext(IDebugContext debugContext)
    {
        Expressions.debugContext = (DebugContext)debugContext;
    }
    
    /**
     * Evaluates expression.
     *
     * @param expression expression
     * @param context context. Can be null
     * @param variables variables. Can be null
     * @return return value. Can be null
     */
    public static <T> T evaluate(String expression, Object context, Map<String, ? extends Object> variables)
    {
        return compile(expression, createCompileContext(null)).execute(context, variables);
    }
    
    /**
     * Compiles expression.
     *
     * @param expression expression
     * @param context compile context
     * @return compiled expression
     */
    public static IExpression compile(String expression, CompileContext context)
    {
        Assert.notNull(expression);
        Assert.notNull(context);
        
        ParseContext parseContext = new ParseContext();
        ExpressionParser parser = new ExpressionParser(expression, parseContext, debugContext);
        IExpressionNode node = parser.parse();
        
        return new Expression(expression, node, context, parseContext.getVariableCount());
    }
    
    /**
     * Creates and initializes compile context from services.
     *
     * @param qualifiers service qualifiers or null if service qualifiers are not used
     * @return created compile context
     */
    public static CompileContext createCompileContext(Set<String> qualifiers)
    {
        CompileContext context = new CompileContext();
        List<ICompileContextRegistrar> providers = Services.loadProviders(ICompileContextRegistrar.class, qualifiers);
        
        for (ICompileContextRegistrar provider : providers)
            provider.register(context);
        
        return context;
    }
    
    /**
     * Creates and initializes runtime context from services.
     *
     * @param qualifiers service qualifiers or null if service qualifiers are not used
     * @param immutable if true immutable context is created
     * @return created runtime context
     */
    public static Map<String, Object> createRuntimeContext(Set<String> qualifiers, boolean immutable)
    {
        Map<String, Object> context = new HashMap<String, Object>();
        List<IRuntimeContextRegistrar> providers = Services.loadProviders(IRuntimeContextRegistrar.class, qualifiers);
        
        for (IRuntimeContextRegistrar provider : providers)
            provider.register(context);
        
        if (immutable)
            return Immutables.wrap(context);
        else
            return context;
    }
    
    private Expressions()
    {
    }
}
