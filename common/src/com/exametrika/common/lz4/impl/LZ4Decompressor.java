/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.readShortLittleEndian;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.safeArraycopy;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.wildArraycopy;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.wildIncrementalCopy;
import static com.exametrika.common.lz4.impl.LZ4Utils.COPY_LENGTH;
import static com.exametrika.common.lz4.impl.LZ4Utils.MIN_MATCH;
import static com.exametrika.common.lz4.impl.LZ4Utils.ML_BITS;
import static com.exametrika.common.lz4.impl.LZ4Utils.ML_MASK;
import static com.exametrika.common.lz4.impl.LZ4Utils.RUN_MASK;
import static com.exametrika.common.lz4.impl.LZ4Utils.safeIncrementalCopy;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readByte;
import static com.exametrika.common.lz4.impl.Utils.checkRange;

/**
 * The {@link LZ4Decompressor} is a fast LZ4 decompressor.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LZ4Decompressor
{
    public static final LZ4Decompressor INSTANCE = new LZ4Decompressor();

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
    public int decompress(byte[] src, final int srcOff, byte[] dest, final int destOff, int destLen)
    {
        checkRange(src, srcOff);
        checkRange(dest, destOff, destLen);

        if (destLen == 0)
        {
            if (src[srcOff] != 0)
                throw new LZ4Exception("Malformed input at " + srcOff);

            return 1;
        }

        final int destEnd = destOff + destLen;

        int sOff = srcOff;
        int dOff = destOff;

        while (true)
        {
            final int token = readByte(src, sOff++) & 0xFF;

            // literals
            int literalLen = token >>> ML_BITS;
            if (literalLen == RUN_MASK)
            {
                byte len;
                while ((len = readByte(src, sOff++)) == (byte)0xFF)
                    literalLen += 0xFF;

                literalLen += len & 0xFF;
            }

            final int literalCopyEnd = dOff + literalLen;
            if (literalCopyEnd > destEnd - COPY_LENGTH)
            {
                if (literalCopyEnd != destEnd)
                    throw new LZ4Exception("Malformed input at " + sOff);
                else
                {
                    safeArraycopy(src, sOff, dest, dOff, literalLen);
                    sOff += literalLen;
                    break; // EOF
                }
            }

            wildArraycopy(src, sOff, dest, dOff, literalLen);
            sOff += literalLen;
            dOff = literalCopyEnd;

            // matchs
            final int matchDec = readShortLittleEndian(src, sOff);
            sOff += 2;
            int matchOff = dOff - matchDec;

            if (matchOff < destOff)
                throw new LZ4Exception("Malformed input at " + sOff);

            int matchLen = token & ML_MASK;
            if (matchLen == ML_MASK)
            {
                byte len;
                while ((len = readByte(src, sOff++)) == (byte)0xFF)
                {
                    matchLen += 0xFF;
                }
                matchLen += len & 0xFF;
            }
            matchLen += MIN_MATCH;

            final int matchCopyEnd = dOff + matchLen;

            if (matchCopyEnd > destEnd - COPY_LENGTH)
            {
                if (matchCopyEnd > destEnd)
                    throw new LZ4Exception("Malformed input at " + sOff);
                
                safeIncrementalCopy(dest, matchOff, dOff, matchLen);
            }
            else
                wildIncrementalCopy(dest, matchOff, dOff, matchCopyEnd);

            dOff = matchCopyEnd;
        }

        return sOff - srcOff;
    }

}