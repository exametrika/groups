/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.commonBytes;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.commonBytesBackward;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.encodeSequence;
import static com.exametrika.common.lz4.impl.LZ4UnsafeUtils.readIntEquals;
import static com.exametrika.common.lz4.impl.LZ4Utils.HASH_TABLE_SIZE_HC;
import static com.exametrika.common.lz4.impl.LZ4Utils.LAST_LITERALS;
import static com.exametrika.common.lz4.impl.LZ4Utils.MAX_DISTANCE;
import static com.exametrika.common.lz4.impl.LZ4Utils.MF_LIMIT;
import static com.exametrika.common.lz4.impl.LZ4Utils.MIN_MATCH;
import static com.exametrika.common.lz4.impl.LZ4Utils.ML_MASK;
import static com.exametrika.common.lz4.impl.LZ4Utils.OPTIMAL_ML;
import static com.exametrika.common.lz4.impl.LZ4Utils.copyTo;
import static com.exametrika.common.lz4.impl.LZ4Utils.hashHC;
import static com.exametrika.common.lz4.impl.LZ4Utils.lastLiterals;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readInt;

import java.util.Arrays;

import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.lz4.impl.LZ4Utils.Match;

/**
 * The {@link LZ4HighCompressor} is a high compression LZ4 compressor.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LZ4HighCompressor
{
    public static final LZ4HighCompressor INSTANCE = new LZ4HighCompressor();

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
    public int compress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int maxDestLen)
    {
        final int srcEnd = srcOff + srcLen;
        final int destEnd = destOff + maxDestLen;
        final int mfLimit = srcEnd - MF_LIMIT;
        final int matchLimit = srcEnd - LAST_LITERALS;

        int sOff = srcOff;
        int dOff = destOff;
        int anchor = sOff++;

        final HashTable ht = new HashTable(srcOff);
        final Match match0 = new Match();
        final Match match1 = new Match();
        final Match match2 = new Match();
        final Match match3 = new Match();

        main: while (sOff < mfLimit)
        {
            if (!ht.insertAndFindBestMatch(src, sOff, matchLimit, match1))
            {
                ++sOff;
                continue;
            }

            // saved, in case we would skip too much
            copyTo(match1, match0);

            search2: while (true)
            {
                assert match1.start >= anchor;
                if (match1.end() >= mfLimit || !ht.insertAndFindWiderMatch(src, match1.end() - 2, match1.start + 1,
                    matchLimit, match1.len, match2))
                {
                    // no better match
                    dOff = encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff, destEnd);
                    anchor = sOff = match1.end();
                    continue main;
                }

                if (match0.start < match1.start)
                {
                    if (match2.start < match1.start + match0.len)
                    { // empirical
                        copyTo(match0, match1);
                    }
                }
                assert match2.start > match1.start;

                if (match2.start - match1.start < 3)
                { // First Match too small : removed
                    copyTo(match2, match1);
                    continue search2;
                }

                search3: while (true)
                {
                    if (match2.start - match1.start < OPTIMAL_ML)
                    {
                        int newMatchLen = match1.len;
                        if (newMatchLen > OPTIMAL_ML)
                        {
                            newMatchLen = OPTIMAL_ML;
                        }
                        if (match1.start + newMatchLen > match2.end() - MIN_MATCH)
                        {
                            newMatchLen = match2.start - match1.start + match2.len - MIN_MATCH;
                        }
                        final int correction = newMatchLen - (match2.start - match1.start);
                        if (correction > 0)
                        {
                            match2.fix(correction);
                        }
                    }

                    if (match2.start + match2.len >= mfLimit || !ht.insertAndFindWiderMatch(src, match2.end() - 3,
                        match2.start, matchLimit, match2.len, match3))
                    {
                        // no better match -> 2 sequences to encode
                        if (match2.start < match1.end())
                        {
                            match1.len = match2.start - match1.start;
                        }
                        // encode seq 1
                        dOff = encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff, destEnd);
                        anchor = sOff = match1.end();
                        // encode seq 2
                        dOff = encodeSequence(src, anchor, match2.start, match2.ref, match2.len, dest, dOff, destEnd);
                        anchor = sOff = match2.end();
                        continue main;
                    }

                    if (match3.start < match1.end() + 3)
                    { // Not enough space for match 2 : remove it
                        if (match3.start >= match1.end())
                        { // // can write Seq1 immediately ==> Seq2 is removed, so Seq3 becomes Seq1
                            if (match2.start < match1.end())
                            {
                                final int correction = match1.end() - match2.start;
                                match2.fix(correction);
                                if (match2.len < MIN_MATCH)
                                {
                                    copyTo(match3, match2);
                                }
                            }

                            dOff = encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff,
                                destEnd);
                            anchor = sOff = match1.end();

                            copyTo(match3, match1);
                            copyTo(match2, match0);

                            continue search2;
                        }

                        copyTo(match3, match2);
                        continue search3;
                    }

                    // OK, now we have 3 ascending matches; let's write at least the first one
                    if (match2.start < match1.end())
                    {
                        if (match2.start - match1.start < ML_MASK)
                        {
                            if (match1.len > OPTIMAL_ML)
                            {
                                match1.len = OPTIMAL_ML;
                            }
                            if (match1.end() > match2.end() - MIN_MATCH)
                            {
                                match1.len = match2.end() - match1.start - MIN_MATCH;
                            }
                            final int correction = match1.end() - match2.start;
                            match2.fix(correction);
                        }
                        else
                        {
                            match1.len = match2.start - match1.start;
                        }
                    }

                    dOff = encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff, destEnd);
                    anchor = sOff = match1.end();

                    copyTo(match2, match1);
                    copyTo(match3, match2);

                    continue search3;
                }

            }

        }

        dOff = lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
        return dOff - destOff;
    }

    private class HashTable
    {
        static final int MAX_ATTEMPTS = 256;
        static final int MASK = MAX_DISTANCE - 1;
        int nextToUpdate;
        private final int base;
        private final int[] hashTable;
        private final short[] chainTable;

        HashTable(int base)
        {
            this.base = base;
            nextToUpdate = base;
            hashTable = new int[HASH_TABLE_SIZE_HC];
            Arrays.fill(hashTable, -1);
            chainTable = new short[MAX_DISTANCE];
        }

        private int hashPointer(byte[] bytes, int off)
        {
            final int v = readInt(bytes, off);
            final int h = hashHC(v);
            return base + hashTable[h];
        }

        private int next(int off)
        {
            return base + off - (chainTable[off & MASK] & 0xFFFF);
        }

        private void addHash(byte[] bytes, int off)
        {
            final int v = readInt(bytes, off);
            final int h = hashHC(v);
            int delta = off - hashTable[h];
            if (delta >= MAX_DISTANCE)
            {
                delta = MAX_DISTANCE - 1;
            }
            chainTable[off & MASK] = (short)delta;
            hashTable[h] = off - base;
        }

        void insert(int off, byte[] bytes)
        {
            for (; nextToUpdate < off; ++nextToUpdate)
            {
                addHash(bytes, nextToUpdate);
            }
        }

        boolean insertAndFindBestMatch(byte[] buf, int off, int matchLimit, Match match)
        {
            match.start = off;
            match.len = 0;

            insert(off, buf);

            int ref = hashPointer(buf, off);

            if (ref >= off - 4 && ref >= base)
            { // potential repetition
                if (readIntEquals(buf, ref, off))
                { // confirmed
                    final int delta = off - ref;
                    int ptr = off;
                    match.len = MIN_MATCH + commonBytes(buf, ref + MIN_MATCH, off + MIN_MATCH, matchLimit);
                    final int end = off + match.len - (MIN_MATCH - 1);
                    while (ptr < end - delta)
                    {
                        chainTable[ptr & MASK] = (short)delta; // pre load
                        ++ptr;
                    }
                    do
                    {
                        chainTable[ptr & MASK] = (short)delta;
                        hashTable[hashHC(readInt(buf, ptr))] = ptr - base; // head of table
                        ++ptr;
                    }
                    while (ptr < end);
                    nextToUpdate = end;
                    match.ref = ref;
                }
                ref = next(ref);
            }

            for (int i = 0; i < MAX_ATTEMPTS; ++i)
            {
                if (ref < Math.max(base, off - MAX_DISTANCE + 1))
                {
                    break;
                }
                if (buf[ref + match.len] == buf[off + match.len] && readIntEquals(buf, ref, off))
                {
                    final int matchLen = MIN_MATCH + commonBytes(buf, ref + MIN_MATCH, off + MIN_MATCH, matchLimit);
                    if (matchLen > match.len)
                    {
                        match.ref = ref;
                        match.len = matchLen;
                    }
                }
                ref = next(ref);
            }

            return match.len != 0;
        }

        boolean insertAndFindWiderMatch(byte[] buf, int off, int startLimit, int matchLimit, int minLen, Match match)
        {
            match.len = minLen;

            insert(off, buf);

            final int delta = off - startLimit;
            int ref = hashPointer(buf, off);
            for (int i = 0; i < MAX_ATTEMPTS; ++i)
            {
                if (ref < Math.max(base, off - MAX_DISTANCE + 1))
                {
                    break;
                }
                if (buf[ref - delta + match.len] == buf[startLimit + match.len] && readIntEquals(buf, ref, off))
                {
                    final int matchLenForward = MIN_MATCH + commonBytes(buf, ref + MIN_MATCH, off + MIN_MATCH,
                        matchLimit);
                    final int matchLenBackward = commonBytesBackward(buf, ref, off, base, startLimit);
                    final int matchLen = matchLenBackward + matchLenForward;
                    if (matchLen > match.len)
                    {
                        match.len = matchLen;
                        match.ref = ref - matchLenBackward;
                        match.start = off - matchLenBackward;
                    }
                }
                ref = next(ref);
            }

            return match.len > minLen;
        }

    }
}
