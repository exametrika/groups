/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;


/**
 * The {@link ILocalizedMessage} represents localized message.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILocalizedMessage
{
    /**
     * Returns message string for default locale.
     *
     * @return message string
     */
    String getString();
    
    /**
     * Returns message string for specified locale.
     *
     * @param locale message locale
     * @return message string
     */
    String getString(Locale locale);
}
