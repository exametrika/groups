/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;


/**
 * The {@link ILocaleProvider} provides locale.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILocaleProvider
{
    /**
     * Returns locale.
     *
     * @return locale
     */
    Locale getLocale();
}
