/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;





/**
 * The {@link JsonLongType} is a JSON long type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonLongType extends JsonType
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    private final long min;
    private final boolean minExclusive;
    private final long max;
    private final boolean maxExclusive;

    public JsonLongType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators, 
        IJsonConverter converter, long min, boolean minExclusive, long max, boolean maxExclusive)
    {
        super(name, description, enumeration, annotation, validators, converter);
        
        if (validators != null)
        {
            for (IJsonValidator validator : validators)
                Assert.isTrue(validator.supports(Long.class));
        }

        this.min = min;
        this.minExclusive = minExclusive;
        this.max = max;
        this.maxExclusive = maxExclusive;
    }
    
    public long getMin()
    {
        return min;
    }

    public boolean isMinExclusive()
    {
        return minExclusive;
    }

    public long getMax()
    {
        return max;
    }

    public boolean isMaxExclusive()
    {
        return maxExclusive;
    }

    @Override
    public boolean supports(Object instance)
    {
        if (instance instanceof String)
        {
            try
            {
                Long.decode((String)instance);
                return true;
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }
        else
            return instance instanceof Number;
    }
    
    @Override
    protected Set<String> getSupportedTypes()
    {
        return Collections.singleton("long");
    }
    
    @Override
    protected Object doValidate(Object instance, JsonValidationContext context)
    {
        super.doValidate(instance, context);
        
        JsonDiagnostics diagnostics = context.getDiagnostics();
        long number;
        if (instance instanceof Number)
            number = ((Number)instance).longValue();
        else
            number = Long.decode((String)instance);
        
        if (number < min || (number == min && minExclusive))
        {
            if (!minExclusive)
                diagnostics.addError(messages.valueLessMin(diagnostics.getPath(), number, min));
            else
                diagnostics.addError(messages.valueLessMinExclusive(diagnostics.getPath(), number, min));
        }
        
        if (number > max || (number == max && maxExclusive))
        {
            if (!maxExclusive)
                diagnostics.addError(messages.valueGreaterMax(diagnostics.getPath(), number, max));
            else
                diagnostics.addError(messages.valueGreaterMaxExclusive(diagnostics.getPath(), number, max));
        }
        
        return number;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Value ''{1}'' is greater than maximal allowed ''{2}''.")
        ILocalizedMessage valueGreaterMax(String path, long value, long max);
        
        @DefaultMessage("Validation error of ''{0}''. Value ''{1}'' is greater than maximal allowed ''{2}''(exclusive).")
        ILocalizedMessage valueGreaterMaxExclusive(String path, long value, long max);

        @DefaultMessage("Validation error of ''{0}''. Value ''{1}'' is less than minimal allowed ''{2}''.")
        ILocalizedMessage valueLessMin(String path, long value, long min);
        
        @DefaultMessage("Validation error of ''{0}''. Value ''{1}'' is less than minimal allowed ''{2}''(exclusive).")
        ILocalizedMessage valueLessMinExclusive(String path, long value, long min);
    }
}
