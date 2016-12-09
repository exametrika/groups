/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import com.exametrika.common.expression.ExpressionException;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TemplateParser} is a template parser. Template is ordinary string with embedded expressions, delimited by
 * template delimites such as <% and %>. Expression can be string expression, which will be added to result of template expression or
 * arbitrary expression (if/for/while) which must be started with # and will not be added to result of template expression 
 * example (<%# if (true) %>). Expression can contain references on template registry like {@literal @template(<templateName>)}.
 *
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev_A
 */
public class TemplateParser
{
    private static final String TEMPLATE = "@template(";
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String value;
    private final String startDelimiter;
    private final String endDelimiter;
    private final ParseContext context;
    private final DebugContext debugContext;
    private final ITemplateRegistry templateRegistry;

    public TemplateParser(String value)
    {
        this(value, new ParseContext(), null, null);
    }
    
    public TemplateParser(String value, ParseContext context, DebugContext debugContext, ITemplateRegistry templateRegistry)
    {
        this(value, "<%", "%>", context, debugContext, templateRegistry);
    }
    
    public TemplateParser(String value, String startDelimiter, String endDelimiter, ParseContext context, DebugContext debugContext,
        ITemplateRegistry templateRegistry)
    {
        Assert.notNull(value);
        Assert.notNull(startDelimiter);
        Assert.notNull(endDelimiter);
        Assert.notNull(context);
        
        this.value = value;
        this.startDelimiter = startDelimiter;
        this.endDelimiter = endDelimiter;
        this.context = context;
        this.debugContext = debugContext;
        this.templateRegistry = templateRegistry;
    }

    public IExpressionNode parse()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("$_exa = '';");

        parse(builder, value);
        
        builder.append("$_exa");
        
        ExpressionParser expressionParser = new ExpressionParser(builder.toString(), context, debugContext);
        return expressionParser.parse();
    }
    
    private void parse(StringBuilder builder, String value)
    {
        int pos = 0;
        while (true)
        {
            int start = value.indexOf(startDelimiter, pos);
            if (start == -1)
            {
                if (pos < value.length())
                    builder.append("$_exa = $_exa + \\" + value.substring(pos) + "\\;");
                break;
            }
            else if (start > pos)
                builder.append("$_exa = $_exa + \\" + value.substring(pos, start) + "\\;");
            
            int end = value.indexOf(endDelimiter, start + startDelimiter.length());
            if (end == -1)
                throw new ExpressionException(messages.unclosedExpression());
            
            String expression = value.substring(start + startDelimiter.length(), end);
            
            if (expression.startsWith(TEMPLATE))
            {
                int endPos = expression.lastIndexOf(')');
                if (endPos == -1)
                    throw new ExpressionException(messages.unclosedExpression());
                
                String templateName = expression.substring(TEMPLATE.length(), endPos);
                String template = null;
                if (templateRegistry != null)
                    template = templateRegistry.findTemplate(templateName);
                if (template == null)
                    throw new ExpressionException(messages.templateNotFound(templateName));
                
                parse(builder, template);
            }
            else if (expression.startsWith("#"))
                builder.append(expression.substring(1) + ";");
            else
                builder.append("$_exa = $_exa + (" + expression + ");");
            
            pos = end + endDelimiter.length();
        }
    }

    private interface IMessages
    {
        @DefaultMessage("Expression is not closed.")
        ILocalizedMessage unclosedExpression();
        
        @DefaultMessage("Template ''{0}'' is not found.")
        ILocalizedMessage templateNotFound(String templateName);
    }  
}
