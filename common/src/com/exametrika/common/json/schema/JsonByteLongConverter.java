/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Pair;







/**
 * The {@link JsonByteLongConverter} is a JSON bytes long converter.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonByteLongConverter extends JsonSuffixLongConverter
{
    public JsonByteLongConverter()
    {
        super(buildSuffixes());
    }

    private static List<Pair<String, Long>> buildSuffixes()
    {
        List<Pair<String, Long>> suffixes = new ArrayList<Pair<String,Long>>(); 
        suffixes.add(new Pair<String, Long>("b", 1l));        
        suffixes.add(new Pair<String, Long>("kb", 1l << 10));
        suffixes.add(new Pair<String, Long>("mb", 1l << 20));
        suffixes.add(new Pair<String, Long>("gb", 1l << 30));
        suffixes.add(new Pair<String, Long>("tb", 1l << 40));
        return suffixes;
    }
}
