/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Set;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;





/**
 * The {@link JsonTypeReferenceValidator} is a validator of schema type references.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonTypeReferenceValidator implements IJsonValidator
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String metaType;
    private final Set<String> predefinedTypes;

    public JsonTypeReferenceValidator(String metaType, Set<String> predefinedTypes)
    {
        Assert.notNull(predefinedTypes);
        
        this.metaType = metaType;
        this.predefinedTypes = predefinedTypes;
    }
    
    @Override
    public boolean supports(Class clazz)
    {
        return clazz == String.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context)
    {
        String typeReference = (String)instance;
        if (predefinedTypes.contains(typeReference))
            return;
        
        IJsonDiagnostics diagnostics = context.getDiagnostics();
        
        JsonObject schemaTypes = ((JsonObject)context.getRoot()).get("types");
        Object referencedType = schemaTypes.get(typeReference, null);
        if (!(referencedType instanceof JsonObject))
            diagnostics.addError(messages.typeNotFound(diagnostics.getPath(), typeReference));
        else if (metaType != null)
        {
            Object instanceOf = ((JsonObject)referencedType).get("instanceOf", null);
            if (!(instanceOf instanceof String))
                diagnostics.addError(messages.typeNotFound(diagnostics.getPath(), typeReference));
            else
            {
                String typeName = (String)instanceOf;
                if (!typeName.equals(metaType))
                    diagnostics.addError(messages.wrongMetatype(diagnostics.getPath(), typeName, metaType));
            }
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Referenced type ''{1}'' is not found.")
        ILocalizedMessage typeNotFound(String path, String type);
        
        @DefaultMessage("Validation error of ''{0}''. Referenced type is a ''{1}'' type. Referenced type must be a ''{2}'' type.")
        ILocalizedMessage wrongMetatype(String path, String typeName, String metaType);
    }
}
