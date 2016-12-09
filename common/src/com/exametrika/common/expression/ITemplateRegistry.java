/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;



/**
 * The {@link ITemplateRegistry} represents a registry of named templates.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ITemplateRegistry
{
    /**
     * Finds template by name.
     *
     * @param name name
     * @return template or null if template is not found
     */
    String findTemplate(String name);
    
    /**
     * Adds template with specified name to registry.
     *
     * @param name template name
     * @param template template
     * @return true if template successfully added to registry, false if template with specified name already exists in registry
     */
    boolean addTemplate(String name, String template);
}
