/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;

/**
 * The {@link ClusterMembershipSerializationRegistrar} is a registrar of membership serializers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class ClusterMembershipSerializationRegistrar implements ISerializationRegistrar
{
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new ClusterMembershipDeltaSerializer());
        registry.register(new DomainMembershipDeltaSerializer());
        registry.register(new NodesMembershipDeltaSerializer());
        registry.register(new WorkerToCoreMembershipDeltaSerializer());
        registry.register(new NodeSerializer());
        registry.register(new ClusterMembershipMessagePartSerializer());
        registry.register(new ClusterMembershipResponseMessagePartSerializer());
        registry.register(new GroupSerializer());
        registry.register(new GroupAddressSerializer());
        registry.register(new GroupDeltaSerializer());
        registry.register(new GroupsMembershipDeltaSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(ClusterMembershipDeltaSerializer.ID);
        registry.unregister(DomainMembershipDeltaSerializer.ID);
        registry.unregister(NodesMembershipDeltaSerializer.ID);
        registry.unregister(WorkerToCoreMembershipDeltaSerializer.ID);
        registry.unregister(NodeSerializer.ID);
        registry.unregister(ClusterMembershipMessagePartSerializer.ID);
        registry.unregister(ClusterMembershipResponseMessagePartSerializer.ID);
        registry.unregister(GroupSerializer.ID);
        registry.unregister(GroupAddressSerializer.ID);
        registry.unregister(GroupDeltaSerializer.ID);
        registry.unregister(GroupsMembershipDeltaSerializer.ID);
    }
}
