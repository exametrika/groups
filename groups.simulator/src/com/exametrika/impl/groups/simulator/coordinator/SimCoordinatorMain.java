/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.impl.ShellBuilder;
import com.exametrika.common.shell.impl.ShellCommandBuilder;
import com.exametrika.common.shell.impl.converters.IntegerConverter;
import com.exametrika.common.utils.Pair;




/**
 * The {@link SimCoordinatorMain} represents a coordinator main class.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimCoordinatorMain
{
    public static void main(String[] args) throws Exception
    {
        IShell shell = new ShellBuilder().title("Simulation coordinator.")
            .defaultCommand(new ShellCommandBuilder()
                .namedParameter().key("port").names("-p", "--port").format("-p, --port <port> [default=1717]")
                    .description("Coordinator server port.").unique().hasArgument().converter(new IntegerConverter()).defaultValue(1717).end()
                .namedParameter().key("bindAddress").names("-b", "--bind-address").format("-b, --bind-address <address> [default=<local-ip-address>]")
                    .description("Coordinator server bind address.").unique().hasArgument().defaultValue(InetAddress.getLocalHost().getHostAddress()).end()
                .namedParameter().key("historyFilePath").unique().names("-h", "--history-file").format("-h, --history-file <path>")
                    .description("Coordinator history file path.").unique().hasArgument().end()
                .build()).build();
        List<Pair<IShellCommand, Map<String, Object>>> list = shell.parse(args);
        if (list == null)
            return;
        
        Map<String, Object> parameters = list.get(0).getValue();
        
        SimCoordinatorChannelFactory factory = new SimCoordinatorChannelFactory();
        SimCoordinatorChannel channel = factory.createChannel("coordinator", (int)parameters.get("port"), (String)parameters.get("bindAddress"));
        shell = new ShellBuilder().title("Simulation coordinator.")
            .commands(new SimShellCommandProvider(channel.getCoordinator()).getCommands())
            .historyFilePath((String)parameters.get("historyFilePath")).build();
        channel.getCoordinator().setShell(shell);
        
        channel.start();
        
        shell.run();
        
        channel.stop();
    }
}
