/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.check;

import com.exametrika.common.utils.ICompletionHandler;

/**
 * The {@link IGroupStateHashProvider} is used to provide MD5 hash of group state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupStateHashProvider
{
    /**
     * Computes MD5 hash of group state.
     *
     * @param completionHandler compketuon handler
     */
    void computeStateHash(ICompletionHandler<String> completionHandler);
}
