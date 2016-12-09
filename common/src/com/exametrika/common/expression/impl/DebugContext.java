/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.expression.ExpressionException;
import com.exametrika.common.expression.IDebugContext;
import com.exametrika.common.utils.Assert;






/**
 * The {@link DebugContext} is a debug context.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DebugContext implements IDebugContext
{
    private volatile List<Breakpoint> breakpoints = Collections.emptyList();

    @Override
    public synchronized void addBreakpoint(Breakpoint breakpoint)
    {
        Assert.notNull(breakpoint);
        
        List<Breakpoint> breakpoints = new ArrayList<Breakpoint>(this.breakpoints);
        breakpoints.add(breakpoint);
        this.breakpoints = breakpoints;
    }
    
    public void onBreakpoint(int startLine, int startCol, int endLine, int endCol, IExpressionNode expression)
    {
        List<Breakpoint> breakpoints = this.breakpoints;
        for (Breakpoint breakpoint : breakpoints)
        {
            if (matches(breakpoint, startLine, startCol, endLine, endCol))
                breakpoint.listener.onBreakpoint(breakpoint, startLine, startCol, endLine, endCol);
        }
    }
    
    public void onException(ExpressionException exception, IExpressionNode expression)
    {
        List<Breakpoint> breakpoints = this.breakpoints;
        for (Breakpoint breakpoint : breakpoints)
        {
            if (breakpoint.exception)
                breakpoint.listener.onException(exception);
        }
    }
    
    private boolean matches(Breakpoint breakpoint, int startLine, int startCol, int endLine, int endCol)
    {
        if (endLine < breakpoint.startLine || (endLine == breakpoint.startLine && endCol < breakpoint.startCol))
            return false;
        if (startLine > breakpoint.endLine || (startLine == breakpoint.endLine && startCol > breakpoint.endCol))
            return false;
        return true;
    }
}
