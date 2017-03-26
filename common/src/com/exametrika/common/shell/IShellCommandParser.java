/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.List;
import java.util.Map;

import com.exametrika.common.utils.Pair;

/**
 * The {@link IShellCommandParser} is a shell command parser.
 * 
 * @author Medvedev-A
 */
public interface IShellCommandParser
{
    /**
     * Parses commands. 
     *
     * @param context current command path context.
     * @param argsStr input command line. Commands can be separated by '|' (pipe), when they are executed sequentially.
     *                If argument contains spaces or tabs, it must be enclosed in double quotes.
     * @return list of pairs (command:parsed arguments)
     */
    List<Pair<IShellCommand, Map<String, Object>>> parseCommands(String context, String argsStr);
    
    /**
     * Parses commands. 
     *
     * @param context current command path context.
     * @param args input arguments. Different commands must be separated by '|' (pipe) argument, when they are executed sequentially.
     * @return list of pairs (command:parsed arguments)
     */
    List<Pair<IShellCommand, Map<String, Object>>> parseCommands(String context, List<String> args);
}