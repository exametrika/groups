/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.commonBytes;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.commonBytesBackward;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.hash;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.hash64k;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.readIntEquals;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.wildArraycopy;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.writeLen;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.writeShortLittleEndian;
import static com.exametrika.common.lz4.impl.LZ4Utils.HASH_TABLE_SIZE;
import static com.exametrika.common.lz4.impl.LZ4Utils.HASH_TABLE_SIZE_64K;
import static com.exametrika.common.lz4.impl.LZ4Utils.LAST_LITERALS;
import static com.exametrika.common.lz4.impl.LZ4Utils.LZ4_64K_LIMIT;
import static com.exametrika.common.lz4.impl.LZ4Utils.MAX_DISTANCE;
import static com.exametrika.common.lz4.impl.LZ4Utils.MF_LIMIT;
import static com.exametrika.common.lz4.impl.LZ4Utils.MIN_LENGTH;
import static com.exametrika.common.lz4.impl.LZ4Utils.MIN_MATCH;
import static com.exametrika.common.lz4.impl.LZ4Utils.ML_BITS;
import static com.exametrika.common.lz4.impl.LZ4Utils.ML_MASK;
import static com.exametrika.common.lz4.impl.LZ4Utils.RUN_MASK;
import static com.exametrika.common.lz4.impl.LZ4Utils.SKIP_STRENGTH;
import static com.exametrika.common.lz4.impl.LZ4Utils.lastLiterals;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readByte;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readInt;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readShort;
import static com.exametrika.common.lz4.impl.UnsafeUtils.writeByte;
import static com.exametrika.common.lz4.impl.UnsafeUtils.writeInt;
import static com.exametrika.common.lz4.impl.UnsafeUtils.writeShort;
import static com.exametrika.common.lz4.impl.Utils.checkRange;

import java.util.Arrays;

import com.exametrika.common.lz4.LZ4;

