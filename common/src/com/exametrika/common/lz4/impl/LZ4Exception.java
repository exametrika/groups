/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

public final class LZ4Exception extends RuntimeException
{
    public LZ4Exception(String msg, Throwable t)
    {
        super(msg, t);
    }

    public LZ4Exception(String msg)
    {
        super(msg);
    }

    public LZ4Exception()
    {
    }
}