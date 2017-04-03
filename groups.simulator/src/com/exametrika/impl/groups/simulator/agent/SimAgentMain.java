/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.groups.cluster.IGroupChannel;
import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.impl.ShellBuilder;
import com.exametrika.common.shell.impl.ShellCommandBuilder;
import com.exametrika.common.shell.impl.converters.IntegerConverter;
import com.exametrika.common.utils.Pair;




/**
 * The {@link SimAgentMain} represents an agent main class.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimAgentMain
{
    public static void main(String[] args) throws Exception
    {
        IShell shell = new ShellBuilder().title("Simulation agent.")
            .defaultCommand(new ShellCommandBuilder()
                .namedParameter().key("host").unique().names("-h", "--host").format("-h, --host <host-name> [default=<localhost-ip-address>]")
                    .description("Coordinator server host.").unique().hasArgument().defaultValue(InetAddress.getLocalHost().getHostAddress()).end()
                .namedParameter().key("port").names("-p", "--port").format("-p, --port <port> [default=1717]")
                    .description("Coordinator server port.").unique().hasArgument().converter(new IntegerConverter()).defaultValue(1717).end()
                .namedParameter().key("bindAddress").names("-b", "--bind-address").format("-b, --bind-address <address>")
                    .description("Agent bind address.").unique().hasArgument().end()
                .namedParameter().key("nodeCount").names("-n", "--node-count").format("-n, --node-count <value> [default=2]")
                    .description("Number of simulation nodes.").unique().hasArgument().converter(new IntegerConverter()).defaultValue(2).end()
                .build()).build();
        List<Pair<IShellCommand, Map<String, Object>>> list = shell.parse(args);
        if (list == null)
            return;
        
        Map<String, Object> parameters = list.get(0).getValue();
        
        int nodeCount = (int)parameters.get("nodeCount");
        List<SimAgentChannel> agentChannels = new ArrayList<SimAgentChannel>();
        List<IGroupChannel> groupChannels = new ArrayList<IGroupChannel>();
        SimGroupChannelFactory groupFactory = new SimGroupChannelFactory();
        groupFactory.init(nodeCount);
        
        for (int i = 0; i < nodeCount; i++)
        {
            SimAgentChannelFactory factory = new SimAgentChannelFactory();
            SimAgentChannel agentChannel = factory.createChannel("node" + i, (String)parameters.get("bindAddress"), (String)parameters.get("host"), 
                (int)parameters.get("port"));
            agentChannels.add(agentChannel);
            agentChannel.start();
            
            IGroupChannel groupChannel = groupFactory.createChannel(agentChannel.getExecutor(), i);
            groupChannels.add(groupChannel);
        }
        
        System.out.println("Press 'q' to exit...");
        while (System.in.read() != 'q')
            Thread.sleep(1000);
        
        for (int i = 0; i < nodeCount; i++)
        {
            agentChannels.get(i).stop();
            groupChannels.get(i).stop();
        }
    }
}
