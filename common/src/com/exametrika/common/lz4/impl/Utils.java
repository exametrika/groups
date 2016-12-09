/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

import java.nio.ByteOrder;

public final class Utils
{
    public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();

    public static void checkRange(byte[] buf, int off)
    {
        if (off < 0 || off >= buf.length)
            throw new ArrayIndexOutOfBoundsException(off);
    }

    public static void checkRange(byte[] buf, int off, int len)
    {
        checkLength(len);
        if (len > 0)
        {
            checkRange(buf, off);
            checkRange(buf, off + len - 1);
        }
    }

    public static void checkLength(int len)
    {
        if (len < 0)
            throw new IllegalArgumentException("lengths must be >= 0");
    }

    public static int readIntBE(byte[] buf, int i)
    {
        return ((buf[i] & 0xFF) << 24) | ((buf[i + 1] & 0xFF) << 16) | ((buf[i + 2] & 0xFF) << 8) | (buf[i + 3] & 0xFF);
    }

    public static int readIntLE(byte[] buf, int i)
    {
        return (buf[i] & 0xFF) | ((buf[i + 1] & 0xFF) << 8) | ((buf[i + 2] & 0xFF) << 16) | ((buf[i + 3] & 0xFF) << 24);
    }

    public static int readInt(byte[] buf, int i)
    {
        if (NATIVE_BYTE_ORDER == ByteOrder.BIG_ENDIAN)
            return readIntBE(buf, i);
        else
            return readIntLE(buf, i);
    }

    private Utils()
    {
    }
}