/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;


/**
 * The {@link AbstractExtensionLoader} is an abstract implementation of {@link IExtensionLoader}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractExtensionLoader implements IExtensionLoader
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private volatile IExtensionLoader extensionLoader;

    @Override
    public final void setExtensionLoader(IExtensionLoader extensionLoader)
    {
        this.extensionLoader = extensionLoader; 
    }
    
    @Override
    public abstract Object loadExtension(String name, String type, Object element, ILoadContext context);
    
    protected final <T> T load(String name, String type, Object element, ILoadContext context)
    {
        Assert.notNull(context);
        
        if (element == null)
            return null;
        
        if (extensionLoader != null)
            return (T)extensionLoader.loadExtension(name, type, element, context);
        else
            throw new InvalidConfigurationException(messages.extensionLoaderNotSet());
    }
    
    protected final <T> T load(String name, String type, Object element, ILoadContext context, T defaultValue)
    {
        T value = load(name, type, element, context);
        if (value != null)
            return value;
        else
            return defaultValue;
    }
    
    protected final String getType(JsonObject element)
    {
        if (element.contains("instanceOf"))
            return element.get("instanceOf");
        else
            throw new InvalidConfigurationException(messages.typeNotSetForNestedElement(element));
    }
    
    protected final String getType(JsonObject element, String defaultValue)
    {
        if (element.contains("instanceOf"))
            return element.get("instanceOf");
        else
            return defaultValue;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Type is not set for configuration element ''{0}''.")
        ILocalizedMessage typeNotSetForNestedElement(JsonObject element);
        @DefaultMessage("Extension loader is not set.")
        ILocalizedMessage extensionLoaderNotSet();
    }
}
