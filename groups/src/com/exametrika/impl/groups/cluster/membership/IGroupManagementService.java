/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.UUID;

/**
 * The {@link IGroupManagementService} represents a group management service.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */

public interface IGroupManagementService
{
    /**
     * Returns list of group definitions.
     *
     * @return  list of group definitions
     */
    List<GroupDefinition> getGroupDefinitions();
    
    /**
     * Adds group definition.
     *
     * @param group group definition
     */
    void addGroupDefinition(GroupDefinition group);
    
    /**
     * Removes group definition.
     *
     * @param domainName group domain name
     * @param groupId group identifier
     */
    void removeGroupDefinition(String domainName, UUID groupId);
}