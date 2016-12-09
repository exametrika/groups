/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.PropertiesReader;


/**
 * The {@link MessageBundle} contains localized messages. Message bundle consists of locale specific parts
 * organized in tree-like structure. Thre root part contains least specific locale messages,
 * the leaf parts - most specific locale messages. Message is searched from leaf parts to root.
 *
 * @threadsafety This class and its methods are thread safe.
 * 
 * @author Medvedev-A
 */
public final class MessageBundle
{
    private final String id;
    private final WeakReference<ClassLoader> classLoader;
    private volatile LinkedHashMap<Locale, MessageBundlePart> parts = new LinkedHashMap<Locale, MessageBundlePart>();

    public MessageBundle(String id, ClassLoader classLoader)
    {
        Assert.notNull(id);
        Assert.notNull(classLoader);
        
        this.id = id;
        this.classLoader = new WeakReference<ClassLoader>(classLoader);
    }
    
    /**
     * Returns message bundle's identifier.
     *
     * @return message bundle's identifier
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns message bundle's classloader used to load messages from.
     *
     * @return message bundle's classloader
     */
    public ClassLoader getClassLoader()
    {
        return classLoader.get();
    }

    /**
     * Returns message for specified identifier and locale. 
     *
     * @param messageId message identifier
     * @param locale message locale
     * @return message
     * @exception MissingResourceException if message is not found in current message bundle
     */
    public String getMessage(String messageId, Locale locale)
    {
        String message = findMessage(messageId, locale);
        if (message != null)
            return message;
        
        throw new MissingResourceException(MessageFormat.format(
            "Message with id ''{0}'' is not found in message bundle ''{1}''.",
            messageId, id), id, messageId);
    }
    
    /**
     * Finds message for specified identifier and locale. 
     *
     * @param messageId message identifier
     * @param locale message locale
     * @return message or null if message is not found
     */
    public String findMessage(String messageId, Locale locale)
    {
        Assert.notNull(messageId);
        Assert.notNull(locale);
        
        MessageBundlePart part = getPart(locale);
        
        String message = part.getMessage(messageId);
        if (message != null)
            return message;
        
        return null;
    }
    
    private MessageBundlePart getPart(Locale locale)
    {
        Map<Locale, MessageBundlePart> parts = this.parts;
        MessageBundlePart part = parts.get(locale);
        if (part == null)
            part = loadPart(locale);
        
        return part;
    }
    
    private synchronized MessageBundlePart loadPart(Locale locale)
    {
        MessageBundlePart part = parts.get(locale);
        if (part != null)
            return part;
        
        Locale parentLocale = getParentLocale(locale);
        
        MessageBundlePart parent;
        if (parentLocale != null)
            parent = getPart(parentLocale);
        else
            parent = null;
        
        Map<String, String> messages = null;
        InputStream stream = classLoader.get().getResourceAsStream(getResourceName(id, locale));
        if (stream != null)
        {
            Reader reader = new BufferedReader(new InputStreamReader(stream));
            PropertiesReader propertiesReader = new PropertiesReader();
            
            try
            {
                messages = propertiesReader.read(reader);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                IOs.close(reader);
            }
        }
        else
            messages = new LinkedHashMap<String, String>();
        
        part = new MessageBundlePart(locale, parent, messages);
        
        LinkedHashMap<Locale, MessageBundlePart> parts = (LinkedHashMap<Locale, MessageBundlePart>)this.parts.clone();
        parts.put(locale, part);
        this.parts = parts;
        
        return part;
    }
    
    private Locale getParentLocale(Locale locale)
    {
        if (locale.getVariant().length() != 0)
            return new Locale(locale.getLanguage(), locale.getCountry());
        else if (locale.getCountry().length() != 0)
            return new Locale(locale.getLanguage());
        else if (locale.getLanguage().length() != 0)
            return new Locale("");
        else
            return null;
    }
    
    private String getResourceName(String id, Locale locale)
    {
        String resourceName = id.replace('.', '/');
        if (locale.getLanguage().length() != 0)
            resourceName += '_' + locale.getLanguage();
        if (locale.getCountry().length() != 0)
            resourceName += '_' + locale.getCountry();
        if (locale.getVariant().length() != 0)
            resourceName += '_' + locale.getVariant();
        
        resourceName += ".messages";
        
        return resourceName;
    }
}
