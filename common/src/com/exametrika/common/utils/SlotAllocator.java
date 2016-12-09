/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.exametrika.common.json.JsonObject;




/**
 * The {@link SlotAllocator} is a meter container slot allocator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SlotAllocator
{
    private final List<Slot> slots = new ArrayList<Slot>();
    private final AtomicInteger refreshIndex = new AtomicInteger();
    
    public static class Slot
    {
        public final String name;
        public final String componentType;
        public final JsonObject metadata;
        public final int id;
        
        public Slot(int id, String name, String componentType, JsonObject metadata)
        {
            Assert.notNull(name);
            Assert.notNull(componentType);
            
            this.id = id;
            this.name = name;
            this.componentType = componentType;
            this.metadata = metadata;
        }
    }
    
    public int getRefreshIndex()
    {
        return refreshIndex.get();
    }
    
    public int getSlotCount()
    {
        return slots.size();
    }
    
    public Slot getSlot(int index)
    {
        return slots.get(index);
    }
    
    public synchronized Slot allocate(String name, String componentType, JsonObject metadata)
    {
        int index = slots.size();
        
        Slot slot = new Slot(index, name, componentType, metadata);
        slots.add(slot);
        refreshIndex.incrementAndGet();
        
        return slot;
    }
    
    public synchronized void free(int id)
    {
        slots.set(id, null);
        refreshIndex.incrementAndGet();
    }
}
