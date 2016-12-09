/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.exametrika.common.l10n.CompositeLocalizedMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;





/**
 * The {@link JsonDiagnostics} is a JSON diagnostics object.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JsonDiagnostics implements IJsonDiagnostics
{
    private final List<ILocalizedMessage> errors = new ArrayList<ILocalizedMessage>();
    private final LinkedList<String> path;
    private String pathStr;
    
    public JsonDiagnostics()
    {
        path = new LinkedList<String>();
    }
    
    public JsonDiagnostics(JsonDiagnostics parent)
    {
        Assert.notNull(parent);
        
        path = parent.path;
    }
    
    @Override
    public void addError(ILocalizedMessage error)
    {
        Assert.notNull(error);
        
        errors.add(error);
    }
    
    public void addErrors(JsonDiagnostics diagnostics)
    {
        errors.addAll(diagnostics.errors);
    }
    
    public boolean isValid()
    {
        return errors.isEmpty();
    }
    
    public List<ILocalizedMessage> getErrors()
    {
        return Immutables.wrap(errors);
    }
    
    public void checkErrors()
    {
        if (!errors.isEmpty())
            throw new JsonValidationException(new CompositeLocalizedMessage(new ArrayList<ILocalizedMessage>(errors)));
    }
    
    @Override
    public String getPath()
    {
        if (pathStr != null)
            return pathStr;
        
        StringBuilder builder = new StringBuilder();
        for (String segment : path)
            builder.append(segment);

        pathStr = builder.toString();
        
        return pathStr;
    }
    
    public void beginType(String name)
    {
        path.addLast("{" + name + "}");
        pathStr = null;
    }
    
    public void beginIndex(int index)
    {
        path.addLast("[" + index + "]");
        pathStr = null;
    }
    
    public void beginProperty(String name)
    {
        path.addLast("." + name);
        pathStr = null;
    }
    
    public void end()
    {
        path.removeLast();
        pathStr = null;
    }
    
    @Override
    public String toString()
    {
        return Strings.toString(errors, false);
    }
}
