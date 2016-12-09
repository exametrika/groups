/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;




/**
 * The {@link JsonCompoundType} is a JSON compound type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonCompoundType extends JsonType
{
    private List<JsonType> types;

    public JsonCompoundType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators,
        IJsonConverter converter)
    {
        super(name, description, enumeration, annotation, validators, converter);
    }
    
    public List<JsonType> getTypes()
    {
        return types;
    }

    public void setTypes(List<JsonType> types)
    {
        Assert.notNull(types);
        Assert.isTrue(types.size() > 0);
        Assert.checkState(!frozen);
        
        this.types = Immutables.wrap(types);
    }
    
    @Override
    public void freeze()
    {
        if (!frozen)
        {
            super.freeze();
            Assert.checkState(types != null);
            
            for (JsonType type : types)
                type.freeze();
        }
    }
    
    @Override
    public boolean supports(Object instance)
    {
        for (JsonType type : types)
        {
            if (type.supports(instance))
                return true;
        }
        
        return false;
    }
    
    @Override
    protected Set<String> getSupportedTypes()
    {
        Set<String> types = new TreeSet<String>();
        for (JsonType type : this.types)
            types.addAll(type.getSupportedTypes());
        return types;
    }
    
    @Override
    protected Object doValidate(Object instance, JsonValidationContext context)
    {
        super.doValidate(instance, context);
        
        JsonDiagnostics diagnostics = context.getDiagnostics();
        
        JsonDiagnostics typesDiagnostics = new JsonDiagnostics();
        for (JsonType type : types)
        {
            if (type.supports(instance))
            {
                JsonDiagnostics typeDiagnostics = new JsonDiagnostics(diagnostics);
                Object res = type.validate(instance, new JsonValidationContext(context, typeDiagnostics));
                
                if (typeDiagnostics.isValid())
                    return res;
                
                typesDiagnostics.addErrors(typeDiagnostics);
            }
        }
        
        diagnostics.addErrors(typesDiagnostics);
        
        return instance;
    }
}
