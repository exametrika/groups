/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.utils.Assert;



/**
 * The {@link TemplateRegistry} is an implementation of {@link ITemplateRegistry}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TemplateRegistry implements ITemplateRegistry
{
    private final Map<String, String> templates = new HashMap<String, String>();

    @Override
    public synchronized String findTemplate(String name)
    {
        return templates.get(name);
    }
    
    @Override
    public synchronized boolean addTemplate(String name, String template)
    {
        Assert.notNull(name);
        Assert.notNull(template);
        
        if (templates.containsKey(name))
            return false;
        
        templates.put(name, template);
        return true;
    }
}
