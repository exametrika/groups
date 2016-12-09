/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.resource.IResourceAllocator;



/**
 * The {@link IRawDatabaseFactory} a factory of database.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawDatabaseFactory
{
    /**
     * Creates database.
     *
     * @param configuration database configuration.
     * @return database
     */
    IRawDatabase createDatabase(RawDatabaseConfiguration configuration);
    
    /**
     * Creates database.
     *
     * @param configuration database configuration.
     * @param compartment compartment to be used. Can be null if default compartment is used
     * @param compartmentOwner true if database owns compartment
     * @param batchContext batch context or null if batch context is not used
     * @param resourceAllocator external resource allocator which will be used as parent of database resource allocator or
     * null if external resource allocator is not set
     * @return database
     */
    IRawDatabase createDatabase(RawDatabaseConfiguration configuration, ICompartment compartment, boolean compartmentOwner,
        IRawBatchContext batchContext, IResourceAllocator resourceAllocator);
}
