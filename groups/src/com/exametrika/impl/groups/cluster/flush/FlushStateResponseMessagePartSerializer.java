/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.flush;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipDelta;

/**
 * The {@link FlushStateResponseMessagePartSerializer} is a serializer for {@link FlushStateResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushStateResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("30b168d4-158f-45aa-9bc3-e03a71d0aa75");
 
    public FlushStateResponseMessagePartSerializer()
    {
        super(ID, FlushStateResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlushStateResponseMessagePart part = (FlushStateResponseMessagePart)object;
        Serializers.writeEnum(serialization, part.getPhase());
        serialization.writeTypedObject(part.getPreparedMembership());
        serialization.writeTypedObject(part.getPreparedMembershipDelta());
        serialization.writeBoolean(part.isFlushProcessingRequired());
        
        serialization.writeInt(part.getFailedMembers().size());
        for (UUID nodeId : part.getFailedMembers())
            Serializers.writeUUID(serialization, nodeId);
        
        serialization.writeInt(part.getLeftMembers().size());
        for (UUID nodeId : part.getLeftMembers())
            Serializers.writeUUID(serialization, nodeId);
        
        serialization.writeInt(part.getCoordinatorStates().size());
        for (Object state : part.getCoordinatorStates())
            serialization.writeObject(state);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        FlushParticipantProtocol.Phase phase = Serializers.readEnum(deserialization, FlushParticipantProtocol.Phase.class);
        IGroupMembership preparedMembership = deserialization.readTypedObject(GroupMembership.class);
        IGroupMembershipDelta preparedMembershipDelta = deserialization.readTypedObject(GroupMembershipDelta.class);
        boolean flushProcessingRequired = deserialization.readBoolean();

        int count = deserialization.readInt();
        Set<UUID> failedMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            failedMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        Set<UUID> leftMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            leftMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        List<Object> coordinatorStates = new ArrayList<Object>();
        for (int i = 0; i < count; i++)
            coordinatorStates.add(deserialization.readObject());
        
        return new FlushStateResponseMessagePart(phase, preparedMembership, preparedMembershipDelta, flushProcessingRequired, 
            failedMembers, leftMembers, coordinatorStates);
    }
}
