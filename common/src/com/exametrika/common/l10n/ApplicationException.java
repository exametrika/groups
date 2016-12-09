/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;


/**
 * The {@link ApplicationException} is an application exception whose message can be localized.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public class ApplicationException extends Exception implements ILocalizedException
{
    private transient final ILocalizedMessage message;
    
    public ApplicationException()
    {
        message = null;
    }

    public ApplicationException(ILocalizedMessage message) 
    {
        super(message != null ? message.getString() : null);
        
        this.message = message;
    }

    public ApplicationException(ILocalizedMessage message, Throwable cause) 
    {
        super(message != null ? message.getString() : null, cause);
        
        this.message = message;
    }
    
    public ApplicationException(Throwable cause) 
    {
        super(cause);
        
        if (cause instanceof ILocalizedException)
            message = ((ILocalizedException)cause).getLocalized();
        else
            message = null;
    }
 
    @Override
    public final String getMessage()
    {
        return message != null ? message.getString() : super.getMessage();
    }
    
    @Override
    public final String getMessage(Locale locale)
    {
        return message != null ? message.getString(locale) : super.getMessage();
    }
    
    @Override
    public final ILocalizedMessage getLocalized()
    {
        return message;
    }
}
