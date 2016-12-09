/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;




/**
 * The {@link IJsonHandler} represents a JSON text handler.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IJsonHandler
{
    /**
     * Called when text is started.
     */
    void startText();
    
    /**
     * Called when text is ended.
     */
    void endText();
    
    /**
     * Called when object is started.
     */
    void startObject();
    
    /**
     * Called when object is ended.
     */
    void endObject();
    
    /**
     * Called when array is started.
     */
    void startArray();
    
    /**
     * Called when array is ended.
     */
    void endArray();
    
    /**
     * Called when key has been read.
     * 
     * @param key key
     */
    void key(String key);
    
    /**
     * Called when value has been read.
     * 
     * @param value value. Can be null if 'null' literal has been read
     */
    void value(Object value);
}
