/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.util.Arrays;
import java.util.List;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandProvider;
import com.exametrika.common.shell.impl.ShellCommandBuilder;
import com.exametrika.common.shell.impl.DefaultShellCommandProvider.GrepShellCommand;




/**
 * The {@link SimShellCommandProvider} represents a simulator shell command provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimShellCommandProvider implements IShellCommandProvider
{
    @Override
    public List<IShellCommand> getCommands()
    {
        return new ShellCommandBuilder()
            .names("sim:start").description("Starts simulation.")
                .addNamedParameter("delay", Arrays.asList("-d", "--delay"), "-d, --delay <millis> [required, unique]", "Agent delay in milliseconds.", 
                    null, true)
                .addNamedParameter("timeIncrement", Arrays.asList("-t", "--time-increment"), "-t, --time-increment <millis> [required, unique]", "Agent delay in milliseconds.", 
                    null, true)
                .addPositionalParameter("filter", messages.grepFilterParameterFormat().toString(), 
                    messages.grepFilterParameterDescription().toString(), null, null, null, null)
                .defaultParameter("expression", messages.grepExpressionParameterFormat().toString(), 
                    messages.grepExpressionParameterDescription().toString(), null, false, false, null, null, null, 
                    null).executor(new GrepShellCommand()).addCommand()
            .build();
    }
}
