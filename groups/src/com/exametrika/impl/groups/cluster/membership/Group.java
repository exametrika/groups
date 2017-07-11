/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link Group} is implementation of {@link IGroup}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Group implements IGroup
{
    private final UUID id;
    private final long changeId;
    private final String name;
    private final boolean primary;
    private final INode coordinator;
    private final List<INode> members;
    private final Map<UUID, INode> membersByIdMap;
    private final Map<IAddress, INode> membersByAddressMap;
    private final GroupAddress address;
    private final Set<GroupOption> options;

    public Group(GroupAddress address, boolean primary, List<INode> members, Set<GroupOption> options, long changeId)
    {
        Assert.notNull(address);
        Assert.notNull(members);
        Assert.isTrue(!members.isEmpty());
        Assert.notNull(options);
        Assert.isTrue(changeId > 0);

        this.id = address.getId();
        this.name = address.getName();
        this.options = Immutables.wrap(options);
        this.primary = primary;
        this.coordinator = members.get(0);
        this.members = Immutables.wrap(members);
        
        Map<UUID, INode> membersByIdMap = new HashMap<UUID, INode>();
        Map<IAddress, INode> membersByAddressMap = new HashMap<IAddress, INode>();
        for (INode member : members)
        {
            membersByIdMap.put(member.getId(), member);
            membersByAddressMap.put(member.getAddress(), member);
        }
        
        this.membersByIdMap = membersByIdMap;
        this.membersByAddressMap = membersByAddressMap;
        this.address = address;
        this.changeId = changeId;
    }

    @Override
    public UUID getId()
    {
        return id;
    }

    @Override
    public long getChangeId()
    {
        return changeId;
    }
    
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public GroupAddress getAddress()
    {
        return address;
    }
    
    @Override
    public Set<GroupOption> getOptions()
    {
        return options;
    }
    
    @Override
    public boolean isPrimary()
    {
        return primary;
    }
    
    @Override
    public INode getCoordinator()
    {
        return coordinator;
    }

    @Override
    public List<INode> getMembers()
    {
        return members;
    }

    @Override
    public INode findMember(UUID nodeId)
    {
        Assert.notNull(nodeId);
        
        return membersByIdMap.get(nodeId);
    }

    @Override
    public INode findMember(IAddress address)
    {
        Assert.notNull(address);
        
        return membersByAddressMap.get(address);
    }

    @Override
    public int compareTo(IGroup o)
    {
        return id.compareTo(o.getId());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof Group))
            return false;

        Group group = (Group)o;
        return id.equals(group.id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public String toString()
    {
        return MessageFormat.format("{0}{1}[{2}->{3}]", primary ? "" : "-", name, coordinator.toString(), members.toString());
    }
}
