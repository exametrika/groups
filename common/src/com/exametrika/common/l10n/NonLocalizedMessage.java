/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;



/**
 * The {@link NonLocalizedMessage} is a non localized message.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class NonLocalizedMessage implements ILocalizedMessage
{
    private final String message;

    public NonLocalizedMessage(String message)
    {
        this.message = message != null ? message : "";
    }
    
    @Override
    public String getString()
    {
        return message;
    }

    @Override
    public String getString(Locale locale)
    {
        return message;
    }
    
    @Override
    public String toString()
    {
        return message;
    }
}
