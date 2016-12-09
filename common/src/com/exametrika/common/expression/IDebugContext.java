/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import com.exametrika.common.utils.Assert;



/**
 * The {@link IDebugContext} is a debug context.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IDebugContext
{
    /**
     * The {@link IBreakpointListener} defines breakpoint.
     */
    class Breakpoint
    {
        public final int startLine;
        public final int startCol;
        public final int endLine;
        public final int endCol;
        public final IBreakpointListener listener;
        public final boolean exception;

        public Breakpoint(int startLine, int startCol, int endLine, int endCol, IBreakpointListener listener, boolean exception)
        {
            Assert.notNull(listener);
            
            this.startLine = startLine;
            this.startCol = startCol;
            this.endLine = endLine;
            this.endCol = endCol;
            this.listener = listener;
            this.exception = exception;
        }
    }
    
    /**
     * The {@link IBreakpointListener} listens to specific expression breakpoint.
     */
    interface IBreakpointListener
    {
        /**
         * Called when breakpoint is fired.
         *
         * @param breakpoint breakpoint
         * @param startLine start line
         * @param startCol start column
         * @param endLine end line
         * @param endCol end column
         * @param expression expression
         */
        void onBreakpoint(Breakpoint breakpoint, int startLine, int startCol, int endLine, int endCol);
        void onException(ExpressionException exception);
    }
    
    /**
     * Adds breakspoint.
     *
     * @param breakpoint breakpoint
     */
    void addBreakpoint(Breakpoint breakpoint);
}
