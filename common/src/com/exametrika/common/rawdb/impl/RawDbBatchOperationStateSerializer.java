/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.LinkedHashSet;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.rawdb.IRawBatchOperation;
import com.exametrika.common.rawdb.impl.RawDbBatchOperationState.PageCacheConstraint;


/**
 * The {@link RawDbBatchOperationStateSerializer} is serializer for {@link RawDbBatchOperationState}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawDbBatchOperationStateSerializer extends AbstractSerializer
{
    private static final UUID ID = UUID.fromString("c9851ce5-21be-4721-bd25-6a974d0a901f");
    
    public RawDbBatchOperationStateSerializer()
    {
        super(ID, RawDbBatchOperationState.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        RawDbBatchOperationState state = (RawDbBatchOperationState)object;
        
        serialization.writeObject(state.operation);
        
        serialization.writeBoolean(state.cachingEnabled);
        serialization.writeInt(state.nonCachedPagesInvalidationQueueSize);
        
        if (state.constraints != null)
        {
            serialization.writeBoolean(true);
            serialization.writeInt(state.constraints.size());
            for (PageCacheConstraint constraint : state.constraints)
            {
                serialization.writeInt(constraint.pageTypeIndex);
                serialization.writeString(constraint.category);
                serialization.writeLong(constraint.maxPageCacheSize);
            }
        }
        else
            serialization.writeBoolean(false);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        IRawBatchOperation operation = deserialization.readObject();
        RawDbBatchOperationState state = new RawDbBatchOperationState(operation);
        
        state.cachingEnabled = deserialization.readBoolean();
        state.nonCachedPagesInvalidationQueueSize = deserialization.readInt();
        
        if (deserialization.readBoolean())
        {
            int count = deserialization.readInt();
            state.constraints = new LinkedHashSet<PageCacheConstraint>(count);
            for (int i = 0; i < count; i++)
            {
                int pageTypeIndex = deserialization.readInt();
                String category = deserialization.readString();
                long maxPageCacheSize = deserialization.readLong();
                state.constraints.add(new PageCacheConstraint(pageTypeIndex, category, maxPageCacheSize));
            }
        }
        
        return state;
    }
}
