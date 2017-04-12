/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.completers;

import org.jline.reader.impl.completer.FileNameCompleter;

/**
 * The {@link ShellFileCompleter} is a file shell completer.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellFileCompleter extends ShellNativeCompleter
{
    public ShellFileCompleter()
    {
        super(new FileNameCompleter());
    }
}