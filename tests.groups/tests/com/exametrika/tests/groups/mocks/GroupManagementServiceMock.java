/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.impl.groups.cluster.membership.GroupDefinition;
import com.exametrika.impl.groups.cluster.membership.IGroupManagementService;

public class GroupManagementServiceMock implements IGroupManagementService
{
    public List<GroupDefinition> groupDefinitions = new ArrayList<GroupDefinition>();
    
    @Override
    public List<GroupDefinition> getGroupDefinitions()
    {
        return groupDefinitions;
    }

    @Override
    public void addGroupDefinition(GroupDefinition group)
    {
        groupDefinitions.add(group);
    }

    @Override
    public void removeGroupDefinition(String domainName, UUID groupId)
    {
    }
}