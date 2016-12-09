/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;


/**
 * The {@link ILocalizedException} represents localized exception.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILocalizedException
{
    /**
     * Returns the cause of this throwable or <code>null</code> if the
     * cause is nonexistent or unknown.  (The cause is the throwable that
     * caused this throwable to get thrown.)
     *
     * @return  the cause of this throwable or <code>null</code> if the
     *          cause is nonexistent or unknown.*/
    Throwable getCause();
    
    /**
     * Returns message string for default locale.
     *
     * @return message string
     */
    String getMessage();
    
    /**
     * Returns message string for specified locale.
     *
     * @param locale message locale
     * @return message string
     */
    String getMessage(Locale locale);
    
    /**
     * Returns localized message.
     *
     * @return localized message
     */
    ILocalizedMessage getLocalized();
}
