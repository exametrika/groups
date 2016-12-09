/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;




/**
 * The {@link JsonUtils} contains different utility methods for JSON.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonUtils
{
    public static final JsonObject EMPTY_OBJECT = new JsonObjectBuilder().toJson();
    public static final JsonArray EMPTY_ARRAY = new JsonArrayBuilder().toJson();
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int BOOLEAN_CACHE_SIZE = Memory.getShallowSize(Boolean.class);
    private static final int DOUBLE_CACHE_SIZE = Memory.getShallowSize(Double.class);
    private static final int LONG_CACHE_SIZE = Memory.getShallowSize(Long.class);
    
    public static String escape(String string)
    {
        if (string == null || string.length() == 0)
            return "";

        char c = 0;
        String hhhh;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); i++)
        {
            c = string.charAt(i);
            switch (c)
            {
            case '\\':
            case '"':
            case '\'':
            case '{':
            case '}':
            case '[':
            case ']':
            case ':':
            case '=':
            case ',':
            case ';':
                builder.append('\\');
                builder.append(c);
                break;
            case '\b':
                builder.append("\\b");
                break;
            case '\t':
                builder.append("\\t");
                break;
            case '\n':
                builder.append("\\n");
                break;
            case '\f':
                builder.append("\\f");
                break;
            case '\r':
                builder.append("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100'))
                {
                    hhhh = "000" + Integer.toHexString(c);
                    builder.append("\\u" + hhhh.substring(hhhh.length() - 4));
                }
                else
                    builder.append(c);
            }
        }
        return builder.toString();
    }
    
    /**
     * Checks and possibly converts value to value of one of supported Json types.
     *
     * @param value initial value. Can be null
     * @return converted value or null if initial value is null
     */
    public static Object checkValue(Object value)
    {
        if (value == null)
            return null;
        else if (value instanceof Double)
            return value;
        else if (value instanceof Long)
            return value;
        else if (value instanceof Float)
            return ((Float)value).doubleValue();
        else if (value instanceof Number)
            return ((Number)value).longValue();
        else if (value instanceof String || value instanceof Boolean || value instanceof JsonObjectBuilder || 
            value instanceof JsonObject || value instanceof JsonArrayBuilder || value instanceof JsonArray)
            return value;
        else
            return value.toString();
    }  
    
    public static <T> T get(JsonObject object, String name, Class<T> type, T defaultValue)
    {
        Object value = object.get(name, null);
        if (value != null && type.isAssignableFrom(value.getClass()))
            return (T)value;
        else
            return defaultValue;
    }
    
    public static Object toJson(Object value)
    {
        if (value == null)
            return null;
        else if (value instanceof Double)
            return value;
        else if (value instanceof Long)
            return value;
        else if (value instanceof Float)
            return ((Float)value).doubleValue();
        else if (value instanceof Number)
            return ((Number)value).longValue();
        else if (value instanceof String || value instanceof Boolean || value instanceof JsonObjectBuilder || 
            value instanceof JsonObject || value instanceof JsonArrayBuilder || value instanceof JsonArray || value == null)
            return value;
        else if (value instanceof Collection)
            return toJson((Collection)value);
        else if (value instanceof Map)
            return toJson((Map)value);
        else if (value instanceof Json)
            return ((Json)value).builder();
        else if (value.getClass().isArray())
        {
            JsonArrayBuilder builder = new JsonArrayBuilder();
            int count = Array.getLength(value);
            for (int i = 0; i < count; i++)
                builder.add(toJson(Array.get(value, i)));
            
            return builder.toJson();
        }
        else
            return value.toString();
    }
    
    public static <T> JsonArray toJson(T[] values)
    {
        if (values == null)
            return null;
        
        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (T value : values)
            builder.add(value);
        
        return builder.toJson();
    }
    
    public static JsonArray toJson(long[] values)
    {
        if (values == null)
            return null;
        
        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (long value : values)
            builder.add(value);
        
        return builder.toJson();
    }
    
    public static JsonArray toJson(double[] values)
    {
        if (values == null)
            return null;
        
        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (double value : values)
            builder.add(value);
        
        return builder.toJson();
    }
    
    public static JsonArray toJson(Collection values)
    {
        if (values == null)
            return null;
        
        if (values instanceof JsonArray)
            return (JsonArray)values;
        
        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (Object value : values)
            builder.add(value);
        
        return builder.toJson();
    }
    
    public static JsonObject toJson(Map<String, ?> values)
    {
        if (values == null)
            return null;
        
        if (values instanceof JsonObject)
            return (JsonObject)values;
        
        JsonObjectBuilder builder = new JsonObjectBuilder();
        for (Map.Entry<String, ?> entry : values.entrySet())
            builder.put(entry.getKey(), entry.getValue());
        
        return builder.toJson();
    }

    public static <T> List<T> toList(JsonArray array)
    {
        if (array == null)
            return null;
        
        List<T> list = new ArrayList<T>();
        for (Object element : array)
            list.add((T)element);
        
        return list;
    }
    
    public static <T> Set<T> toSet(JsonArray array)
    {
        if (array == null)
            return null;
        
        Set<T> set = new LinkedHashSet<T>();
        for (Object element : array)
            set.add((T)element);
        
        return set;
    }
    
    public static <T> Map<String, T> toMap(JsonObject object)
    {
        if (object == null)
            return null;
        
        Map<String, T> map = new LinkedHashMap<String, T>();
        for (Map.Entry<String, Object> entry : object)
            map.put(entry.getKey(), (T)entry.getValue());
        
        return map;
    }
    
    public static Object toBuilder(Object value)
    {
        if (value instanceof JsonObjectBuilder)
            return value;
        else if (value instanceof JsonObject)
            return new JsonObjectBuilder((JsonObject)value);
        else if (value instanceof JsonArrayBuilder)
            return value;
        else if (value instanceof JsonArray)
            return new JsonArrayBuilder((JsonArray)value);
        else
            return value;
    }
    
    public static Object toImmutable(Object value)
    {
        if (value instanceof JsonObjectBuilder)
            return ((JsonObjectBuilder)value).toJson();
        else if (value instanceof JsonArrayBuilder)
            return ((JsonArrayBuilder)value).toJson();
        else
            return value;
    }
        
    public static int getCacheSize(Object value)
    {
        if (value == null)
            return 0;
        else if (value instanceof Double)
            return DOUBLE_CACHE_SIZE;
        else if (value instanceof Long)
            return LONG_CACHE_SIZE;
        else if (value instanceof String)
            return CacheSizes.getStringCacheSize((String)value);
        else if (value instanceof Boolean)
            return BOOLEAN_CACHE_SIZE;
        else if (value instanceof JsonObject)
            return ((JsonObject)value).getCacheSize();
        else if (value instanceof JsonArray)
            return ((JsonArray)value).getCacheSize();
        else
            return Assert.error();
    }  
    
    public static IJsonCollection truncate(IJsonCollection collection, int length, boolean ellipsis)
    {
        return (IJsonCollection)truncate(collection, new int[]{length}, ellipsis);
    }
    
    public static JsonObjectBuilder mergeObjects(JsonObject element1, JsonObject element2, String path, boolean replace)
    {
        if (element1 == null)
        {
            if (element2 instanceof JsonObjectBuilder)
                return (JsonObjectBuilder)element2;
            else
                return new JsonObjectBuilder(element2); 
        }
        
        JsonObjectBuilder builder = new JsonObjectBuilder(element1);
        for (Map.Entry<String, Object> entry : element2)
        {
            String elementPath;
            if (!path.isEmpty())
                elementPath = path + '.' + entry.getKey();
            else
                elementPath = entry.getKey();
            
            Object value1 = builder.get(entry.getKey(), null);
            Object value2 = entry.getValue();
            if (value1 instanceof JsonObject && value2 instanceof JsonObject)
                value1 = mergeObjects((JsonObject)value1, (JsonObject)value2, elementPath, replace);
            else if (value1 instanceof JsonArray && value2 instanceof JsonArray)
            {
                JsonArrayBuilder array1 = new JsonArrayBuilder((JsonArray)value1);
                array1.addAll((JsonArray)value2);
                
                value1 = array1;
            }
            else if (value1 == null || replace || Objects.equals(value1, value2))
                value1 = value2;
            else
                throw new InvalidConfigurationException(messages.incompatibleObjects(elementPath));
            
            builder.put(entry.getKey(), value1);
        }
        
        return builder;
    }
    
    private static Object truncate(Object value, int[] length, boolean ellipsis)
    {
        if (value instanceof Long || value instanceof Double)
        {
            if (length[0] - 8 >= 0)
            {
                length[0] -= 8;
                return value;
            }
            else
            {
                length[0] = -1;
                return null;
            }
        }
        else if (value instanceof Boolean || value == null)
        {
            if (length[0] - 1 >= 0)
            {
                length[0] -= 1;
                return value;
            }
            else
            {
                length[0] = -1;
                return null;
            }
        }
        else if (value instanceof String)
        {
            int l = ellipsis ? 4 : 1;
            if (length[0] - l >= 0)
            {
                String str = Strings.truncate((String)value, length[0] - l + 1, ellipsis);
                length[0] -= str.length();
                return str;
            }
            else
            {
                length[0] = -1;
                return null;
            }
        }
        else if (value instanceof JsonObject)
        {
            if (length[0] <= 0)
                return null;
            
            JsonObjectBuilder builder = new JsonObjectBuilder();
            JsonObject object = (JsonObject)value;
            for (Map.Entry<String, Object> entry : object)
            {
                if (length[0] > entry.getKey().length())
                {
                    String entryKey = (String)truncate(entry.getKey(), length, ellipsis);
                    Object entryValue = truncate(entry.getValue(), length, ellipsis);
                    
                    builder.put(entryKey, entryValue);
                }
                else
                {
                    if (ellipsis)
                        builder.put("...", "...");
                    
                    break;
                }
            }
            
            return builder.toJson();
        }
        else if (value instanceof JsonArray)
        {
            if (length[0] <= 0)
                return null;
            
            JsonArrayBuilder builder = new JsonArrayBuilder();
            JsonArray array = (JsonArray)value;
            for (Object element : array)
            {
                element = truncate(element, length, ellipsis);
                if (length[0] >= 0)
                    builder.add(element);
                else
                {
                    if (ellipsis)
                        builder.add("...");
                    
                    break;
                }
            }
            
            return builder.toJson();
        }
        else
            return Assert.error();
    }
    
    private JsonUtils()
    {
    }
    
    private interface IMessages
    {
        @DefaultMessage("Could not create combined object. Combining objects have incompatible elements on path ''{0}''.")
        ILocalizedMessage incompatibleObjects(String path);
    }
}
