/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;


/**
 * The {@link SystemException} is an system exception whose message can be localized.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public class SystemException extends RuntimeException implements ILocalizedException
{
    private transient final ILocalizedMessage message;
    
    public SystemException()
    {
        message = null;
    }

    public SystemException(ILocalizedMessage message) 
    {
        super(message != null ? message.getString() : null);
        
        this.message = message;
    }

    public SystemException(ILocalizedMessage message, Throwable cause) 
    {
        super(message != null ? message.getString() : null, cause);
        
        this.message = message;
    }
    
    public SystemException(Throwable cause) 
    {
        super(cause);
        
        if (cause instanceof ILocalizedException)
            message = ((ILocalizedException)cause).getLocalized();
        else
            message = null;
    }
 
    @Override
    public String getMessage()
    {
        return message != null ? message.getString() : super.getMessage();
    }
    
    @Override
    public String getMessage(Locale locale)
    {
        return message != null ? message.getString(locale) : super.getMessage();
    }
    
    @Override
    public final ILocalizedMessage getLocalized()
    {
        return message;
    }
}
