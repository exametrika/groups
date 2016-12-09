/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.IClassResolver;
import com.exametrika.common.expression.ICollectionProvider;
import com.exametrika.common.expression.IConversionProvider;
import com.exametrika.common.expression.impl.nodes.DebugExpressionNode;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Pair;






/**
 * The {@link ExpressionContext} is a expression context.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ExpressionContext
{
    private final Object root;
    private final Map<String, Object> context;
    private final ICollectionProvider collectionProvider;
    private final IClassResolver classResolver;
    private final IConversionProvider conversionProvider;
    private final ArrayList<Object> variables;
    private boolean returnRequested;
    private boolean breakRequested;
    private boolean continueRequested;
    private List<Pair<DebugExpressionNode, Object>> stack;

    public ExpressionContext()
    {
        this(null);
    }
    
    public ExpressionContext(Object value)
    {
        this(value, null, new StandardCollectionProvider(), new StandardClassResolver(), new StandardConversionProvider(), 1);
    }
    
    public ExpressionContext(Object root, Map<String, ? extends Object> context, ICollectionProvider collectionProvider, IClassResolver classResolver, 
        IConversionProvider conversionProvider, int variableCount)
    {
        this.root = root;
        this.context = (Map)context;
        this.collectionProvider = collectionProvider;
        this.classResolver = classResolver;
        this.conversionProvider = conversionProvider;
        
        if (variableCount > 0)
            this.variables = new ArrayList<Object>(variableCount);
        else
            this.variables = null;
    }
    
    public Object getRoot()
    {
        return root;
    }
    
    public Map<String, Object> getContext()
    {
        return context;
    }
    
    public ICollectionProvider getCollectionProvider()
    {
        return collectionProvider;
    }

    public IClassResolver getClassResolver()
    {
        return classResolver;
    }

    public IConversionProvider getConversionProvider()
    {
        return conversionProvider;
    }

    public Object getVariableValue(int slotIndex)
    {
        return Collections.get(variables, slotIndex);
    }

    public void setVariableValue(int slotIndex, Object value)
    {
        Collections.set(variables, slotIndex, value);
    }
    
    public boolean isStopRequested()
    {
        return returnRequested || breakRequested || continueRequested;
    }
    
    public boolean isReturnRequested()
    {
        return returnRequested;
    }
    
    public void setReturnRequested()
    {
        this.returnRequested = true;
    }
    
    public boolean isBreakRequested()
    {
        return breakRequested;
    }
    
    public void setBreakRequested(boolean value)
    {
        this.breakRequested = value;
    }
    
    public boolean isContinueRequested()
    {
        return continueRequested;
    }
    
    public void setContinueRequested(boolean value)
    {
        this.continueRequested = value;
    }
    
    public List<Pair<DebugExpressionNode, Object>> getStack()
    {
        return stack;
    }
    
    public void pushStack(DebugExpressionNode expression, Object self)
    {
        if (stack == null)
            stack = new ArrayList<Pair<DebugExpressionNode, Object>>(100);
        
        stack.add(new Pair(expression, self));
    }
    
    public void popStack()
    {
        stack.remove(stack.size() - 1);
    }
}
