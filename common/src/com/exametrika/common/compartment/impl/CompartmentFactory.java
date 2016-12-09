/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartmentFactory;



/**
 * The {@link CompartmentFactory} is a compartment factory implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompartmentFactory implements ICompartmentFactory
{
    @Override
    public Compartment createCompartment(ICompartmentFactory.Parameters parameters)
    {
        return new Compartment(parameters);
    }
}
