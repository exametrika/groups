/**
 * Copyright 2009 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.l10n;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * Defines typed localizable messages.
 * 
 * @author Medvedev-A
 */
public interface ITypedMessages
{
    @DefaultMessage("This is a message1.")
    ILocalizedMessage message1();
    
    @DefaultMessage("This is a message2 with ''{0}'' and ''{1}''.")
    ILocalizedMessage message2(int param1, String param2);
    
    @DefaultMessage("This is a message3.")
    ILocalizedMessage message3();
    
    @DefaultMessage(id = "message4(param1,param2)", value = "This is a message4 with ''{0}'' and ''{1}''.")
    ILocalizedMessage message4(int param1, String param2);
    
    @DefaultMessage(id = "message5(param1,param2)")
    ILocalizedMessage message5(int param1, String param2);
    
    ILocalizedMessage message6(int param1, String param2);
    
    ILocalizedMessage unknownMessage();
}
