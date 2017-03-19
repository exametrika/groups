/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link IModelVisitor} represents a network model visitor.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IModelVisitor
{
    /**
     * Visits specified network model component.
     *
     * @param component component to visit
     */
    void visitComponent(ILifecycle component);
}
