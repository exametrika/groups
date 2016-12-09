/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;




/**
 * The {@link TcpPacket} is a TCP packet.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class TcpPacket
{
    private final List<ByteArray> buffers;
    private final List<File> files;
    private final int size;
    private final Object digest;

    public TcpPacket(ByteArray buffer)
    {
        this(Collections.singletonList(buffer), null, null);
    }
    
    public TcpPacket(List<ByteArray> buffers, List<File> files, Object digest)
    {
        Assert.notNull(buffers);
        Assert.isTrue(!buffers.isEmpty());
        
        this.buffers = Immutables.wrap(buffers);
        this.files = Immutables.wrap(files);
        
        int size = 0;
        for (int i = 0; i < buffers.size(); i++)
            size += buffers.get(i).getLength();
        
        this.size = size;
        this.digest = digest;
    }

    public List<ByteArray> getBuffers()
    {
        return buffers;
    }

    public List<File> getFiles()
    {
        return files;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public <T> T getDigest()
    {
        return (T)digest;
    }
    
    public void cleanup()
    {
        if (files != null)
        {
            for (File file : files)
                file.delete();
        }
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        else if (!(o instanceof TcpPacket))
            return false;
        
        TcpPacket packet = (TcpPacket)o;
        return buffers.equals(packet.buffers) && Objects.equals(files, packet.files);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(buffers, files);
    }
    
    @Override
    public String toString()
    {
        return (files != null ? files + ", " : "") + buffers.toString(); 
    }
}
