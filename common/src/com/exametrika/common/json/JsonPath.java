/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;




/**
 * The {@link JsonPath} represents path accessor to {@link JsonArray} or {@link JsonObject}. Path
 * consists of segments. Each segment is applied to base value, producing a new base, to which next segment is applied (if any).
 * Each segment can be one of:
 * <li> .<property-name> - property segment. If this is a first segment period can be omitted.
 * Property name must have first symbol as {@link Character#isJavaIdentifierStart(char)}, 
 * other symbols as {@link Character#isJavaIdentifierPart(char)}. Base value must be {@link JsonObject}. Segment returns map value
 * for given property name as a map key.
 * <li> ["property-name"] - indexed property segment. Property name can contain any symbols. If it contains double quote symbols their must be doubled.
 * Quotes can be omitted if first symbol of property-name is not a digit and property name does not contain square brackets.
 * Base value must be {@link JsonObject}. Segment returns map value for given property name as a map key.
 * <li> [numeric-index] - indexed segment. Base value must be {@link JsonArray}. Segment returns list value for given list index.
 * 
 * Segment can be optional or required. If segment is optional and segment value is null, result of entire {@link JsonPath} expression will be null,
 * if segment is required and segment value is null, exception will be thrown. By default segment is required. 
 * To mark segment as optional use ? symbol after segment definition.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonPath
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String path;
    private boolean required = true;
    private final List<Segment> segments;

    public static class Segment
    {
        public final boolean required;
        
        public Segment(boolean required)
        {
            this.required = required;
        }
    }
    
    public static class PropertySegment extends Segment
    {
        public final String name;
        
        public PropertySegment(String name, boolean required)
        {
            super(required);
            
            Assert.notNull(name);
            
            this.name = name;
        }
    }
    
    public static class IndexSegment extends Segment
    {
        public final int index;
        
        public IndexSegment(int index, boolean required)
        {
            super(required);
            
            this.index = index;
        }
    }
    

    public JsonPath(String path)
    {
        Assert.notNull(path);
        
        this.path = path;
        segments = Immutables.wrap(parse(path));
    }
    
    public String getPath()
    {
        return path;
    }
    
    public List<Segment> getSegments()
    {
        return segments;
    }
    
    public <T> T get(Object value)
    {
        if (value == null)
        {
            if (required)
                Assert.notNull(value);
            else
                return null;
        }
        
        if (segments.isEmpty())
            return (T)value;
        
        Object base = value;
        for (Segment segment : segments)
        {
            if (segment instanceof PropertySegment)
            {
                PropertySegment propertySegment = (PropertySegment)segment;
                
                if (base instanceof JsonObject)
                {
                    JsonObject object = (JsonObject)base;
                    base = object.get(propertySegment.name, null);
                }
                else
                    throw new InvalidArgumentException(messages.invalidBaseForProperty(propertySegment.name));
                
                if (base == null)
                {
                    if (!segment.required)
                        return null;
                    else
                        Assert.notNull(null, "Required value is not found on path ''{0}''", path);
                }
            }
            else if (segment instanceof IndexSegment)
            {
                IndexSegment indexSegment = (IndexSegment)segment;
                
                if (base instanceof JsonArray)
                {
                    JsonArray array = (JsonArray)base;
                    if (indexSegment.index < array.size())
                        base = array.get(indexSegment.index, null);
                    else
                        base = null;
                }
                else
                    throw new InvalidArgumentException(messages.invalidBaseForIndex(indexSegment.index));
                
                if (base == null)
                {
                    if (!segment.required)
                        return null;
                    else
                        Assert.notNull(null, "Required value is not found on path ''{0}''", path);
                }
            }
            else
                Assert.error();
        }
        
        return (T)base;
    }
    
    public <T> T get(Object value, T defaultValue)
    {
        if (value == null)
            return defaultValue;
        
        if (segments.isEmpty())
            return (T)value;
        
        Object base = value;
        for (Segment segment : segments)
        {
            if (segment instanceof PropertySegment)
            {
                PropertySegment propertySegment = (PropertySegment)segment;
                
                if (base instanceof JsonObject)
                {
                    JsonObject object = (JsonObject)base;
                    base = object.get(propertySegment.name, null);
                }
                else
                    throw new InvalidArgumentException(messages.invalidBaseForProperty(propertySegment.name));
                
                if (base == null)
                    return defaultValue;
            }
            else if (segment instanceof IndexSegment)
            {
                IndexSegment indexSegment = (IndexSegment)segment;
                
                if (base instanceof JsonArray)
                {
                    JsonArray array = (JsonArray)base;
                    if (indexSegment.index < array.size())
                        base = array.get(indexSegment.index, null);
                    else
                        base = null;
                }
                else
                    throw new InvalidArgumentException(messages.invalidBaseForIndex(indexSegment.index));
                
                if (base == null)
                    return defaultValue;
            }
            else
                Assert.error();
        }
        
        return (T)base;
    }
    
    public void set(Object valueBuilder, Object value)
    {
        Assert.notNull(valueBuilder);
        
        Object base = valueBuilder;
        for (int i = 0; i < segments.size(); i++)
        {
            Segment segment = segments.get(i);
            if (segment instanceof PropertySegment)
            {
                PropertySegment propertySegment = (PropertySegment)segment;
                
                if (base instanceof JsonObjectBuilder)
                {
                    JsonObjectBuilder objectBuilder = (JsonObjectBuilder)base;
                    if (i == segments.size() - 1)
                        objectBuilder.put(propertySegment.name, value);
                    else
                    {
                        Object baseValue = objectBuilder.get(propertySegment.name, null);
                        if (baseValue instanceof JsonObjectBuilder || baseValue instanceof JsonArrayBuilder)
                            base = baseValue;
                        else if (baseValue instanceof JsonObject)
                        {
                            JsonObjectBuilder builder = new JsonObjectBuilder((JsonObject)baseValue);
                            objectBuilder.put(propertySegment.name, builder);
                            base = builder;
                        }
                        else if (baseValue instanceof JsonArray)
                        {
                            JsonArrayBuilder builder = new JsonArrayBuilder((JsonArray)baseValue);
                            objectBuilder.put(propertySegment.name, builder);
                            base = builder;
                        }
                        else 
                        {
                            Segment nextSegment = segments.get(i + 1);
                            if (nextSegment instanceof PropertySegment)
                            {
                                if (baseValue == null)
                                {
                                    JsonObjectBuilder builder = new JsonObjectBuilder();
                                    objectBuilder.put(propertySegment.name, builder);
                                    base = builder;
                                }
                                else
                                    throw new InvalidArgumentException(messages.invalidBaseForProperty(((PropertySegment)nextSegment).name));
                            }
                            else if (nextSegment instanceof IndexSegment)
                            {
                                if (baseValue == null)
                                {
                                    JsonArrayBuilder builder = new JsonArrayBuilder();
                                    objectBuilder.put(propertySegment.name, builder);
                                    base = builder;
                                }
                                else
                                    throw new InvalidArgumentException(messages.invalidBaseForIndex(((IndexSegment)nextSegment).index));
                            }
                            else
                                Assert.error();
                        }
                    }
                }
                else
                    throw new InvalidArgumentException(messages.invalidBaseForProperty(propertySegment.name));
            }
            else if (segment instanceof IndexSegment)
            {
                IndexSegment indexSegment = (IndexSegment)segment;
                
                if (base instanceof JsonArrayBuilder)
                {
                    JsonArrayBuilder arrayBuilder = (JsonArrayBuilder)base;
                    if (i == segments.size() - 1)
                        arrayBuilder.set(indexSegment.index, value);
                    else
                    {
                        Object baseValue;
                        if (indexSegment.index < arrayBuilder.size())
                            baseValue = arrayBuilder.get(indexSegment.index, null);
                        else
                            baseValue = null;
                        
                        if (baseValue instanceof JsonObjectBuilder || baseValue instanceof JsonArrayBuilder)
                            base = baseValue;
                        else if (baseValue instanceof JsonObject)
                        {
                            JsonObjectBuilder builder = new JsonObjectBuilder((JsonObject)baseValue);
                            arrayBuilder.set(indexSegment.index, builder);
                            base = builder;
                        }
                        else if (baseValue instanceof JsonArray)
                        {
                            JsonArrayBuilder builder = new JsonArrayBuilder((JsonArray)baseValue);
                            arrayBuilder.set(indexSegment.index, builder);
                            base = builder;
                        }
                        else
                        {
                            Segment nextSegment = segments.get(i + 1);
                            if (nextSegment instanceof PropertySegment)
                            {
                                if (baseValue == null)
                                {
                                    JsonObjectBuilder builder = new JsonObjectBuilder();
                                    arrayBuilder.set(indexSegment.index, builder);
                                    base = builder;
                                }
                                else
                                    throw new InvalidArgumentException(messages.invalidBaseForProperty(((PropertySegment)nextSegment).name));
                            }
                            else if (nextSegment instanceof IndexSegment)
                            {
                                if (baseValue == null)
                                {
                                    JsonArrayBuilder builder = new JsonArrayBuilder();
                                    arrayBuilder.set(indexSegment.index, builder);
                                    base = builder;
                                }
                                else
                                    throw new InvalidArgumentException(messages.invalidBaseForIndex(((IndexSegment)nextSegment).index));
                            }
                            else
                                Assert.error();
                        }
                    }
                }
                else
                    throw new InvalidArgumentException(messages.invalidBaseForIndex(indexSegment.index));
            }
            else
                Assert.error();
        }
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof JsonPath))
            return false;
        
        JsonPath valuePath = (JsonPath)o;
        return path.equals(valuePath.path);
    }
    
    @Override
    public int hashCode()
    {
        return path.hashCode();
    }
    
    @Override
    public String toString()
    {
        return path;
    }
    
    private List<Segment> parse(String path)
    {
        List<Segment> segments = new ArrayList<Segment>();
        
        for (int i = 0; i < path.length();)
        {
            char c = path.charAt(i);
            if (c == '.')
                i = parsePropertySegment(path, i + 1, segments);
            else if (c == '[')
                i = parseBracketSegment(path, i + 1, segments);
            else if (c == '?')
            {
                if (segments.isEmpty())
                    required = false;
                else
                {
                    Segment segment = segments.get(segments.size() - 1);
                    if (segment instanceof PropertySegment)
                    {
                        PropertySegment propertySegment = (PropertySegment)segment;
                        segments.set(segments.size() - 1, new PropertySegment(propertySegment.name, false));
                    }
                    else if (segment instanceof IndexSegment)
                    {
                        IndexSegment indexSegment = (IndexSegment)segment;
                        segments.set(segments.size() - 1, new IndexSegment(indexSegment.index, false));
                    }
                    else
                        Assert.error();
                }
                
                i++;
            }
            else if (Character.isJavaIdentifierStart(c))
                i = parsePropertySegment(path, i, segments);
            else
                throw new InvalidArgumentException(messages.syntaxError(path));
        }
        
        return segments;
    }
    
    private int parsePropertySegment(String path, int index, List<Segment> segments)
    {
        if (index >= path.length())
            throw new InvalidArgumentException(messages.syntaxError(path));
        
        int end = path.length();
        for (int i = index; i < path.length(); i++)
        {
            char c = path.charAt(i);
            if (i == index)
            {
                if (!Character.isJavaIdentifierStart(c))
                    throw new InvalidArgumentException(messages.notValidPropertyStart(c, path));
            }
            else if (!Character.isJavaIdentifierPart(c))
            {
                end = i;
                break;
            }
        }
        
        segments.add(new PropertySegment(path.substring(index, end), true));
        return end;
    }
    
    private int parseBracketSegment(String path, int index, List<Segment> segments)
    {
        if (index >= path.length())
            throw new InvalidArgumentException(messages.syntaxError(path));
        
        if (path.charAt(index) == '"')
            return parseQuotedIndexedPropertySegment(path, index + 1, segments);
        else if (!Character.isDigit(path.charAt(index)))
            return parseIndexedPropertySegment(path, index, segments);
        else
            return parseIndexSegment(path, index, segments);
    }

    private int parseIndexSegment(String path, int index, List<Segment> segments)
    {
        int end = path.indexOf(']', index);
        if (end == -1 || end <= index)
            throw new InvalidArgumentException(messages.syntaxError(path));
        segments.add(new IndexSegment(Integer.parseInt(path.substring(index, end)), true));
        return end + 1;
    }
    
    private int parseIndexedPropertySegment(String path, int index, List<Segment> segments)
    {
        int end = path.indexOf(']', index);
        if (end == -1)
            throw new InvalidArgumentException(messages.syntaxError(path));
        segments.add(new PropertySegment(path.substring(index, end), true));
        return end + 1;
    }
    
    private int parseQuotedIndexedPropertySegment(String path, int index, List<Segment> segments)
    {
        int end = -1;
        for (int i = index + 1; i < path.length();)
        {
            if (path.charAt(i) == '"')
            {
                if (i >= path.length() - 1)
                    throw new InvalidArgumentException(messages.syntaxError(path));
                if (path.charAt(i + 1) == '"')
                {
                    i += 2;
                    continue;
                }
                
                if (path.charAt(i + 1) != ']')
                    throw new InvalidArgumentException(messages.syntaxError(path));
                end = i;
                break;
            }
            i++;
        }
        if (end == -1)
            throw new InvalidArgumentException(messages.syntaxError(path));
        segments.add(new PropertySegment(path.substring(index, end), true));
        return end + 2;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Syntax error. Character ''{0}'' is not a valid first symbol of property name. Path: {1}")
        ILocalizedMessage notValidPropertyStart(char c, String path);
        @DefaultMessage("Invalid base value is set while accessing list index ''{0}''.")
        ILocalizedMessage invalidBaseForIndex(int index);
        @DefaultMessage("Invalid base value is set while accessing property value ''{0}''.")
        ILocalizedMessage invalidBaseForProperty(String name);
        @DefaultMessage("Syntax error in ''{0}''.")
        ILocalizedMessage syntaxError(String path);
    }
}
