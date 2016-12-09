/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4;

import com.exametrika.common.lz4.impl.LZ4Compressor;
import com.exametrika.common.lz4.impl.LZ4Decompressor;
import com.exametrika.common.lz4.impl.LZ4Exception;
import com.exametrika.common.lz4.impl.LZ4HighCompressor;
import com.exametrika.common.lz4.impl.XXHash32;
import com.exametrika.common.utils.ByteArray;


/**
 * The {@link LZ4} contains LZ4 compression/decompression methods.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LZ4
{
    /** Returns the maximum compressed length for an input of size <code>length</code>. 
    *
    * @param length decompressed length
    * @return maximum compressed legnth
    */
    public static final int maxCompressedLength(int length)
    {
        if (length < 0)
            throw new IllegalArgumentException("length must be >= 0, got " + length);

        return length + length / 255 + 16;
    }

    /**
     * Compresses <code>src[srcOff:srcOff+srcLen]</code> into <code>dest[destOff:destOff+destLen]</code> and return the
     * compressed length. This method will throw a {@link LZ4Exception} if this compressor is unable to compress the
     * input into less than <code>maxDestLen</code> bytes. To prevent this exception to be thrown, you should make sure
     * that <code>maxDestLen >= maxCompressedLength(srcLen)</code>.
     *
     * @param fast if true fast compression is used
     * @param src source buffer
     * @param srcOff source buffer offset
     * @param srcLen source buffer length
     * @param dest destination buffer
     * @param destOff destination buffer offset
     * @param maxDestLen maximum destination length
     * @return the compressed size
     * @exception LZ4Exception if maxDestLen is too small
     */
    public static int compress(boolean fast, byte[] src, final int srcOff, int srcLen, byte[] dest, final int destOff, int maxDestLen)
    {
        if (fast)
            return LZ4Compressor.INSTANCE.compress(src, srcOff, srcLen, dest, destOff, maxDestLen);
        else
            return LZ4HighCompressor.INSTANCE.compress(src, srcOff, srcLen, dest, destOff, maxDestLen);
    }
    
    /**
     * Uncompress <code>src[srcOff:]</code> into <code>dest[destOff:destOff+destLen]</code> and return the number of
     * bytes read from <code>src</code>. <code>destLen</code> must be exactly the size of the decompressed data.
     * 
     * @param src source buffer
     * @param srcOff source buffer offset
     * @param dest destination buffer
     * @param destOff destination buffer offset
     * @param destLen the <b>exact</b> size of the original input
     * @return the number of bytes read to restore the original input
     */
    public static int decompress(byte[] src, final int srcOff, byte[] dest, final int destOff, int destLen)
    {
        return LZ4Decompressor.INSTANCE.decompress(src, srcOff, dest, destOff, destLen);
    }
    
    /**
     * Compresses the specified buffer.
     *
     * @param fast if true fast compression is used
     * @param buffer buffer to compress
     * @return compressed buffer
     */
    public static ByteArray compress(boolean fast, ByteArray buffer)
    {
        int maxCompressedLength = maxCompressedLength(buffer.getLength());
        byte[] compressedBuffer = new byte[maxCompressedLength];
        
        int compressedLength = compress(fast, buffer.getBuffer(), buffer.getOffset(), buffer.getLength(), 
            compressedBuffer, 0, maxCompressedLength);
        
        return new ByteArray(compressedBuffer, 0, compressedLength);
    }
    
    /**
     * Decompresses the specified buffer.
     *
     * @param compressedBuffer compressed buffer
     * @param decompressedLength length of originl decompressed buffer
     * @return decompressed buffer
     */
    public static ByteArray decompress(ByteArray compressedBuffer, int decompressedLength)
    {
        byte[] decompressedBuffer = new byte[decompressedLength];
        decompress(compressedBuffer.getBuffer(), compressedBuffer.getOffset(), decompressedBuffer, 0, decompressedBuffer.length);
        
        return new ByteArray(decompressedBuffer);
    }
    
    /**
     * Performs fast hashing calculation based on XXHash32 algorithm.
     *
     * @param buf buffer
     * @param off offset
     * @param len length
     * @param seed seed
     * @return hash code
     */
    public static int hash(byte[] buf, int off, int len, int seed)
    {
        return XXHash32.hash(buf, off, len, seed);
    }
    
    private LZ4()
    {
    }
}
