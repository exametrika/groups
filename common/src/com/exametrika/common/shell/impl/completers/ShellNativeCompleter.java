/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.completers;

import java.util.List;

import org.jline.reader.Completer;

import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameterCompleter;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ShellNativeCompleter} is a shell completer which uses native jline completer.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellNativeCompleter implements IShellParameterCompleter
{
    private final Completer completer;
    
    public ShellNativeCompleter(Completer completer)
    {
        Assert.notNull(completer);
        
        this.completer = completer;
    }
    
    public Completer getCompleter()
    {
        return completer;
    }
    
    @Override
    public List<Candidate> complete(IShellContext context, String value)
    {
        return Assert.error();
    }
}