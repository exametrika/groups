/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.util.LinkedList;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;




/**
 * The {@link JsonModelHandler} is a Json model handler which builds json model.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JsonModelHandler implements IJsonHandler
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private JsonReader reader;
    private final boolean useBuilder;
    private IJsonCollection result;
    private final LinkedList<Object> builders = new LinkedList<Object>();
    private String key;
    
    public JsonModelHandler(boolean useBuilder)
    {
        this.useBuilder = useBuilder;
    }

    public IJsonCollection getResult()
    {
        return result;
    }

    public void setReader(JsonReader reader)
    {
        Assert.notNull(reader);
        
        this.reader = reader;
    }
    
    @Override
    public void startText()
    {
        result = null;
        builders.clear();
        key = null;
    }

    @Override
    public void endText()
    {
        if (!useBuilder)
        {
            if (result instanceof JsonObjectBuilder)
                result = ((JsonObjectBuilder)result).toJson();
            else if (result instanceof JsonArrayBuilder)
                result = ((JsonArrayBuilder)result).toJson();
        }
    }

    @Override
    public void startObject()
    {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        
        if (builders.isEmpty())
            result = builder;
        else 
        {
            Object parent = builders.getFirst();
            if (parent instanceof JsonObjectBuilder)
            {
                if (((JsonObjectBuilder)parent).put(key, builder) != null)
                    throwKeyAlreadyExists();
            }
            else if (parent instanceof JsonArrayBuilder)
                ((JsonArrayBuilder)parent).add(builder);
        }
        
        builders.addFirst(builder);
    }

    @Override
    public void endObject()
    {
        builders.removeFirst();
    }

    @Override
    public void startArray()
    {
        JsonArrayBuilder builder = new JsonArrayBuilder();
        
        if (builders.isEmpty())
            result = builder;
        else 
        {
            Object parent = builders.getFirst();
            if (parent instanceof JsonObjectBuilder)
            {
                if (((JsonObjectBuilder)parent).put(key, builder) != null)
                    throwKeyAlreadyExists();
            }
            else if (parent instanceof JsonArrayBuilder)
                ((JsonArrayBuilder)parent).add(builder);
        }
        
        builders.addFirst(builder);
    }

    @Override
    public void endArray()
    {
        builders.removeFirst();
    }

    @Override
    public void key(String key)
    {
        this.key = key;
    }

    @Override
    public void value(Object value)
    {
        Object builder = builders.getFirst();
        if (builder instanceof JsonObjectBuilder)
        {
            if (((JsonObjectBuilder)builder).put(key, value) != null)
                throwKeyAlreadyExists();
        }
        else if (builder instanceof JsonArrayBuilder)
            ((JsonArrayBuilder)builder).add(value);
    }

    private void throwKeyAlreadyExists()
    {
        if (reader != null)
            throw new JsonException(messages.keyAlreadyExists(key, reader.getPosition(), reader.getLine()));
        else
            throw new JsonException(messages.keyAlreadyExists(key));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Key ''{0}'' already exists at[ln:{2},col:{1}].")
        ILocalizedMessage keyAlreadyExists(String key, int pos, int line);
        
        @DefaultMessage("Key ''{0}'' already exists.")
        ILocalizedMessage keyAlreadyExists(String key);
    }  
}