/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jline.utils.AttributedStringBuilder;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellCommandProvider;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameterCompleter;
import com.exametrika.common.shell.IShellParameterHighlighter;
import com.exametrika.common.shell.impl.ShellCommandsBuilder;
import com.exametrika.common.shell.impl.ShellStyles;
import com.exametrika.common.shell.impl.converters.LongConverter;
import com.exametrika.common.utils.Assert;




/**
 * The {@link SimShellCommandProvider} represents a simulator shell command provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimShellCommandProvider implements IShellCommandProvider
{
    private final SimCoordinator coordinator;
    
    public SimShellCommandProvider(SimCoordinator coordinator)
    {
        Assert.notNull(coordinator);
        
        this.coordinator = coordinator;
    }
    
    @Override
    public List<IShellCommand> getCommands()
    {
        return new ShellCommandsBuilder()
            .namespace().key("sim").names("sim").description("Messaging simulation framework.").end()
            .command().key("start").names("sim:start").description("Starts simulation.")
                .namedParameter().key("delay").names("-d", "--delay").format("-d, --delay <millis> [default=5000ms]")
                    .description("Agent delay in milliseconds.").unique().hasArgument().converter(new LongConverter()).defaultValue(5000l).end() 
                .namedParameter().key("timeIncrement").names("-t", "--time-increment").format("-t, --time-increment <millis> [default=100ms]")
                    .description("Simulation time increment in milliseconds per 10s time tick.").unique().hasArgument()
                    .converter(new LongConverter()).defaultValue(100l).end()
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("delay").names("sim:delay").description("Delays simulation.")
                .namedParameter().key("period").names("-p", "--period").format("-p, --period <millis> [default=5000ms]")
                    .description("Agent delay in milliseconds.").unique().hasArgument().converter(new LongConverter()).defaultValue(5000l).end() 
                .namedParameter().key("oneTime").names("-o", "--one-time").format("-o, --one-time")
                    .description("If delay is one-time?").unique().end()
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("timeSpeed").names("sim:timeSpeed").description("Sets time speed.")
                .defaultParameter().key("increment").format("<increment millis> [default=100ms]")
                    .description("Simulation time increment in milliseconds per 10s time tick.").unique().hasArgument()
                    .converter(new LongConverter()).defaultValue(100l).end() 
                .executor(new SimShellCommandExecutor()).end()
            .command().key("time").names("sim:time").description("Time period since simulation start in simulation time.")
                .executor(new SimShellCommandExecutor()).end()
            .command().key("stop").names("sim:stop").description("Stops simulation.")
                .namedParameter().key("condition").names("-c", "--condition").format("-c, --condition <expression>")
                    .description("Stop condition.").unique().hasArgument().end() 
                .namedParameter().key("name").names("-n", "--name").format("-n, --name <name>")
                    .description("Stop condition name.").unique().hasArgument().end()
                .namedParameter().key("remove").names("-r", "--remove").format("-r, --remove")
                    .description("Removes stop condition.").unique().end()
                .namedParameter().key("list").names("-l", "--list").format("-l, --list")
                    .description("Lists stop conditions.").unique().end()
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("suspend").names("sim:suspend").description("Suspends simulation.")
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("resume").names("sim:resume").description("Resumes simulation.")
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("kill").names("sim:kill").description("Kills agents.")
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("print").names("sim:print").description("Prints current message.")
                .namedParameter().key("expression").names("-e", "--expression").format("-e, --expression <expression>")
                    .description("Transform expression.").unique().hasArgument().end() 
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("log").names("sim:log").description("Enables/disabled message logging.")
                .namedParameter().key("name").names("-n", "--name").format("-n, --name <name>")
                    .description("Log filter name.").unique().hasArgument().end()
                .namedParameter().key("remove").names("-r", "--remove").format("-r, --remove")
                    .description("Removes log filter.").unique().end()
                .namedParameter().key("list").names("-l", "--list").format("-l, --list")
                    .description("Lists log filters.").unique().end()
                .namedParameter().key("filter").names("-f", "--filter").format("-f, --filter <expression>")
                    .description("Filter expression.").unique().hasArgument().end()
                .namedParameter().key("expression").names("-e", "--expression").format("-e, --expression <expression>")
                    .description("Transform expression.").unique().hasArgument().end()
                .namedParameter().key("off").names("-o", "--off").format("-o, --off")
                    .description("Is logging disabled?.").unique().end()
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .command().key("sleep").names("sim:sleep").description("Sleeps current thread on specified number of milliseconds.")
                .defaultParameter()
                    .key("period").format("<period>").converter(new LongConverter()) 
                    .description("sleep period in milliseconds").unique().required().hasArgument().end() 
                .executor(new SimShellCommandExecutor()).end()
            .command().key("wait").names("sim:wait").description("Waits agent events.")
                .namedParameter().key("condition").names("-c", "--condition").format("-c, --condition <expression>")
                    .description("Wait condition.").unique().hasArgument().end() 
                .namedParameter().key("name").names("-n", "--name").format("-n, --name <name>")
                    .description("Wait condition name.").unique().hasArgument().end()
                .namedParameter().key("add").names("-a", "--add").format("-a, --add")
                    .description("Adds wait condition.").unique().end()
                .namedParameter().key("remove").names("-r", "--remove").format("-r, --remove")
                    .description("Removes wait condition.").unique().end()
                .namedParameter().key("list").names("-l", "--list").format("-l, --list")
                    .description("Lists wait conditions.").unique().end()
                .defaultParameter().key("agentNamePattern").format("<agent> [many]").description("Agent name glob/regexp pattern.")
                    .completer(new SimAgentNameCompleter()).highlighter(new SimAgentNameHighlighter()).end()
                .executor(new SimShellCommandExecutor()).end()
            .build();
    }
    
    private class SimShellCommandExecutor implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
        {
            return coordinator.execute(command.getKey(), parameters);
        }
    }
    
    private class SimAgentNameCompleter implements IShellParameterCompleter
    {
        @Override
        public List<Candidate> complete(IShellContext context, String value)
        {
            List<Candidate> candidates = new ArrayList<Candidate>();
            for (IAddress agent : coordinator.getChannel().getAgents().keySet())
            {
                Candidate candidate = new Candidate();
                candidate.value = agent.getName();
                candidate.displayName = agent.getName();
                candidates.add(candidate);
            }
            return candidates;
        }
    }
    
    private class SimAgentNameHighlighter implements IShellParameterHighlighter
    {
        @Override
        public String highlight(IShellContext context, String value)
        {
            AttributedStringBuilder builder = new AttributedStringBuilder();
            if (value.startsWith("#") || value.contains("*") || value.contains("?") || coordinator.getChannel().findAgent(value) != null)
                builder.style(ShellStyles.PARAMETER_STYLE);
            else
                builder.style(ShellStyles.ERROR_STYLE);
                
            builder.append(value);
            return builder.toAnsi();
        }
    }
}
