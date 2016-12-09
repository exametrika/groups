/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;



/**
 * The {@link Marker} is a {@link IMarker} implementation.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class Marker implements IMarker
{
    private final String name;
    private final List<IMarker> references;
    
    public Marker(String name, IMarker[] references)
    {
        Assert.notNull(name);
        
        this.name = name;
        
        if (references != null)
        {
            ArrayList<IMarker> list = new ArrayList<IMarker>(references.length);
            for (IMarker marker : references)
            {
                if (marker != null)
                    list.add(marker);
            }
            
            this.references = Immutables.wrap(list);
        }
        else
            this.references = Collections.emptyList();
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        
        if (!(o instanceof Marker))
            return false;
        
        Marker marker = (Marker)o;
        return name.equals(marker.getName());
    }
    
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        
        if (!references.isEmpty())
        {
            builder.append(" [");
            
            boolean first = true;
            for (IMarker marker : references)
            {
                if (first)
                    first = false;
                else
                    builder.append(',');

                builder.append(marker.getName());
            }
            builder.append("]");
        }
        
        return builder.toString();
    }
}
