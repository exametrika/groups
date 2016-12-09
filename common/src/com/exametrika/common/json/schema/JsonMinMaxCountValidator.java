/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;





/**
 * The {@link JsonMinMaxCountValidator} is a validator of minimal and maximal counts.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonMinMaxCountValidator implements IJsonValidator
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
        
        long minCount = JsonUtils.get(instanceType, "minCount", Long.class, 0l);
        long maxCount = loadMax(instanceType.get("maxCount", Integer.MAX_VALUE));
        if (minCount > maxCount)
            diagnostics.addError(messages.minCountGreaterMax(diagnostics.getPath(), minCount, maxCount));
    }
    
    private long loadMax(Object element)
    {
        if (element instanceof Long)
            return (Long)element;
        else
            return Integer.MAX_VALUE;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Minimal count ''{1}'' is greater than maximal count ''{2}''.")
        ILocalizedMessage minCountGreaterMax(String path, long minCount, long maxCount);
    }
}
