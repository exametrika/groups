/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp.Capability;

import com.exametrika.common.expression.Expressions;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellCommandProvider;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Strings;



/**
 * The {@link DefaultShellCommandProvider} is a default shell command provider.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class DefaultShellCommandProvider implements IShellCommandProvider
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    @Override
    public List<IShellCommand> getCommands()
    {
        return new ShellCommandBuilder()
            .name("clear").description(messages.clearCommand().toString()).executor(new ClearShellCommand()).addCommand()
            .name("exit").description(messages.exitCommand().toString()).executor(new ExitShellCommand()).addCommand()
            .name("quit").description(messages.exitCommand().toString()).executor(new ExitShellCommand()).addCommand()
            .name("help").description(messages.helpCommand().toString())
                .defaultParameter("command", messages.helpCommandParameterFormat().toString(), 
                    messages.helpCommandParameterDescription().toString(), false, false, null, null, null, null).executor(new HelpShellCommand()).addCommand()
            .name("eval").description(messages.evalCommand().toString())
                .defaultParameter("expression", messages.evalExpressionParameterFormat().toString(), 
                    messages.evalExpressionParameterDescription().toString(), true, true, null, null, null, null).executor(new EvalShellCommand()).addCommand()
            .name("grep").description(messages.grepCommand().toString())
                .addNamedParameter("caseInsensitive", Arrays.asList("-c", "--case-insensitive"), messages.grepCaseParameterFormat().toString(), 
                    messages.grepCaseParameterDescription().toString(), false)
                .addPositionalParameter("filter", messages.grepFilterParameterFormat().toString(), 
                    messages.grepFilterParameterDescription().toString(), null, null, null)
                .defaultParameter("expression", messages.grepExpressionParameterFormat().toString(), 
                    messages.grepExpressionParameterDescription().toString(), false, false, null, null, null, null).executor(new GrepShellCommand()).addCommand()
            .build();
    }
    
    private static class ClearShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            Terminal terminal = ((Shell)context.getShell()).getLineReader().getTerminal();
            terminal.puts(Capability.clear_screen);
            terminal.flush();
            return null;
        }
    }
    
    private static class ExitShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            throw new UserInterruptException("");
        }
    }
    
    private static class HelpShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            ShellNode contextNode = ((Shell)context.getShell()).getContextNode();
            AttributedStringBuilder builder = new AttributedStringBuilder();
            builder.append("\n");
            
            List<String> commands = (List<String>)parameters.get("command");
            if (Collections.isEmpty(commands))
            {
                boolean first = true;
                for (ShellNode child : contextNode.getChildren().values())
                {
                    if (first)
                        first = false;
                    else
                        builder.append("\n");
                    
                    if (!context.getShell().isNoColors())
                        builder.style(ShellConstants.COMMAND_STYLE);
                    builder.append(child.getName());
                    if (!context.getShell().isNoColors())
                        builder.style(ShellConstants.DEFAULT_STYLE);
                    
                    builder.append(" - ")
                        .append(child.getCommand().getDescription());
                }
            }
            else
            {
                boolean first = true;
                for (String commandName : commands)
                {
                    if (first)
                        first = false;
                    else
                        builder.append("\n\n");
                    
                    IShellCommand command = contextNode.find(commandName);
                    if (command != null)
                        builder.appendAnsi(command.getUsage(!context.getShell().isNoColors()));
                    else 
                    {
                        if (!context.getShell().isNoColors())
                            builder.style(ShellConstants.ERROR_STYLE);
                        builder.append(messages.commandNotFound(commandName).toString());
                        if (!context.getShell().isNoColors())
                            builder.style(ShellConstants.DEFAULT_STYLE);
                    }
                }
            }
            
            return builder.toAnsi();
        }
    }
    
    private static class EvalShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            String expression = (String)parameters.get("expression");
            return Expressions.evaluate(expression, context, null);
        }
    }
    
    private static class GrepShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            String filter = (String)parameters.get("filter");
            ICondition<String> condition = Strings.createFilterCondition(filter, !parameters.containsKey("caseInsensitive"));
            List<Object> expression = (List<Object>)parameters.get("expression");
            List<Object> result = new ArrayList<Object>();
            for (Object value : expression)
            {
                String[] parts = value.toString().split("[\r\n]");
                for (String part : parts)
                {
                    if (part.trim().isEmpty())
                        continue;
                    if (condition.evaluate(part))
                        result.add(part);
                }
            }
            
            return Strings.toString(result, false);
        }
    }
    
    interface IMessages
    {
        @DefaultMessage("clears the terminal screen")
        ILocalizedMessage clearCommand();
        @DefaultMessage("exits the shell")
        ILocalizedMessage exitCommand();
        @DefaultMessage("prints command usage")
        ILocalizedMessage helpCommand();
        @DefaultMessage("<command>")
        ILocalizedMessage helpCommandParameterFormat();
        @DefaultMessage("command to print usage for")
        ILocalizedMessage helpCommandParameterDescription();
        @DefaultMessage("Command ''{0}'' is not found.")
        ILocalizedMessage commandNotFound(String command);
        @DefaultMessage("evaluates expression")
        ILocalizedMessage evalCommand();
        @DefaultMessage("<expression>")
        ILocalizedMessage evalExpressionParameterFormat();
        @DefaultMessage("expression to evaluate")
        ILocalizedMessage evalExpressionParameterDescription();
        @DefaultMessage("filters expression")
        ILocalizedMessage grepCommand();
        @DefaultMessage("-c --case-insensitive")
        ILocalizedMessage grepCaseParameterFormat();
        @DefaultMessage("case insensitive filtering")
        ILocalizedMessage grepCaseParameterDescription();
        @DefaultMessage("<filter>")
        ILocalizedMessage grepFilterParameterFormat();
        @DefaultMessage("filter is glob or regexp pattern")
        ILocalizedMessage grepFilterParameterDescription();
        @DefaultMessage("<expression>")
        ILocalizedMessage grepExpressionParameterFormat();
        @DefaultMessage("expression to filter")
        ILocalizedMessage grepExpressionParameterDescription();
    }
}
