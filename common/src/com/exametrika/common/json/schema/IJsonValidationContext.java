/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.List;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;




/**
 * The {@link IJsonValidationContext} represents a JSON validation context.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IJsonValidationContext
{
    public static class PathElement
    {
    }
    
    public static class ObjectPathElement extends PathElement
    {
        public final JsonType type;
        public final JsonObject object;
        public final String key;

        public ObjectPathElement(JsonType type, JsonObject object, String key)
        {
            Assert.notNull(object);
            Assert.notNull(key);
            
            this.type = type;
            this.object = object;
            this.key = key;
        }
    }
    
    public static class ArrayPathElement extends PathElement
    {
        public final JsonArrayType type;
        public final JsonArray array;
        public final int index;

        public ArrayPathElement(JsonArrayType type, JsonArray array, int index)
        {
            Assert.notNull(array);
            
            this.type = type;
            this.array = array;
            this.index = index;
        }
    }
    
    /**
     * Returns validating schema.
     *
     * @return validating schema
     */
    JsonSchema getSchema();
    
    /**
     * Returns diagnostics.
     *
     * @return diagnostics
     */
    IJsonDiagnostics getDiagnostics();
    
    /**
     * Returns validation path.
     *
     * @return validation path
     */
    List<PathElement> getPathList();
    
    /**
     * Returns validation path.
     *
     * @return validation path
     */
    String getPath();
    
    /**
     * Returns type of root element.
     *
     * @return type of root element
     */
    JsonType getRootType();
    
    /**
     * Returns root validated object.
     *
     * @return root validated object
     */
    Object getRoot();
}
