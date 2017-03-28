/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameterHighlighter;
import com.exametrika.common.shell.impl.ShellHighlighter.TokenInfo;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;

/**
 * The {@link ShellCompleter} is a shell command completer.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellCompleter implements Completer
{
    // TODO:
    // - привязать к TokenInfo параметры и их аргументы(парсингом аналргично остальным), и сложить в Set все найденные параметры
    // - взять последний аргумент, если строка больше, и привязан параметр с аргументом, комлетить аргумент,
    //   если нет параметра или нетаргумента, взять любой не заданный параметр командыне или заданный не уникальный
    // - если последний аргумент равен строке (пробелов между последним параметром и окончанием строки нет). Найти предыдущий
    //   аргумент, если он привязан к параметру с аргументом, комлетить аргумент по заданному префиксу, иначе комплетить
    //   параметр по заданному префиксу среди любых не заданеых или заданеых неуникальных
    private final ShellNode rootNode;
    private final IShellCommand defaultCommand;
    private final char nameSeparator;
    private final IShellContext context;

    public ShellCompleter(ShellNode rootNode, IShellCommand defaultCommand, char nameSeparator,
        IShellContext context)
    {
        Assert.notNull(rootNode);
        Assert.notNull(context);
        
        this.rootNode = rootNode;
        this.defaultCommand = defaultCommand;
        this.nameSeparator = nameSeparator;
        this.context = context;
    }
    
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates)
    {
        List<TokenInfo> args = parseArgs(line.line().substring(0, line.cursor()));
        // TODO Auto-generated method stub
        
    }
    
    private CompletionInfo parseCommands(String context, String line, List<TokenInfo> args)
    {
        boolean first = true;
        IShellCommand command = null;
        List<TokenInfo> commandArgs = new ArrayList<TokenInfo>();
        for (int i = 0; i < args.size(); i++)
        {
            TokenInfo arg = args.get(i);
            boolean last = i == args.size() - 1;
            if (first)
            {
                String prefix;
                if (!context.isEmpty())
                    prefix = context + nameSeparator;
                else
                    prefix = "";
                
                String commandName = prefix + line.substring(arg.start, arg.start + arg.length);
                command = rootNode.find(commandName, nameSeparator);
                if (command == null)
                    command = defaultCommand;
                
                if (last)
                {
                    CompletionInfo completionInfo = new CompletionInfo();
                    if (arg.start + arg.length == line.length())
                    {
                        completionInfo.type = CompletionType.COMMAND;
                        completionInfo.prefix = commandName;
                    }
                    else if (command != null)
                    {
                        completionInfo.type = CompletionType.PARAMETER;
                        completionInfo.prefix = "";
                        completionInfo.command = command;
                    }
                    else
                        completionInfo = null;
                    
                    return completionInfo;
                }
                
                if (command == null)
                    return null;
               
                first = false;
            }
            else
                commandArgs.add(arg);
        }
        
        Assert.notNull(command);
        parseCommandParameters(command, line, commandArgs);
    }
    
    private List<TokenInfo> parseArgs(String argsStr)
    {
        List<TokenInfo> args = new ArrayList<TokenInfo>();
        int start = 0;
        int length = 0;
        boolean quote = false;
        for (int i = 0; i < argsStr.length(); i++)
        {
            char ch = argsStr.charAt(i);
            if (ch == '\"')
            {
                quote = !quote;
                continue;
            }
            
            if (!quote && (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == ShellCommandParser.PIPE))
            {
                if (length > 0)
                {
                    TokenInfo token = new TokenInfo();
                    token.start = start;
                    token.length = length;
                    args.add(token);
                }
                
                if (ch == ShellCommandParser.PIPE)
                    args.clear();
                
                start = i + 1;
                length = 0;
                continue;
            }
            else
                length++;
        }
        
        if (length > 0)
        {
            TokenInfo token = new TokenInfo();
            token.start = start;
            token.length = length;
            args.add(token);
        }

        return args;
    }
    
    private static class TokenInfo
    {
        int start;
        int length;
    }
    
    private enum CompletionType
    {
        COMMAND,
        PARAMETER,
        PARAMETER_ARGUMENT
    }
    
    private static class CompletionInfo
    {
        CompletionType type;
        String prefix;
        IShellCommand command;
    }
}