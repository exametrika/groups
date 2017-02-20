/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;

/**
 * The {@link MembershipSerializationRegistrar} is a registrar of membership serializers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class MembershipSerializationRegistrar implements ISerializationRegistrar
{
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new GroupSerializer());
        registry.register(new GroupAddressSerializer());
        registry.register(new MembershipDeltaSerializer());
        registry.register(new MembershipSerializer());
        registry.register(new NodeSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(GroupSerializer.ID);
        registry.unregister(GroupAddressSerializer.ID);
        registry.unregister(MembershipDeltaSerializer.ID);
        registry.unregister(MembershipSerializer.ID);
        registry.unregister(NodeSerializer.ID);
    }
}
