/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.LinkedList;
import java.util.List;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;





/**
 * The {@link JsonValidationContext} is a JSON validation context.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JsonValidationContext implements IJsonValidationContext
{
    private final JsonSchema schema;
    private final JsonDiagnostics diagnostics;
    private final JsonType rootType;
    private final Object root;
    private final LinkedList<PathElement> path;

    public JsonValidationContext(JsonValidationContext parent, JsonDiagnostics diagnostics)
    {
        Assert.notNull(parent);
        Assert.notNull(diagnostics);
        
        this.schema = parent.schema;
        this.diagnostics = diagnostics;
        this.rootType = parent.rootType;
        this.root = parent.root;
        this.path = parent.path;
    }
    
    public JsonValidationContext(JsonDiagnostics diagnostics, Object root)
    {
        Assert.notNull(diagnostics);
        Assert.notNull(root);
        
        this.schema = null;
        this.diagnostics = diagnostics;
        this.rootType = null;
        this.root = root;
        this.path = new LinkedList<PathElement>();
    }
    
    public JsonValidationContext(JsonSchema schema, JsonDiagnostics diagnostics, JsonType rootType, Object root)
    {
        Assert.notNull(schema);
        Assert.notNull(diagnostics);
        Assert.notNull(rootType);
        Assert.notNull(root);
        
        this.schema = schema;
        this.diagnostics = diagnostics;
        this.rootType = rootType;
        this.root = root;
        this.path = new LinkedList<PathElement>();
    }
    
    @Override
    public JsonSchema getSchema()
    {
        return schema;
    }

    @Override
    public JsonDiagnostics getDiagnostics()
    {
        return diagnostics;
    }

    @Override
    public JsonType getRootType()
    {
        return rootType;
    }
    
    @Override
    public Object getRoot()
    {
        return root;
    }
    
    @Override
    public List<PathElement> getPathList()
    {
        return Immutables.wrap(path);
    }
    
    @Override
    public String getPath()
    {
        StringBuilder builder = new StringBuilder();
        for (PathElement element: path)
        {
            if (element instanceof ObjectPathElement)
            {
                builder.append("[\"");
                builder.append(((ObjectPathElement)element).key);
                builder.append("\"]");
            }
            else if (element instanceof ArrayPathElement)
            {
                builder.append("[");
                builder.append(((ArrayPathElement)element).index);
                builder.append("]");
            }
        }
        return builder.toString();
    }
    
    public void beginObjectElement(JsonType type, JsonObject object, String key)
    {
        path.addLast(new ObjectPathElement(type, object, key));
    }
    
    public void beginArrayElement(JsonArrayType type, JsonArray array, int index)
    {
        path.addLast(new ArrayPathElement(type, array, index));
    }

    public void endElement()
    {
        path.removeLast();
    }
    
    @Override
    public String toString()
    {
        return diagnostics.getPath();
    }
}
