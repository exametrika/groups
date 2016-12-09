/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import com.exametrika.common.utils.Assert;




/**
 * The {@link JsonDiff} computes difference between two Json elements.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonDiff
{
    private static final Object INCOMPATIBLE = new Object();
    private final boolean full;
    private final NumberFormat format;

    /**
     * Creates object.
     */
    public JsonDiff()
    {
        this(true, createDefaultNumberFormat());
    }

    /**
     * Creates object.
     *
     * @param full if false only difference in structure is computed
     */
    public JsonDiff(boolean full)
    {
        this(full, createDefaultNumberFormat());
    }
    
    /**
     * Creates object.
     *
     * @param full if false only difference in structure is computed
     * @param format number format to format doubles in full comparisons
     */
    public JsonDiff(boolean full, NumberFormat format)
    {
        Assert.notNull(format);
        
        this.full = full;
        this.format = format;
    }
    
    /**
     * Computes difference between two Json objects or arrays.
     *
     * @param value1 first value to compare
     * @param value2 second value to compare
     * @return comparison result or null if two values are equal
     */
    public JsonObject diff(IJsonCollection value1, IJsonCollection value2)
    {
        Assert.notNull(value1);
        Assert.notNull(value2);
        Assert.isTrue(value1.getClass().equals(value2.getClass()));
        
        if (value1 instanceof JsonObject)
            return diff((JsonObject)value1, (JsonObject)value2);
        else if (value1 instanceof JsonArray)
            return diff((JsonArray)value1, (JsonArray)value2);
        else
            return Assert.error();
    }
    
    private Object diff(Object value1, Object value2)
    {
        if (value1 == value2)
            return null;
        
        if (value1 == null || value2 == null)
            return INCOMPATIBLE;
        
        if (value1 instanceof Number && value2 instanceof Number)
            return diff((Number)value1, (Number)value2);
        
        if (!value1.getClass().equals(value2.getClass()))
            return INCOMPATIBLE;
        
        if (value1 instanceof JsonObject)
            return diff((JsonObject)value1, (JsonObject)value2);
        else if (value1 instanceof JsonArray)
            return diff((JsonArray)value1, (JsonArray)value2);
        else if (value1.equals(value2))
            return null;
        else
            return INCOMPATIBLE;
    }

    private JsonObject diff(JsonObject value1, JsonObject value2)
    {
        if (value1 == value2)
            return null;
        
        JsonObjectBuilder builder = new JsonObjectBuilder();
        
        for (Map.Entry<String, Object> entry : value1)
        {
            if (!value2.contains(entry.getKey()))
                builder.put("+" + entry.getKey(), full ? entry.getValue() : "");
            else
            {
                Object element2 = value2.get(entry.getKey(), null);

                Object diff = diff(entry.getValue(), element2);
                if (diff == INCOMPATIBLE)
                {
                    builder.put("+" + entry.getKey(), full ? entry.getValue() : "");
                    builder.put("-" + entry.getKey(), full ? element2 : "");
                }
                else if (diff != null)
                    builder.put("~" + entry.getKey(), diff);
            }
        }
        
        for (Map.Entry<String, Object> entry : value2)
        {
            if (!value1.contains(entry.getKey()))
                builder.put("-" + entry.getKey(), full ? entry.getValue() : "");
        }
        
        if (!builder.isEmpty())
            return builder.toJson();
        else
            return null;
    }
    
    private JsonObject diff(JsonArray value1, JsonArray value2)
    {
        if (value1 == value2)
            return null;
        
        JsonObjectBuilder builder = new JsonObjectBuilder();
        int i = 0;
        for ( ; i < value1.size(); i++)
        {
            if (i >= value2.size())
                builder.put("+[" + i + "]", full ? value1.get(i, null) : "");
            else
            {
                Object element1 = value1.get(i, null);
                Object element2 = value2.get(i, null);

                Object diff = diff(element1, element2);
                if (diff == INCOMPATIBLE)
                {
                    builder.put("+[" + i + "]", full ? element1 : "");
                    builder.put("-[" + i + "]", full ? element2 : "");
                }
                else if (diff != null)
                    builder.put("~[" + i + "]", diff);
            }
        }
        
        for ( ; i < value2.size(); i++)
            builder.put("-[" + i + "]", full ? value2.get(i, null) : "");
        
        if (!builder.isEmpty())
            return builder.toJson();
        else
            return null;
    }

    private Object diff(Number value1, Number value2)
    {
        if (value1.equals(value2))
            return null;
        else if (value1 instanceof Long && value2 instanceof Long)
        {
            long diff = (Long)value1 - (Long)value2;
            if (full)
                return diff + " (" + value1 + " - " + value2 + ")";
            else
                return diff;
        }
        else if (value1 instanceof Double && value2 instanceof Double)
        {
            double diff = (Double)value1 - (Double)value2;
            if (full)
                return format.format(diff) + " (" + format.format(value1) + " - " + format.format(value2) + ")";
            else
                return diff;
        }
        else if (value1 instanceof Long && value2 instanceof Double)
        {
            double diff = (Long)value1 - (Double)value2;
            if (full)
                return format.format(diff) + " (" + value1 + " - " + format.format(value2) + ")";
            else
                return diff;
        }
        else if (value1 instanceof Double && value2 instanceof Long)
        {
            double diff = (Double)value1 - (Long)value2;
            if (full)
                return format.format(diff) + " (" + format.format(value1) + " - " + value2 + ")";
            else
                return diff;
        }
        else
            return INCOMPATIBLE;
    }
    
    private static NumberFormat createDefaultNumberFormat()
    {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setGroupingUsed(false);
        format.setMaximumIntegerDigits(30);
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(0);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
