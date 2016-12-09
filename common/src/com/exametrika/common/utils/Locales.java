/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.Locale;

/**
 * The {@link Locales} contains different utility methods for locale creation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Locales
{
    /**
     * Returns locale by language, country and variant separated by underscore.
     *
     * @param locale locale identifier
     * @return locale
     */
    public static Locale getLocale(String locale)
    {
        Assert.notNull(locale);
        
        String language = "";
        String country = "";
        String variant = "";
        
        String[] parts = locale.split("_");
        if (parts.length > 0)
            language = parts[0];
        if (parts.length > 1)
            country = parts[1];
        if (parts.length > 2)
            variant = parts[2];
        
        return new Locale(language, country, variant);
    }
    
    private Locales()
    {
    }
}
