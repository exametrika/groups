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
import com.exametrika.impl.groups.cluster.feedback.IGroupState.State;

/**
 * The {@link GroupFeedbackDataSerializer} is a serializer of {@link GroupFeedbackData}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupFeedbackDataSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("78678240-bdf5-4f05-86a4-e76889c07a7f");
    
    public GroupFeedbackDataSerializer()
    {
        super(ID, GroupFeedbackData.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        List<IGroupState> states = new ArrayList<IGroupState>(count);
        for (int i = 0; i < count; i++)
        {
            String domain = deserialization.readString();
            UUID groupId = Serializers.readUUID(deserialization);
            long membershipId = deserialization.readLong();
            
            int memberCount = deserialization.readInt();
            List<UUID> members = new ArrayList<UUID>(memberCount);
            for (int k = 0; k < memberCount; k++)
                members.add(Serializers.readUUID(deserialization));
            
            boolean primary = deserialization.readBoolean();
            State state = Serializers.readEnum(deserialization, State.class);
            
            states.add(new GroupState(domain, groupId, membershipId, members, primary, state));
        }
        
        return new GroupFeedbackData(states);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupFeedbackData data = (GroupFeedbackData)object;
        serialization.writeInt(data.getStates().size());
        
        for (IGroupState state : data.getStates())
        {
            serialization.writeString(state.getDomain());
            Serializers.writeUUID(serialization, state.getId());
            serialization.writeLong(state.getMembershipId());
            
            serialization.writeInt(state.getMembers().size());
            for (UUID id : state.getMembers())
                Serializers.writeUUID(serialization, id);
            
            serialization.writeBoolean(state.isPrimary());
            Serializers.writeEnum(serialization, state.getState());
        }
    }
}
