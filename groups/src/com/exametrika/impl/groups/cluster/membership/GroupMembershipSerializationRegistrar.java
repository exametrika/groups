/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;

/**
 * The {@link GroupMembershipSerializationRegistrar} is a registrar of membership serializers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupMembershipSerializationRegistrar implements ISerializationRegistrar
{
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new GroupSerializer());
        registry.register(new GroupDeltaSerializer());
        registry.register(new GroupAddressSerializer());
        registry.register(new GroupMembershipDeltaSerializer());
        registry.register(new GroupMembershipSerializer());
        registry.register(new NodeSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(GroupSerializer.ID);
        registry.unregister(GroupDeltaSerializer.ID);
        registry.unregister(GroupAddressSerializer.ID);
        registry.unregister(GroupMembershipDeltaSerializer.ID);
        registry.unregister(GroupMembershipSerializer.ID);
        registry.unregister(NodeSerializer.ID);
    }
}
