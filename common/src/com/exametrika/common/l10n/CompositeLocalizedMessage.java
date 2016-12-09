/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.List;
import java.util.Locale;

import com.exametrika.common.utils.Assert;



/**
 * The {@link CompositeLocalizedMessage} is a composite localized message.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class CompositeLocalizedMessage implements ILocalizedMessage
{
    private final List<ILocalizedMessage> messages;

    public CompositeLocalizedMessage(List<ILocalizedMessage> messages)
    {
        Assert.notNull(messages);
        
        this.messages = messages;
    }
    
    @Override
    public String getString()
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (ILocalizedMessage message : messages)
        {
            if (first)
                first = false;
            else
                builder.append('\n');
            
            builder.append(message.getString());
        }
        return builder.toString();
    }

    @Override
    public String getString(Locale locale)
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (ILocalizedMessage message : messages)
        {
            if (first)
                first = false;
            else
                builder.append('\n');
            
            builder.append(message.getString(locale));
        }
        return builder.toString();
    }
    
    @Override
    public String toString()
    {
        return getString();
    }
}
