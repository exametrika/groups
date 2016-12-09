/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import java.util.Map;


/**
 * The {@link ITemplate} represents a compiled template.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ITemplate
{
    /**
     * Returns text of template.
     *
     * @return text of template
     */
    String getText();
    
    /**
     * Executes template.
     *
     * @param context context. Can be null
     * @param variables variables. Can be null
     * @return return value
     */
    String execute(Object context, Map<String, ? extends Object> variables);
}
