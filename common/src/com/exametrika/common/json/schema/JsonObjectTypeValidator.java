/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;





/**
 * The {@link JsonObjectTypeValidator} is a validator of object type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonObjectTypeValidator implements IJsonValidator
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    @Override
    public boolean supports(Class clazz)
    {
        return clazz == JsonObject.class || clazz == JsonObjectBuilder.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context)
    {
        JsonObject instanceType = (JsonObject)instance;
        
        IJsonDiagnostics diagnostics = context.getDiagnostics();
        
        boolean isAbstract = JsonUtils.get(instanceType, "abstract", Boolean.class, false);
        boolean isFinal = JsonUtils.get(instanceType, "final", Boolean.class, false);
        boolean open = JsonUtils.get(instanceType, "open", Boolean.class, false);
        String baseReference =JsonUtils.get(instanceType, "base", String.class, null);
        JsonObject base = getBase(context.getRoot(), baseReference);
        JsonArray enumeration = JsonUtils.get(instanceType, "enumeration", JsonArray.class, null);
        
        if (isAbstract && isFinal)
            diagnostics.addError(messages.abstractAndFinal(diagnostics.getPath()));
        
        if (!isFinal && enumeration != null)
            diagnostics.addError(messages.nonFinalEnumeration(diagnostics.getPath()));
        
        if (base != null)
        {
            if (JsonUtils.get(base, "final", Boolean.class, false))
                diagnostics.addError(messages.baseFinal(diagnostics.getPath(), baseReference));
            
            if (open && !JsonUtils.get(base, "open", Boolean.class, false))
                diagnostics.addError(messages.baseNotOpen(diagnostics.getPath(), baseReference));
        }
    }
    
    private JsonObject getBase(Object root, String baseReference)
    {
        if (baseReference == null)
            return null;
        
        JsonObject schemaTypes = ((JsonObject)root).get("types");
        Object referencedType = schemaTypes.get(baseReference, null);
        if (!(referencedType instanceof JsonObject))
            return null;

        Object instanceOf = ((JsonObject)referencedType).get("instanceOf", null);
        if (!(instanceOf instanceof String))
            return null;

        String typeName = (String)instanceOf;
        if (!typeName.equals("object"))
            return null;
        
        return (JsonObject)referencedType;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Object can not be abstract and final at the same time.")
        ILocalizedMessage abstractAndFinal(String path);
        
        @DefaultMessage("Validation error of ''{0}''. Base object type ''{1}'' is final. Base type can not be final.")
        ILocalizedMessage baseFinal(String path, String typeName);
        
        @DefaultMessage("Validation error of ''{0}''. Enumeration can be set in a final object type only.")
        ILocalizedMessage nonFinalEnumeration(String path);
        
        @DefaultMessage("Validation error of ''{0}''. Base object type ''{1}'' is close. Base type for open type must also be open.")
        ILocalizedMessage baseNotOpen(String path, String typeName);
    }
}
