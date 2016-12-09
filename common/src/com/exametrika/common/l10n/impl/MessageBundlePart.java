/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n.impl;

import java.util.Locale;
import java.util.Map;

import com.exametrika.common.utils.Assert;


/**
 * The {@link MessageBundlePart} contains messages for specific locale.
 *
 * @threadsafety This class and its methods are thread safe.
 * 
 * @author Medvedev-A
 */
public final class MessageBundlePart
{
    private final Locale locale;
    private final MessageBundlePart parent;
    private final Map<String, String> messages;

    /**
     * Creates a new object.
     *
     * @param locale part locale
     * @param parent parent part. Can be null for root part
     * @param messages map of messages as {messageId : message}
     */
    public MessageBundlePart(Locale locale, MessageBundlePart parent, Map<String, String> messages)
    {
        Assert.notNull(locale);
        Assert.notNull(messages);
        
        this.locale = locale;
        this.parent = parent;
        this.messages = messages;
    }
 
    /**
     * Returns part's locale.
     *
     * @return part's locale
     */
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * Returns parent part. Parent part contains messages for less
     * specific locale. For root part parent is <c>null<c>. Root part
     * contains messages for least specific locale available.
     *
     * @return parent part or <c>null<c> if current part is root
     */
    public MessageBundlePart getParent()
    {
        return parent;
    }

    /**
     * Returns message for specified identifier. If message is not found in current part,
     * message search is delegated to parent (if any). 
     *
     * @param messageId message identifier
     * @return message or <c>null<c> if message is not found
     */
    public String getMessage(String messageId)
    {
        String message = messages.get(messageId);
        if (message != null)
            return message;
        
        if (parent != null)
            return parent.getMessage(messageId);

        return null;
    }
}
