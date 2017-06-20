/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;
import com.exametrika.impl.groups.cluster.management.CommandManager;
import com.exametrika.impl.groups.cluster.management.ICommand;
import com.exametrika.impl.groups.cluster.management.ICommandHandler;

public class ClusterCommandsTests extends AbstractClusterTests
{
    private static final int CORE_NODE_COUNT = 5;
    private static final int WORKER_NODE_COUNT = 10;
    private List<TestComnandHandler> commandHandlers = new ArrayList<TestComnandHandler>();
    
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
    public void testCommands() throws Throwable
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        addCommandHandlers();
        
        CommandManager commandManager = findCommandManager(coreChannels.get(findNonCoordinators(1).iterator().next()));
        SyncCompletionHandler completionHandler = new SyncCompletionHandler();
        commandManager.execute(new TestCommand("test1"), completionHandler);
        commandManager.execute(new TestCommand("test2"), completionHandler);
        completionHandler.await(500);
        Threads.sleep(200);
        
        for (TestComnandHandler commandHandler : commandHandlers)
            assertThat(commandHandler.values, is(Arrays.asList("test1", "test2")));
    }
    
    private void addCommandHandlers() throws Throwable
    {
        for (CoreNodeChannel chanhel : coreChannels)
        {
            CommandManager commandManager = findCommandManager(chanhel);
            List<ICommandHandler> commandHandlers = Tests.get(commandManager, "commandHandlers");
            TestComnandHandler comnandHandler = new TestComnandHandler();
            this.commandHandlers.add(comnandHandler);
            commandHandlers.add(comnandHandler);
        }
    }
    
    public static class TestCommand implements ICommand, Serializable
    {
        public final String value;
        
        public TestCommand(String value)
        {
            this.value = value;
        }
    }
    
    public static class TestComnandHandler implements ICommandHandler
    {
        private final List<String> values = new ArrayList<String>();
        
        @Override
        public boolean supports(ICommand command)
        {
            return command instanceof TestCommand;
        }

        @Override
        public void execute(ICommand command)
        {
            TestCommand testCommand = (TestCommand)command;
            values.add(testCommand.value);
        }
    }
}
