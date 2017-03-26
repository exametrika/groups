/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;

/**
 * The {@link ShellCompleter} is a shell command completer.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellCompleter implements Completer
{
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates)
    {
        // TODO Auto-generated method stub
        
    }
}