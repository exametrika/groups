/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.IRawDatabaseFactory;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.utils.Assert;



/**
 * The {@link RawDatabaseFactory} is an implementation of {@link IRawDatabaseFactory}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawDatabaseFactory implements IRawDatabaseFactory
{
    @Override
    public RawDatabase createDatabase(RawDatabaseConfiguration configuration)
    {
        return createDatabase(configuration, null, false, null, null);
    }
    
    @Override
    public RawDatabase createDatabase(RawDatabaseConfiguration configuration, ICompartment compartment, boolean compartmentOwner,
        IRawBatchContext batchContext, IResourceAllocator resourceAllocator)
    {
        Assert.notNull(configuration);
        
        if (compartment == null)
        {
            ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
            compartmentParameters.name = configuration.getName();
            compartmentParameters.dispatchPeriod = configuration.getTimerPeriod();
            compartmentParameters.queue = new RawTransactionQueue();
            
            compartment = new CompartmentFactory().createCompartment(compartmentParameters);
            compartmentOwner = true;
        }
        
        return new RawDatabase(configuration, compartment, compartmentOwner, batchContext, resourceAllocator);
    }
}
