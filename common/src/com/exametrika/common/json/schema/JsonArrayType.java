/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;




/**
 * The {@link JsonArrayType} is a JSON array type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonArrayType extends JsonType
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int minCount;
    private final int maxCount;
    private final boolean allowDuplicates;
    private final boolean allowNulls;
    private JsonType elementType;

    public JsonArrayType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators, 
        IJsonConverter converter, int minCount, int maxCount, boolean allowDuplicates, boolean allowNulls)
    {
        super(name, description, enumeration, annotation, validators, converter);
        
        if (validators != null)
        {
            for (IJsonValidator validator : validators)
            {
                Assert.isTrue(validator.supports(JsonArray.class));
                Assert.isTrue(validator.supports(JsonArrayBuilder.class));
            }
        }
        
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.allowDuplicates = allowDuplicates;
        this.allowNulls = allowNulls;
    }
    
    public int getMinCount()
    {
        return minCount;
    }

    public int getMaxCount()
    {
        return maxCount;
    }

    public boolean isAllowDuplicates()
    {
        return allowDuplicates;
    }

    public boolean isAllowNulls()
    {
        return allowNulls;
    }

    public JsonType getElementType()
    {
        return elementType;
    }

    public void setElementType(JsonType elementType)
    {
        Assert.notNull(elementType);
        Assert.checkState(!frozen);
        
        this.elementType = elementType;
    }
    
    @Override
    public void freeze()
    {
        if (!frozen)
        {
            super.freeze();
            Assert.checkState(elementType != null);
            
            elementType.freeze();
        }
    }
    
    @Override
    public boolean supports(Object instance)
    {
        return instance instanceof JsonArray;
    }
    
    @Override
    protected Set<String> getSupportedTypes()
    {
        return Collections.singleton("array");
    }
    
    @Override
    protected Object doValidate(Object instance, JsonValidationContext context)
    {
        super.doValidate(instance, context);
        
        JsonDiagnostics diagnostics = context.getDiagnostics();
        JsonArrayBuilder array = (JsonArrayBuilder)JsonUtils.toBuilder(instance);
        instance = array;
        int size = array.size();

        if (size < minCount)
            diagnostics.addError(messages.countLessMin(diagnostics.getPath(), size, minCount));
        
        if (size > maxCount)
            diagnostics.addError(messages.countGreaterMax(diagnostics.getPath(), size, maxCount));
        
        Set<Object> elementSet = null;
        for (int i = 0; i < array.size(); i++)
        {
            Object element = array.get(i, null);
            diagnostics.beginIndex(i);
            if (element != null)
            {
                context.beginArrayElement(this, array, i);
                element = elementType.validate(element, context);
                context.endElement();
                
                array.set(i, element);
            }
            else if (!allowNulls)
                diagnostics.addError(messages.nullElement(diagnostics.getPath()));
            
            if (!allowDuplicates)
            {
                if (elementSet == null)
                    elementSet = new HashSet<Object>();
                
                if (elementSet.contains(element))
                    diagnostics.addError(messages.notUniqueElement(diagnostics.getPath(), element != null ? Strings.indent(element.toString(), 8) : "null"));
                else
                    elementSet.add(element);
            }
            diagnostics.end();
        }
        
        return instance;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. The following array element is not unique:\n{1}")
        ILocalizedMessage notUniqueElement(String path,String arrayElement);

        @DefaultMessage("Validation error of ''{0}''. Array element can not be null.")
        ILocalizedMessage nullElement(String path);

        @DefaultMessage("Validation error of ''{0}''. Count of array elements ''{1}'' is greater than maximal allowed ''{2}''.")
        ILocalizedMessage countGreaterMax(String path, int count, int maxCount);

        @DefaultMessage("Validation error of ''{0}''. Count of array elements ''{1}'' is less than minimal allowed ''{2}''.")
        ILocalizedMessage countLessMin(String path, int count, int minCount);
    }
}
