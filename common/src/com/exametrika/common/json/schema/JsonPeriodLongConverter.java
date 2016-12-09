/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Pair;

/**
 * The {@link JsonPeriodLongConverter} is a JSON period long converter with base in milliseconds.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonPeriodLongConverter extends JsonSuffixLongConverter
{
    public JsonPeriodLongConverter()
    {
        super(buildSuffixes());
    }

    private static List<Pair<String, Long>> buildSuffixes()
    {
        List<Pair<String, Long>> suffixes = new ArrayList<Pair<String,Long>>();

        long multiplier = 1;
        suffixes.add(new Pair<String, Long>("ms", multiplier));
        
        multiplier *= 1000;
        suffixes.add(new Pair<String, Long>("s", multiplier));
        
        multiplier *= 60;
        suffixes.add(new Pair<String, Long>("m", multiplier));
        
        multiplier *= 60;
        suffixes.add(new Pair<String, Long>("h", multiplier));
        
        multiplier *= 24;
        suffixes.add(new Pair<String, Long>("d", multiplier));
        return suffixes;
    }
}
