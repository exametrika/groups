/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.config.resource.ClassPathResourceLoader;
import com.exametrika.common.config.resource.FileResourceLoader;
import com.exametrika.common.config.resource.IResourceLoader;
import com.exametrika.common.config.resource.ResourceManager;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link JsonSerializers} is serializer utility class for values.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonSerializers
{
    private static final byte NULL = 0x0;
    private static final byte LONG = 0x1;
    private static final byte BOOLEAN = 0x2;
    private static final byte DOUBLE = 0x3;
    private static final byte STRING = 0x4;
    private static final byte ARRAY = 0x5;
    private static final byte OBJECT = 0x6;
    
    public static void serialize(IDataSerialization serialization, Object value)
    {
        if (value == null)
            serialization.writeByte(NULL);
        else if (value instanceof Long)
        {
            serialization.writeByte(LONG);
            serialization.writeLong((Long)value);
        }
        else if (value instanceof Boolean)
        {
            serialization.writeByte(BOOLEAN);
            serialization.writeBoolean((Boolean)value);
        }
        else if (value instanceof Double)
        {
            serialization.writeByte(DOUBLE);
            serialization.writeDouble((Double)value);
        }
        else if (value instanceof String)
        {
            serialization.writeByte(STRING);
            serialization.writeString((String)value);
        }
        else if (value instanceof JsonArray)
        {
            serialization.writeByte(ARRAY);
            JsonArray list = (JsonArray)value;
            serialization.writeInt(list.size());
            for (Object element : list)
                serialize(serialization, element);
        }
        else if (value instanceof JsonObject)
        {
            serialization.writeByte(OBJECT);
            JsonObject map = (JsonObject)value;
            serialization.writeInt(map.size());
            
            for (Map.Entry<String, Object> entry : map)
            {
                serialization.writeString(entry.getKey());
                serialize(serialization, entry.getValue());
            }
        }
        else
            throw new SerializationException();
    }
    
    public static <T> T deserialize(IDataDeserialization deserialization)
    {
        byte type = deserialization.readByte();
        switch (type)
        {
        case NULL:
            return null;
        case LONG:
            return (T)Long.valueOf(deserialization.readLong());
        case BOOLEAN:
            return (T)Boolean.valueOf(deserialization.readBoolean());
        case DOUBLE:
            return (T)Double.valueOf(deserialization.readDouble());
        case STRING:
            return (T)deserialization.readString();
        case ARRAY:
        {
            int count = deserialization.readInt();
            List<Object> list = new ArrayList<Object>(count);
            for (int i = 0; i < count; i++)
                list.add(deserialize(deserialization));
            
            return (T)new JsonArray(Immutables.wrap(list));
        }
        case OBJECT:
        {
            int count = deserialization.readInt();
            Map<String, Object> map = new LinkedHashMap<String, Object>(count);
            for (int i = 0; i < count; i++)
            {
                String key = deserialization.readString();
                Object value = deserialize(deserialization);
                map.put(key, value);
            }
            
            return (T)new JsonObject(Immutables.wrap(map));
        }
        default:
            throw new SerializationException();
        }
    }
    
    public static <T> T load(String resourceLocation, boolean useBuilder)
    {
        Map<String, IResourceLoader> resourceLoaders = new HashMap<String, IResourceLoader>();
        resourceLoaders.put(FileResourceLoader.SCHEMA, new FileResourceLoader());
        resourceLoaders.put(ClassPathResourceLoader.SCHEMA, new ClassPathResourceLoader());
        ResourceManager resourceManager = new ResourceManager(resourceLoaders, "file");
        InputStream stream = null;
        try
        {
            stream = resourceManager.getResource(resourceLocation);
            return read(new InputStreamReader(stream, "UTF-8"), useBuilder);
        }
        catch (Exception e)
        {
            return Exceptions.wrapAndThrow(e);
        }
        finally
        {
            IOs.close(stream);
        }
    }
    
    public static <T> T read(Reader reader, boolean useBuilder)
    {
        Assert.notNull(reader);
        
        JsonModelHandler handler = new JsonModelHandler(useBuilder);
        JsonReader jsonReader = new JsonReader(reader, handler);
        handler.setReader(jsonReader);
        jsonReader.read();
        
        return (T)handler.getResult();
    }
    
    public static <T> T read(String str, boolean useBuilder)
    {
        Assert.notNull(str);
        
        return read(new StringReader(str), useBuilder);
    }
    
    public static void write(Writer writer, Object value, boolean format)
    {
        Assert.notNull(writer);
     
        JsonWriter jsonWriter = new JsonWriter(writer, format ? 4 : 0);
        
        jsonWriter.startText();
        
        write(jsonWriter, value);
        
        jsonWriter.endText();
    }
    
    public static void write(File file, Object value, boolean format)
    {
        Writer writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(file));
            write(writer, value, format);
        }
        catch (IOException e)
        {
            Exceptions.wrapAndThrow(e);
        }
        finally
        {
            IOs.close(writer);
        }
    }
    
    public static String write(Object value, boolean format)
    {
        StringWriter writer = new StringWriter();
        write(writer, value, format);
        
        return writer.toString();
    }
    
    public static void write(IJsonHandler handler, Object value)
    {
        if (value instanceof JsonObject)
        {
            handler.startObject();
            
            for (Map.Entry<String, Object> entry : (JsonObject)value)
            {
                handler.key(entry.getKey());
                write(handler, entry.getValue());
            }
            
            handler.endObject();
        }
        else if (value instanceof JsonArray)
        {
            handler.startArray();
            
            for (Object element : (JsonArray)value)
                write(handler, element);
            
            handler.endArray();
        }
        else
            handler.value(value);
    }

    private JsonSerializers()
    {
    }
}
