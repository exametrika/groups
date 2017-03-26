/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl.converters;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;

/**
 * String to {@link InetSocketAddress} converter.
 * 
 * @author andreym
 */
public class InetSocketAddressConverter implements IShellParameterConverter
{
    static final IMessages messages = Messages.get(IMessages.class);
    
    @Override
    public Object convert(String value)
    {
        try
        {
            value = Strings.unquote(value);
            
            int pos = value.indexOf(':');
            if (pos == -1)
                throw new InvalidArgumentException(messages.inetSocketAddressConversionError(value));
            
            return new InetSocketAddress(InetAddress.getByName(value.substring(0, pos)), 
                Integer.valueOf(value.substring(pos + 1)));
        }
        catch (Exception e)
        {
            throw new InvalidArgumentException(e);
        }
    }
    
    interface IMessages
    {
        @DefaultMessage("Can not convert from ''{0}'' to network socket address value.")
        ILocalizedMessage inetSocketAddressConversionError(Object value);
    }
}