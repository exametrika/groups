/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartmentGroupFactory;



/**
 * The {@link CompartmentGroupFactory} is a compartment group factory implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompartmentGroupFactory implements ICompartmentGroupFactory
{
    @Override
    public CompartmentGroup createCompartmentGroup(ICompartmentGroupFactory.Parameters parameters)
    {
        return new CompartmentGroup(parameters);
    }
}
