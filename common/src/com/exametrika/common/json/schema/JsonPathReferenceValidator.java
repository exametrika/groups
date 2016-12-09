/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;





/**
 * The {@link JsonPathReferenceValidator} is a validator of path references.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonPathReferenceValidator implements IJsonValidator
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String path;
    
    public JsonPathReferenceValidator(String path)
    {
        Assert.notNull(path);
        this.path = path;
    }
    
    @Override
    public boolean supports(Class clazz)
    {
        return clazz == String.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context)
    {
        String reference = (String)instance;
        
        IJsonDiagnostics diagnostics = context.getDiagnostics();
        
        JsonObject referents = ((JsonObject)context.getRoot()).select(path, null);
        Object referent = null;
        if (referents != null)
            referent = referents.get(reference, null);
        if (!(referent instanceof JsonObject))
            diagnostics.addError(messages.referentNotFound(diagnostics.getPath(), reference, path));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Referenced object ''{1}'' is not found on path ''{2}''.")
        ILocalizedMessage referentNotFound(String path, String type, String referentPath);
    }
}
