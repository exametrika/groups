/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import java.util.Arrays;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;
import com.exametrika.impl.groups.cluster.management.CommandManager;
import com.exametrika.impl.groups.cluster.membership.AddGroupsCommand;
import com.exametrika.impl.groups.cluster.membership.GroupDefinition;

public class ClusterMembershipTests extends AbstractClusterTests
{
    private static final int CORE_NODE_COUNT = 5;
    private static final int WORKER_NODE_COUNT = 10;
    
    @Before
    public void setUp()
    {
        createCluster(CORE_NODE_COUNT, WORKER_NODE_COUNT);
    }
    
    @After
    public void tearDown()
    {
        stopCluster();
    }
    
    @Test
    public void testMembership()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(Collections.asSet(WORKER_NODE_COUNT - 1));
        Threads.sleep(2000);
        
        checkWorkerNodesMembership(Collections.asSet(WORKER_NODE_COUNT - 1));
        
        GroupDefinition group1 = new GroupDefinition("test", UUID.randomUUID(), "group1", 
            Enums.of(GroupOption.DURABLE, GroupOption.ASYNC_STATE_TRANSFER, GroupOption.CHECK_STATE), null, 3, null);
        GroupDefinition group2 = new GroupDefinition("test", UUID.randomUUID(), "group2", 
            Enums.of(GroupOption.DURABLE, GroupOption.ASYNC_STATE_TRANSFER, GroupOption.CHECK_STATE), null, 3, null);
        GroupDefinition group3 = new GroupDefinition("test", UUID.randomUUID(), "group3", 
            Enums.of(GroupOption.DURABLE, GroupOption.ASYNC_STATE_TRANSFER, GroupOption.CHECK_STATE), null, 3, null);
        GroupDefinition group4 = new GroupDefinition("test", UUID.randomUUID(), "group4", 
            Enums.of(GroupOption.DURABLE, GroupOption.ASYNC_STATE_TRANSFER, GroupOption.CHECK_STATE), null, 3, null);
        
        CommandManager commandManager = findCommandManager(coreChannels.get(0));
        AddGroupsCommand addGroupsCommand = new AddGroupsCommand(Arrays.asList(group1, group2, group3));
        SyncCompletionHandler completionHandler = new SyncCompletionHandler();
        commandManager.execute(addGroupsCommand, completionHandler);
        completionHandler.await(5000);
        
        checkGroupsMembership();
        // TODO:
        // создать 3 группы через команды, проверить что есть в членстве по аналогии с узлами
        // создать нового воркера - стартовать последнего, остановить старого - первого, 
        // создать новую - четвертую, изменить вторую и удалить третью группы, проверить
        // создание и удаление через команды
    }
    
    private CommandManager findCommandManager(CoreNodeChannel coreNode)
    {
        return ((SubChannel)coreNode.getMainSubChannel()).getProtocolStack().find(CommandManager.class);
    }
}
