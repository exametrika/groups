/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.UUID;


/**
 * The {@link IDataLossState} represents a data loss state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IDataLossState
{
    /**
     * Returns group domain.
     *
     * @return group domain
     */
    String getDomain();
    
    /**
     * Returns group with data loss identifier.
     *
     * @return group with data loss identifier
     */
    UUID getId();
}