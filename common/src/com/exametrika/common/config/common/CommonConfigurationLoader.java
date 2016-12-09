/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.common;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.NameFilter;



/**
 * The {@link CommonConfigurationLoader} is a configuration loader for common configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CommonConfigurationLoader extends AbstractElementLoader implements IExtensionLoader
{
    @Override
    public void loadElement(JsonObject element, ILoadContext context)
    {
        CommonLoadContext commonContext = context.get(CommonConfiguration.SCHEMA);

        String modeElement = element.get("runtimeMode");
        RuntimeMode runtimeMode = RuntimeMode.DEVELOPMENT;
        if (modeElement.equals("development"))
            runtimeMode = RuntimeMode.DEVELOPMENT;
        else if (modeElement.equals("production"))
            runtimeMode = RuntimeMode.PRODUCTION;
        else
            Assert.error();
        
        commonContext.setRuntimeMode(runtimeMode);
    }

    @Override
    public Object loadExtension(String name, String type, Object element, ILoadContext context)
    {
        if (type.equals("NameFilter") || type.equals("CompoundNameFilterExpression"))
            return loadNameFilter(element);
        else
            throw new InvalidConfigurationException();
    }

    private NameFilter loadNameFilter(Object element)
    {
        if (element == null)
            return null;
        else if (element instanceof String)
            return new NameFilter((String)element);
        
        JsonObject object = (JsonObject)element;
        String expression = object.get("expression", null);
        List<NameFilter> includeFilters = loadNameFilters((JsonArray)object.get("include", null));
        List<NameFilter> excludeFilters = loadNameFilters((JsonArray)object.get("exclude", null));
        
        return new NameFilter(expression, includeFilters, excludeFilters);
    }

    private List<NameFilter> loadNameFilters(JsonArray element)
    {
        if (element == null)
            return null;
        
        List<NameFilter> list = new ArrayList<NameFilter>();
        for (Object e : element)
            list.add(loadNameFilter(e));
        
        return list;
    }
}
