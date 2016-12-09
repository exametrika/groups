/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.text.MessageFormat;

import com.exametrika.common.expression.impl.DebugContext;
import com.exametrika.common.expression.impl.DebugExpressionException;
import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.ExpressionTokenizer;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;





/**
 * The {@link DebugExpressionNode} is a debug expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DebugExpressionNode implements IExpressionNode
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final DebugContext debugContext;
    private final int startPos;
    private final int endPos;
    private final IExpressionNode expression;
    private final String expressionText;
    
    public DebugExpressionNode(DebugContext debugContext, int startPos, int endPos, IExpressionNode expression, String expressionText)
    {
        Assert.notNull(debugContext);
        Assert.notNull(expression);
        Assert.notNull(expressionText);
        
        this.debugContext = debugContext;
        this.startPos = startPos;
        this.endPos = endPos;
        this.expression = expression;
        this.expressionText = expressionText;
    }
    
    public IExpressionNode getExpression()
    {
        return expression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        int[] start = ExpressionTokenizer.calculateLineCol(expressionText, startPos);
        int[] end = ExpressionTokenizer.calculateLineCol(expressionText, endPos);
        context.pushStack(this, self);
        debugContext.onBreakpoint(start[0], start[1], end[0], end[1], this);
        
        try
        {
            return expression.evaluate(context, self);
        }
        catch (DebugExpressionException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            DebugExpressionException exception = new DebugExpressionException(messages.runtimeError(e.getMessage(), buildStack(context)), e);
            debugContext.onException(exception, this);
            
            throw exception;
        }
        finally
        {
            context.popStack();
        }
    }
    
    @Override
    public String toString()
    {
        return expression.toString();
    }
    
    private String buildStack(ExpressionContext context)
    {
        StringBuilder builder = new StringBuilder();
        for (Pair<DebugExpressionNode, Object> pair : context.getStack())
        {
            int[] location = ExpressionTokenizer.calculateLineCol(expressionText, startPos);
            builder.append(MessageFormat.format("  @[{0},{1}], self-{2}: {3}\n", location[0], location[1], 
                pair.getValue(), pair.getKey().expressionText.substring(pair.getKey().startPos, pair.getKey().endPos + 1)));
        }
        
        return builder.toString();
    }

    private interface IMessages
    {
        @DefaultMessage("Runtime error {0}:\n{1}")
        ILocalizedMessage runtimeError(String message, String stack);
    }
}