/**
 * The {@link LZ4Compressor} is a fast LZ4 compressor.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LZ4Compressor
{
    public static final LZ4Compressor INSTANCE = new LZ4Compressor();

    /** Returns the maximum compressed length for an input of size <code>length</code>. 
     *
     * @param length decompressed length
     * @return maximum compressed legnth
     */
    public final int maxCompressedLength(int length)
    {
        return LZ4.maxCompressedLength(length);
    }
    
    /**
     * Compresses <code>src[srcOff:srcOff+srcLen]</code> into <code>dest[destOff:destOff+destLen]</code> and return the
     * compressed length. This method will throw a {@link LZ4Exception} if this compressor is unable to compress the
     * input into less than <code>maxDestLen</code> bytes. To prevent this exception to be thrown, you should make sure
     * that <code>maxDestLen >= maxCompressedLength(srcLen)</code>.
     *
     * @param src source buffer
     * @param srcOff source buffer offset
     * @param srcLen source buffer length
     * @param dest destination buffer
     * @param destOff destination buffer offset
     * @param maxDestLen maximum destination length
     * @return the compressed size
     * @exception LZ4Exception if maxDestLen is too small
     */
    public int compress(byte[] src, final int srcOff, int srcLen, byte[] dest, final int destOff, int maxDestLen)
    {
        checkRange(src, srcOff, srcLen);
        checkRange(dest, destOff, maxDestLen);
        final int destEnd = destOff + maxDestLen;

        if (srcLen < LZ4_64K_LIMIT)
            return compress64k(src, srcOff, srcLen, dest, destOff, destEnd);

        final int srcEnd = srcOff + srcLen;
        final int srcLimit = srcEnd - LAST_LITERALS;
        final int mflimit = srcEnd - MF_LIMIT;

        int sOff = srcOff, dOff = destOff;
        int anchor = sOff++;

        final int[] hashTable = new int[HASH_TABLE_SIZE];
        Arrays.fill(hashTable, anchor);

        main: while (true)
        {
            // find a match
            int forwardOff = sOff;

            int ref;
            int findMatchAttempts = (1 << SKIP_STRENGTH) + 3;
            int back;
            do
            {
                sOff = forwardOff;
                forwardOff += findMatchAttempts++ >>> SKIP_STRENGTH;

                if (forwardOff > mflimit)
                    break main;

                final int h = hash(src, sOff);
                ref = readInt(hashTable, h);
                back = sOff - ref;
                writeInt(hashTable, h, sOff);
            }
            while (back >= MAX_DISTANCE || !readIntEquals(src, ref, sOff));

            final int excess = commonBytesBackward(src, ref, sOff, srcOff, anchor);
            sOff -= excess;
            ref -= excess;

            // sequence == refsequence
            final int runLen = sOff - anchor;

            // encode literal length
            int tokenOff = dOff++;

            if (dOff + runLen + (2 + 1 + LAST_LITERALS) + (runLen >>> 8) > destEnd)
                throw new LZ4Exception("maxDestLen is too small");

            if (runLen >= RUN_MASK)
            {
                writeByte(dest, tokenOff, RUN_MASK << ML_BITS);
                dOff = writeLen(runLen - RUN_MASK, dest, dOff);
            }
            else
                writeByte(dest, tokenOff, runLen << ML_BITS);

            // copy literals
            wildArraycopy(src, anchor, dest, dOff, runLen);
            dOff += runLen;

            while (true)
            {
                // encode offset
                writeShortLittleEndian(dest, dOff, back);
                dOff += 2;

                // count nb matches
                sOff += MIN_MATCH;
                final int matchLen = commonBytes(src, ref + MIN_MATCH, sOff, srcLimit);
                if (dOff + (1 + LAST_LITERALS) + (matchLen >>> 8) > destEnd)
                    throw new LZ4Exception("maxDestLen is too small");
                
                sOff += matchLen;

                // encode match len
                if (matchLen >= ML_MASK)
                {
                    writeByte(dest, tokenOff, readByte(dest, tokenOff) | ML_MASK);
                    dOff = writeLen(matchLen - ML_MASK, dest, dOff);
                }
                else
                    writeByte(dest, tokenOff, readByte(dest, tokenOff) | matchLen);

                // test end of chunk
                if (sOff > mflimit)
                {
                    anchor = sOff;
                    break main;
                }

                // fill table
                writeInt(hashTable, hash(src, sOff - 2), sOff - 2);

                // test next position
                final int h = hash(src, sOff);
                ref = readInt(hashTable, h);
                writeInt(hashTable, h, sOff);
                back = sOff - ref;

                if (back >= MAX_DISTANCE || !readIntEquals(src, ref, sOff))
                    break;

                tokenOff = dOff++;
                writeByte(dest, tokenOff, 0);
            }

            // prepare next loop
            anchor = sOff++;
        }

        dOff = lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
        return dOff - destOff;
    }
    
    private int compress64k(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int destEnd)
    {
        final int srcEnd = srcOff + srcLen;
        final int srcLimit = srcEnd - LAST_LITERALS;
        final int mflimit = srcEnd - MF_LIMIT;

        int sOff = srcOff, dOff = destOff;

        int anchor = sOff;

        if (srcLen >= MIN_LENGTH)
        {
            final short[] hashTable = new short[HASH_TABLE_SIZE_64K];

            ++sOff;

            main: while (true)
            {
                // find a match
                int forwardOff = sOff;

                int ref;
                int findMatchAttempts = (1 << SKIP_STRENGTH) + 3;
                do
                {
                    sOff = forwardOff;
                    forwardOff += findMatchAttempts++ >>> SKIP_STRENGTH;

                    if (forwardOff > mflimit)
                        break main;

                    final int h = hash64k(src, sOff);
                    ref = srcOff + readShort(hashTable, h);
                    writeShort(hashTable, h, sOff - srcOff);
                }
                while (!readIntEquals(src, ref, sOff));

                // catch up
                final int excess = commonBytesBackward(src, ref, sOff, srcOff, anchor);
                sOff -= excess;
                ref -= excess;

                // sequence == refsequence
                final int runLen = sOff - anchor;

                // encode literal length
                int tokenOff = dOff++;

                if (dOff + runLen + (2 + 1 + LAST_LITERALS) + (runLen >>> 8) > destEnd)
                    throw new LZ4Exception("maxDestLen is too small");

                if (runLen >= RUN_MASK)
                {
                    writeByte(dest, tokenOff, RUN_MASK << ML_BITS);
                    dOff = writeLen(runLen - RUN_MASK, dest, dOff);
                }
                else
                    writeByte(dest, tokenOff, runLen << ML_BITS);

                // copy literals
                wildArraycopy(src, anchor, dest, dOff, runLen);
                dOff += runLen;

                while (true)
                {
                    // encode offset
                    writeShortLittleEndian(dest, dOff, (short)(sOff - ref));
                    dOff += 2;

                    // count nb matches
                    sOff += MIN_MATCH;
                    ref += MIN_MATCH;
                    final int matchLen = commonBytes(src, ref, sOff, srcLimit);
                    if (dOff + (1 + LAST_LITERALS) + (matchLen >>> 8) > destEnd)
                        throw new LZ4Exception("maxDestLen is too small");

                    sOff += matchLen;

                    // encode match len
                    if (matchLen >= ML_MASK)
                    {
                        writeByte(dest, tokenOff, readByte(dest, tokenOff) | ML_MASK);
                        dOff = writeLen(matchLen - ML_MASK, dest, dOff);
                    }
                    else
                        writeByte(dest, tokenOff, readByte(dest, tokenOff) | matchLen);

                    // test end of chunk
                    if (sOff > mflimit)
                    {
                        anchor = sOff;
                        break main;
                    }

                    // fill table
                    writeShort(hashTable, hash64k(src, sOff - 2), sOff - 2 - srcOff);

                    // test next position
                    final int h = hash64k(src, sOff);
                    ref = srcOff + readShort(hashTable, h);
                    writeShort(hashTable, h, sOff - srcOff);

                    if (!readIntEquals(src, sOff, ref))
                        break;

                    tokenOff = dOff++;
                    dest[tokenOff] = 0;
                }

                // prepare next loop
                anchor = sOff++;
            }
        }

        dOff = lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
        return dOff - destOff;
    }
}