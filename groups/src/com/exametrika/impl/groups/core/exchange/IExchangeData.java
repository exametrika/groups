/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.exchange;




/**
 * The {@link IExchangeData} is a data used in data exchange between nodes.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IExchangeData
{
    /**
     * Returns unique identifier of exchanged data.
     *
     * @return unique identifier of exchanged data monotonically increasing in case of changes
     */
    long getId();
    
    /**
     * Returns estimated size of data.
     *
     * @return estimated size of data
     */
    int getSize();
}
