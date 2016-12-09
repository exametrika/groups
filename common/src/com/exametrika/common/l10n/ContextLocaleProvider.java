/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;

import com.exametrika.common.utils.Assert;


/**
 * The {@link ContextLocaleProvider} is a locale provider that provides context locale of current thread or default locale,
 * if context locale of current thread is not set.
 * 
 * @see Locale
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class ContextLocaleProvider implements ILocaleProvider
{
    private ThreadLocal<Locale> locale = new ThreadLocal<Locale>();
    
    @Override
    public Locale getLocale()
    {
        Locale contextLocale = locale.get();
        if (contextLocale != null)
            return contextLocale;
        
        return Locale.getDefault();
    }
    
    /**
     * Returns context locale of current thread or null if context locale is not set.
     *
     * @return context locale of current thread or null if context locale is not set
     */
    public Locale getContextLocale()
    {
        return locale.get();
    }
    
    /**
     * Sets context locale of current thread.
     *
     * @param locale context locale of current thread
     */
    public void setContextLocale(Locale locale)
    {
        Assert.notNull(locale);
        
        this.locale.set(locale);
    }
    
    /**
     * Removes context locale of current thread.
     */
    public void removeContextLocale()
    {
        locale.remove();
    }
}
