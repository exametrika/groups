/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.groups.cluster.channel.IPropertyProvider;

/**
 * The {@link LocalNodeProvider} provides local node.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class LocalNodeProvider
{
    private final INode localNode;
    
    public LocalNodeProvider(ILiveNodeProvider liveNodeProvider, IPropertyProvider propertyProvider, String domainName)
    {
        Assert.notNull(liveNodeProvider);
        Assert.notNull(propertyProvider);
        Assert.notNull(domainName);

        localNode = new Node(liveNodeProvider.getLocalNode(), propertyProvider.getProperties(), domainName);
    }
    
    public INode getLocalNode()
    {
        return localNode;
    }
}
