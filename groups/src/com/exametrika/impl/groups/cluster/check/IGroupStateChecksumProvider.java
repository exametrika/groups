/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.check;

/**
 * The {@link IGroupStateChecksumProvider} is used to provide checksum of group state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupStateChecksumProvider
{
    /**
     * Returns checksum of group state.
     *
     * @return checksum of group state
     */
    long getStateChecksum();
}
