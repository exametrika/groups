/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.HashMap;
import java.util.Map;






/**
 * The {@link ParseContext} is a parse context.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ParseContext
{
    private int nextVariableSlot;
    private final Map<String, Integer> variableIndexesMap = new HashMap<String, Integer>();

    public int getVariableCount()
    {
        return variableIndexesMap.size();
    }
    
    public Integer findVariable(String name)
    {
        return variableIndexesMap.get(name);
    }
    
    public int allocateVariable(String name)
    {
        Integer slotIndex = variableIndexesMap.get(name);
        if (slotIndex == null)
        {
            slotIndex = nextVariableSlot++;
            variableIndexesMap.put(name, slotIndex);
        }
        
        return slotIndex;
    }
}
