/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.lz4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.utils.Times;


/**
 * The {@link LZ4PerfTests} are performance tests for LZ4 compression.
 * 
 * @author Medvedev-A
 */
public class LZ4PerfTests
{
    @Test
    public void testFastCompression()
    {
        byte[] small = createBuffer(100);
        byte[] smallCompressed = new byte[LZ4.maxCompressedLength(small.length)];
        byte[] medium = createBuffer(10000);
        byte[] mediumCompressed = new byte[LZ4.maxCompressedLength(medium.length)];
        byte[] large = createBuffer(10000000);
        byte[] largeCompressed = new byte[LZ4.maxCompressedLength(large.length)];
        
        int smallCompressedLength = 0;
        int mediumCompressedLength = 0;
        int largeCompressedLength = 0;
        
        long t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            smallCompressedLength = LZ4.compress(true, small, 0, small.length, smallCompressed, 0, smallCompressed.length);
        System.out.println("fast small compression time:" + (Times.getCurrentTime() - t) + ", size: " + 
            small.length + ", compressed size: " + smallCompressedLength);
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            mediumCompressedLength = LZ4.compress(true, medium, 0, medium.length, mediumCompressed, 0, mediumCompressed.length);
        System.out.println("fast medium compression time:" + (Times.getCurrentTime() - t) + ", size: " + 
            medium.length + ", compressed size: " + mediumCompressedLength);
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            largeCompressedLength = LZ4.compress(true, large, 0, large.length, largeCompressed, 0, largeCompressed.length);
        System.out.println("fast large compression time:" + (Times.getCurrentTime() - t) + ", size: " + 
            large.length + ", compressed size: " + largeCompressedLength);
        
        byte[] small2 = new byte[small.length];
        byte[] medium2 = new byte[medium.length];
        byte[] large2 = new byte[large.length];
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            LZ4.decompress(smallCompressed, 0, small2, 0, small2.length);
        System.out.println("fast small decompression time:" + (Times.getCurrentTime() - t));
        assertThat(Arrays.equals(small, small2), is(true));
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            LZ4.decompress(mediumCompressed, 0, medium2, 0, medium2.length);
        System.out.println("fast medium decompression time:" + (Times.getCurrentTime() - t));
        assertThat(Arrays.equals(medium, medium2), is(true));
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            LZ4.decompress(largeCompressed, 0, large2, 0, large2.length);
        System.out.println("fast large decompression time:" + (Times.getCurrentTime() - t));
        assertThat(Arrays.equals(large, large2), is(true));
    }
    
    @Test
    public void testHighCompression()
    {
        byte[] small = createBuffer(100);
        byte[] smallCompressed = new byte[LZ4.maxCompressedLength(small.length)];
        byte[] medium = createBuffer(10000);
        byte[] mediumCompressed = new byte[LZ4.maxCompressedLength(medium.length)];
        byte[] large = createBuffer(10000000);
        byte[] largeCompressed = new byte[LZ4.maxCompressedLength(large.length)];
        
        int smallCompressedLength = 0;
        int mediumCompressedLength = 0;
        int largeCompressedLength = 0;
        
        long t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            smallCompressedLength = LZ4.compress(false, small, 0, small.length, smallCompressed, 0, smallCompressed.length);
        System.out.println("high small compression time:" + (Times.getCurrentTime() - t) + ", size: " + 
            small.length + ", compressed size: " + smallCompressedLength);
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            mediumCompressedLength = LZ4.compress(false, medium, 0, medium.length, mediumCompressed, 0, mediumCompressed.length);
        System.out.println("high medium compression time:" + (Times.getCurrentTime() - t) + ", size: " + 
            medium.length + ", compressed size: " + mediumCompressedLength);
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            largeCompressedLength = LZ4.compress(false, large, 0, large.length, largeCompressed, 0, largeCompressed.length);
        System.out.println("high large compression time:" + (Times.getCurrentTime() - t) + ", size: " + 
            large.length + ", compressed size: " + largeCompressedLength);
        
        byte[] small2 = new byte[small.length];
        byte[] medium2 = new byte[medium.length];
        byte[] large2 = new byte[large.length];
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            LZ4.decompress(smallCompressed, 0, small2, 0, small2.length);
        System.out.println("high small decompression time:" + (Times.getCurrentTime() - t));
        assertThat(Arrays.equals(small, small2), is(true));
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            LZ4.decompress(mediumCompressed, 0, medium2, 0, medium2.length);
        System.out.println("high medium decompression time:" + (Times.getCurrentTime() - t));
        assertThat(Arrays.equals(medium, medium2), is(true));
        
        t = Times.getCurrentTime();
        for (int i = 0; i < 1000; i++)
            LZ4.decompress(largeCompressed, 0, large2, 0, large2.length);
        System.out.println("high large decompression time:" + (Times.getCurrentTime() - t));
        assertThat(Arrays.equals(large, large2), is(true));
    }
    
    private byte[] createBuffer(int size)
    {
        byte[] buf = new byte[size];
        for (int i = 0; i < size; i++)
            buf[i] = (byte)(i & 31);
        
        return buf;
    }
}
