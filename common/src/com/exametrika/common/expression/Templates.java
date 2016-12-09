/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.expression.impl.DebugContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.expression.impl.ParseContext;
import com.exametrika.common.expression.impl.Template;
import com.exametrika.common.expression.impl.TemplateParser;
import com.exametrika.common.expression.impl.TemplateRegistry;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;



/**
 * The {@link Templates} contains different utility methods for work with templates.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Templates
{
    /**
     * Evaluates template.
     *
     * @param template template
     * @param context context. Can be null
     * @param variables variables. Can be null
     * @return return value
     */
    public static String evaluate(String template, Object context, Map<String, ? extends Object> variables)
    {
        return compile(template, Expressions.createCompileContext(null), null).execute(context, variables);
    }
    
    /**
     * Compiles template.
     *
     * @param template template
     * @param context compile context
     * @param templateRegistry template registry. Can be null
     * @return compiled template
     */
    public static ITemplate compile(String template, CompileContext context, ITemplateRegistry templateRegistry)
    {
        Assert.notNull(template);
        Assert.notNull(context);
        
        ParseContext parseContext = new ParseContext();
        TemplateParser parser = new TemplateParser(template, parseContext, (DebugContext)Expressions.getDebugContext(), templateRegistry);
        IExpressionNode node = parser.parse();
        
        return new Template(template, node, context, parseContext.getVariableCount());
    }

    /**
     * Creates template registry from services.
     *
     * @param qualifiers service qualifiers or null if service qualifiers are not used
     * @return template registry
     */
    public static ITemplateRegistry createTemplateRegistry(Set<String> qualifiers)
    {
        TemplateRegistry registry = new TemplateRegistry();
        List<ITemplateRegistrar> providers = Services.loadProviders(ITemplateRegistrar.class, qualifiers);
            
        for (ITemplateRegistrar provider : providers)
            provider.register(registry);
            
        return registry;
    }
    
    private Templates()
    {
    }
}
