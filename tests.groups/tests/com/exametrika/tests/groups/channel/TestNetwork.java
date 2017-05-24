/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Times;

/**
 * The {@link TestNetwork} is a test network.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class TestNetwork 
{
    private final List<TestProtocolStack> nodes;
    private final Map<IAddress, TestProtocolStack> nodesMap;
    private final Random random;

    public TestNetwork(List<TestProtocolStack> nodes, long seed)
    {
        Assert.notNull(nodes);
        
        this.nodes = nodes;
        this.random = new Random(seed);
        Map<IAddress, TestProtocolStack> nodesMap = new HashMap<IAddress, TestProtocolStack>();
        for (TestProtocolStack stack : nodes)
            nodesMap.put(stack.getAddress(), stack);
        
        this.nodesMap = Immutables.wrap(nodesMap);
    }
    
    public List<TestProtocolStack> getNodes()
    {
        return nodes;
    }
    
    public TestProtocolStack findNode(IAddress address)
    {
        return nodesMap.get(address);
    }
    
    public void start()
    {
        for (TestProtocolStack stack : nodes)
            stack.start();
    }
    
    public void stop()
    {
        for (TestProtocolStack stack : nodes)
            stack.stop();
    }
    
    public void onTimer(long currentTime)
    {
        Times.setTest(currentTime);
        for (TestProtocolStack stack : nodes)
            stack.onTimer(currentTime);
    }
    
    public void process(int roundCount, long timeIncrement)
    {
        process(roundCount, timeIncrement, null, Collections.<TestProtocolStack>emptySet());
    }
    
    public void process(int roundCount, long timeIncrement, TestProtocolStack ignoredDestination, Set<TestProtocolStack> ignoredNodes)
    {
        for (int i = 0; i < roundCount; i++)
        {
            int k = 0;
            while (true)
            {
                List<TestProtocolStack> nodes = new ArrayList<TestProtocolStack>(this.nodes);
                java.util.Collections.shuffle(nodes, random);
                boolean sent = false;
                for (TestProtocolStack stack : nodes)
                {
                    if (!stack.isActive())
                        continue;
                    if (k < stack.getSentMessages().size())
                    {
                        IMessage message = stack.getSentMessages().get(k);
                        TestProtocolStack destination = nodesMap.get(message.getDestination());
                        Assert.notNull(destination);
                        
                        if (ignoredNodes.isEmpty())
                            destination.receive(message);
                        else if (!(ignoredNodes.contains(destination) && (ignoredDestination == null ||
                            destination.equals(ignoredDestination))))
                            destination.receive(message);
                        sent = true;
                    }
                }
                k++;
                if (!sent)
                    break;
            }
            
            for (TestProtocolStack stack : nodes)
                stack.clearSentMessages();
            
            onTimer(Times.getCurrentTime() + timeIncrement);
        }
    }
}
