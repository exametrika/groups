/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp.Capability;

import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.Templates;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellCommandProvider;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterCompleter;
import com.exametrika.common.shell.IShellParameterHighlighter;
import com.exametrika.common.shell.impl.completers.ShellFileCompleter;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Pair;
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
        return new ShellCommandsBuilder()
            .command().key("clear").names("clear", "cls").description(messages.clearCommand().toString()).executor(new ClearShellCommand()).end()
            .command().key("quit").names("quit", "exit", "q").description(messages.exitCommand().toString()).executor(new ExitShellCommand()).end()
            .command().key("help").names("help", "man", "?").description(messages.helpCommand().toString())
                .defaultParameter()
                    .key("command").format(messages.helpCommandParameterFormat().toString()) 
                    .description(messages.helpCommandParameterDescription().toString()).hasArgument()
                    .completer(new HelpCommandCompleter())
                    .highlighter(new HelpCommandHighlighter()).end()
                .executor(new HelpShellCommand()).end()
            .command().key("eval").names("eval").description(messages.evalCommand().toString())
                .defaultParameter()
                    .key("expression").format(messages.evalExpressionParameterFormat().toString()) 
                    .description(messages.evalExpressionParameterDescription().toString()).unique().required().hasArgument().end() 
                .executor(new EvalShellCommand()).end()
            .command().key("echo").names("echo").description(messages.echoCommand().toString())
                .defaultParameter()
                    .key("template").format(messages.echoTemplateParameterFormat().toString()) 
                    .description(messages.echoTemplateParameterDescription().toString()).unique().required().hasArgument().end() 
                .executor(new EchoShellCommand()).end()
            .command().key("grep").names("grep").description(messages.grepCommand().toString())
                .namedParameter()
                    .key("caseInsensitive").names("-c", "--case-insensitive").format(messages.grepCaseParameterFormat().toString()) 
                    .description(messages.grepCaseParameterDescription().toString()).unique().end()
                .positionalParameter()
                    .key("filter").format(messages.grepFilterParameterFormat().toString()) 
                    .description(messages.grepFilterParameterDescription().toString()).unique().hasArgument().end()
                .defaultParameter()
                    .key("expression").format(messages.grepExpressionParameterFormat().toString()) 
                    .description(messages.grepExpressionParameterDescription().toString()).hasArgument().end()
                .executor(new GrepShellCommand()).end()
            .command().key("load").names("load").description(messages.loadCommand().toString())
                .namedParameter()
                    .key("commandMode").names("-c", "--command-mode").format(messages.loadCommandModeParameterFormat().toString()) 
                    .description(messages.loadCommandModeParameterDescription().toString()).unique().end()
                .defaultParameter()
                    .key("scriptPath").format(messages.loadExpressionParameterFormat().toString()) 
                    .description(messages.loadExpressionParameterDescription().toString())
                    .unique().required().hasArgument()
                    .completer(new ShellFileCompleter()).end()
                .executor(new LoadShellCommand()).end()
            .command().key("alias").names("alias").description(messages.aliasCommand().toString())
                .namedParameter()
                    .key("description").names("-d", "--description").format(messages.aliasDescriptionParameterFormat().toString()) 
                    .description(messages.aliasDescriptionParameterDescription().toString()).unique().hasArgument().end()
                .positionalParameter()
                    .key("name").format(messages.aliasNameParameterFormat().toString()) 
                    .description(messages.aliasNameParameterDescription().toString()).unique().hasArgument().end()
                .defaultParameter()
                    .key("command").format(messages.aliasCommandParameterFormat().toString()) 
                    .description(messages.aliasCommandParameterDescription().toString()).unique().required().hasArgument().end()
                .executor(new AliasShellCommand()).end()
            .build();
    }
    
    private static class ClearShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
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
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
        {
            throw new UserInterruptException("");
        }
    }
    
    private static class HelpShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand c, IShellContext context, Map<String, Object> parameters)
        {
            ShellNode contextNode = ((Shell)context.getShell()).getContextNode();
            AttributedStringBuilder builder = new AttributedStringBuilder();
            builder.append("\n");
            
            List<String> commands = (List<String>)parameters.get("command");
            if (Collections.isEmpty(commands))
            {
                boolean first = true;
                Set<IShellCommand> processed = new HashSet<IShellCommand>();
                for (ShellNode child : contextNode.getChildren().values())
                {
                    if (processed.contains(child.getCommand()))
                        continue;
                    
                    if (first)
                        first = false;
                    else
                        builder.append("\n");
                    
                    if (!context.getShell().isNoColors())
                        builder.style(ShellStyles.COMMAND_STYLE);
                    builder.append(buildCommandNames(child.getName(), child.getCommand(), context.getShell().getNameSeparator()));
                    if (!context.getShell().isNoColors())
                        builder.style(ShellStyles.DEFAULT_STYLE);
                    
                    builder.append(" - ")
                        .append(child.getCommand().getDescription());
                    
                    processed.add(child.getCommand());
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
                        builder.appendAnsi(command.getUsage(!context.getShell().isNoColors(), false));
                    else 
                    {
                        if (!context.getShell().isNoColors())
                            builder.style(ShellStyles.ERROR_STYLE);
                        builder.append(messages.commandNotFound(commandName).toString());
                        if (!context.getShell().isNoColors())
                            builder.style(ShellStyles.DEFAULT_STYLE);
                    }
                }
            }
            
            return builder.toAnsi();
        }

        private String buildCommandNames(String firstName, IShellCommand command, char nameSeparator)
        {
            StringBuilder builder = new StringBuilder();
            builder.append(firstName);
            for (String name : command.getNames())
            {
                int pos = name.lastIndexOf(nameSeparator);
                if (pos != -1)
                    name = name.substring(pos + 1);

                if (name.equals(firstName))
                    continue;
                
                builder.append(", ");
                builder.append(name);
            }
            
            return builder.toString();
        }
    }
    
    private static class HelpCommandCompleter implements IShellParameterCompleter
    {
        @Override
        public List<Candidate> complete(IShellContext context, String value)
        {
            List<Candidate> candidates = new ArrayList<Candidate>();
            ShellNode contextNode = ((Shell)context.getShell()).getContextNode();
            for (ShellNode node : contextNode.getChildren().values())
            {
                Candidate candidate = new Candidate();
                candidate.value = node.getName();
                candidate.displayName = node.getName();
                candidate.description = node.getCommand().getShortDescription();
                candidates.add(candidate);
            }
            return candidates;
        }
    }
    
    private static class HelpCommandHighlighter implements IShellParameterHighlighter
    {
        @Override
        public String highlight(IShellContext context, String value)
        {
            ShellNode contextNode = ((Shell)context.getShell()).getContextNode();
            AttributedStringBuilder builder = new AttributedStringBuilder();
            if (contextNode.find(value) != null)
                builder.style(ShellStyles.PARAMETER_STYLE);
            else
                builder.style(ShellStyles.ERROR_STYLE);
                
            builder.append(value);
            return builder.toAnsi();
        }
    }
    
    private static class EvalShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
        {
            String expression = (String)parameters.get("expression");
            return Expressions.evaluate(expression, context, null);
        }
    }
    
    private static class EchoShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
        {
            String template = (String)parameters.get("template");
            return Templates.evaluate(template, context, null);
        }
    }
    
    private static class GrepShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
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
    
    private static class LoadShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
        {
            Boolean commandMode = (Boolean)parameters.get("commandMode");
            String scriptPath = (String)parameters.get("scriptPath");
            String scriptText = Files.read(new File(scriptPath));
            if (Boolean.TRUE.equals(commandMode))
                context.getShell().executeScript(scriptText);
            else
                Expressions.evaluate(scriptText, context, null);
            
            return null;
        }
    }
    
    private static class AliasShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
        {
            Pair<IShellCommand, Map<String, Object>> pair = ((ShellCommandParser)context.getShell().getParser())
                .parsePartialCommand((String)parameters.get("command"));
            
            command = pair.getKey();
            Map<String, Object> args = pair.getValue();
            String description = (String)parameters.get("description");
            if (description == null)
                description = command.getDescription();

            List<IShellParameter> namedParameters = new ArrayList<IShellParameter>();
            for (IShellParameter parameter : command.getNamedParameters())
            {
                if (!args.containsKey(parameter.getKey()))
                    namedParameters.add(parameter);
            }
            
            List<IShellParameter> positionalParameters = new ArrayList<IShellParameter>();
            for (IShellParameter parameter : command.getPositionalParameters())
            {
                if (!args.containsKey(parameter.getKey()))
                    positionalParameters.add(parameter);
            }
            
            IShellParameter defaultParameter;
            if (!args.containsKey(command.getDefaultParameter().getKey()))
                defaultParameter = command.getDefaultParameter();
            else
                defaultParameter = null;
            
            IShellCommand alias = new ShellCommand(command.getKey(), Arrays.asList((String)parameters.get("name")), 
                description, command.getShortDescription(), command.getValidator(), namedParameters, positionalParameters, 
                defaultParameter, new AliasCommandExecutor(command, args));
            ((Shell)context.getShell()).addCommand(alias);
            
            return null;
        }
    }
    
    private static class AliasCommandExecutor implements IShellCommandExecutor
    {
        private final IShellCommand command;
        private final Map<String, Object> args;

        public AliasCommandExecutor(IShellCommand command, Map<String, Object> args)
        {
            this.command = command;
            this.args = args;
        }
        
        @Override
        public Object execute(IShellCommand command, IShellContext context, Map<String, Object> parameters)
        {
            parameters = new LinkedHashMap<String, Object>(parameters);
            parameters.putAll(args);
            
            return this.command.getExecutor().execute(this.command, context, parameters);
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
        @DefaultMessage("<command> [many]")
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
        @DefaultMessage("prints value")
        ILocalizedMessage echoCommand();
        @DefaultMessage("<template>")
        ILocalizedMessage echoTemplateParameterFormat();
        @DefaultMessage("template to print")
        ILocalizedMessage echoTemplateParameterDescription();
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
        @DefaultMessage("<expression> [many]")
        ILocalizedMessage grepExpressionParameterFormat();
        @DefaultMessage("expression to filter")
        ILocalizedMessage grepExpressionParameterDescription();
        @DefaultMessage("loads script")
        ILocalizedMessage loadCommand();
        @DefaultMessage("-c --command-mode")
        ILocalizedMessage loadCommandModeParameterFormat();
        @DefaultMessage("if set script is loaded in command mode \nelse in expression evaluation mode")
        ILocalizedMessage loadCommandModeParameterDescription();
        @DefaultMessage("<script-file-path>")
        ILocalizedMessage loadExpressionParameterFormat();
        @DefaultMessage("path to script being loaded")
        ILocalizedMessage loadExpressionParameterDescription();
        @DefaultMessage("makes command alias")
        ILocalizedMessage aliasCommand();
        @DefaultMessage("-d --description")
        ILocalizedMessage aliasDescriptionParameterFormat();
        @DefaultMessage("command alias description")
        ILocalizedMessage aliasDescriptionParameterDescription();
        @DefaultMessage("<name>")
        ILocalizedMessage aliasNameParameterFormat();
        @DefaultMessage("command alias name")
        ILocalizedMessage aliasNameParameterDescription();
        @DefaultMessage("<command>")
        ILocalizedMessage aliasCommandParameterFormat();
        @DefaultMessage("command with parameters")
        ILocalizedMessage aliasCommandParameterDescription();
    }
}
