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
import com.exametrika.impl.groups.cluster.feedback.INodeState.State;

/**
 * The {@link NodeFeedbackDataSerializer} is a serializer of {@link NodeFeedbackData}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class NodeFeedbackDataSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("78678240-bdf5-4f05-86a4-e76889c07a7f");
    
    public NodeFeedbackDataSerializer()
    {
        super(ID, NodeFeedbackData.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        List<INodeState> states = new ArrayList<INodeState>(count);
        for (int i = 0; i < count; i++)
        {
            String domain = deserialization.readString();
            UUID nodeId = Serializers.readUUID(deserialization);
            
            State state = Serializers.readEnum(deserialization, State.class);
            
            states.add(new NodeState(domain, nodeId, state));
        }
        
        return new NodeFeedbackData(states);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        NodeFeedbackData data = (NodeFeedbackData)object;
        serialization.writeInt(data.getStates().size());
        
        for (INodeState state : data.getStates())
        {
            serialization.writeString(state.getDomain());
            Serializers.writeUUID(serialization, state.getId());
            Serializers.writeEnum(serialization, state.getState());
        }
    }
}
