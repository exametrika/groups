/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;





/**
 * The {@link JsonStringType} is a JSON string type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonStringType extends JsonType
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String pattern;
    private final int minCount;
    private final int maxCount;
    private final Pattern compiledPattern;

    public JsonStringType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators, 
        IJsonConverter converter, String pattern, int minCount, int maxCount)
    {
        super(name, description, enumeration, annotation, validators, converter);
        
        if (validators != null)
        {
            for (IJsonValidator validator : validators)
                Assert.isTrue(validator.supports(String.class));
        }

        this.pattern = pattern;
        this.minCount = minCount;
        this.maxCount = maxCount;
        
        if (pattern != null)
            compiledPattern = java.util.regex.Pattern.compile(pattern);
        else
            compiledPattern = null;
    }
    
    public String getPattern()
    {
        return pattern;
    }

    public int getMinCount()
    {
        return minCount;
    }

    public int getMaxCount()
    {
        return maxCount;
    }

    @Override
    public boolean supports(Object instance)
    {
        return instance instanceof String;
    }
    
    @Override
    protected Set<String> getSupportedTypes()
    {
        return Collections.singleton("string");
    }
    
    @Override
    protected Object doValidate(Object instance, JsonValidationContext context)
    {
        super.doValidate(instance, context);
        
        JsonDiagnostics diagnostics = context.getDiagnostics();
        String string = (String)instance;
        if (string.length() < minCount)
            diagnostics.addError(messages.lengthLessMin(diagnostics.getPath(), string.length(), minCount));
        
        if (string.length() > maxCount)
            diagnostics.addError(messages.lengthGreaterMax(diagnostics.getPath(), string.length(), maxCount));
        
        if (compiledPattern != null && !compiledPattern.matcher(string).matches())
            diagnostics.addError(messages.stringNotMatch(diagnostics.getPath(), string, pattern));
        
        return instance;
    }
    
    private interface IMessages
    {        
        @DefaultMessage("Validation error of ''{0}''. String length ''{1}'' is greater than maximal allowed ''{2}''.")
        ILocalizedMessage lengthGreaterMax(String path, int length, int maxCount);

        @DefaultMessage("Validation error of ''{0}''. String length ''{1}'' is less than minimal allowed ''{2}''.")
        ILocalizedMessage lengthLessMin(String path, int length, int minCount);
        
        @DefaultMessage("Validation error of ''{0}''. String value ''{1}'' does not match pattern ''{2}''.")
        ILocalizedMessage stringNotMatch(String path, String value, String pattern);
    }
}
