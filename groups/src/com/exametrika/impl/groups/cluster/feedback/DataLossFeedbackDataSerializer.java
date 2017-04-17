/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link DataLossFeedbackDataSerializer} is a serializer of {@link DataLossFeedbackData}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class DataLossFeedbackDataSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("7ee36747-5912-44da-b4ab-9dc96987287b");
    
    public DataLossFeedbackDataSerializer()
    {
        super(ID, DataLossFeedbackData.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        List<IDataLossState> states = new ArrayList<IDataLossState>(count);
        for (int i = 0; i < count; i++)
        {
            String domain = deserialization.readString();
            UUID groupId = Serializers.readUUID(deserialization);
            
            states.add(new DataLossState(domain, groupId));
        }
        
        return new DataLossFeedbackData(states);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        DataLossFeedbackData data = (DataLossFeedbackData)object;
        serialization.writeInt(data.getStates().size());
        
        for (IDataLossState state : data.getStates())
        {
            serialization.writeString(state.getDomain());
            Serializers.writeUUID(serialization, state.getId());
        }
    }
}
