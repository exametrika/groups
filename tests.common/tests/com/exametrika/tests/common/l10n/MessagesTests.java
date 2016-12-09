/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.l10n;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Locale;
import java.util.MissingResourceException;

import org.junit.Test;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.tests.Expected;


/**
 * The {@link MessagesTests} are tests for {@link Messages} class.
 * 
 * @see Messages
 * @author Medvedev_A
 */
public class MessagesTests
{
    @Test
    public void testMessages() throws Throwable
    {
        final Locale locale = new Locale("fr");
        final ITypedMessages messages = Messages.get(ITypedMessages.class);
        ILocalizedMessage message1 = messages.message1();
        ILocalizedMessage message2 = messages.message2(10, "test");
        ILocalizedMessage message3 = messages.message3();
        ILocalizedMessage message4 = messages.message4(10, "test");
        final ILocalizedMessage message5 = messages.message5(10, "test");
        final ILocalizedMessage message6 = messages.message6(10, "test");
        final ILocalizedMessage unknownMessage = messages.unknownMessage();
        
        assertThat(message1.getString(locale), is("This is a message1."));
        assertThat(message2.getString(locale), is("This is a message2 with '10' and 'test'."));
        assertThat(message3.getString(locale), is("This is a message3."));
        assertThat(message4.getString(locale), is("This is a message4 with '10' and 'test'."));
        assertThat(message6.getString(locale), is("This is a message6 with '10' and 'test'."));
        
        new Expected(MissingResourceException.class, new Runnable()
        {
            @Override
            public void run()
            {
                unknownMessage.getString(locale);
            }
        });
        
        new Expected(MissingResourceException.class, new Runnable()
        {
            @Override
            public void run()
            {
                message5.getString(locale);
            }
        });
        
        Locale locale2 = new Locale("ru");
        
        assertThat(message1.getString(locale2), is("ru Локализованное сообщение1."));
        assertThat(message2.getString(locale2), is("ru Локализованное сообщение2 с '10' и 'test'."));
        assertThat(message3.getString(locale2), is("This is a message3."));
        assertThat(message4.getString(locale2), is("ru Локализованное сообщение4 с '10' и 'test'."));
        assertThat(message5.getString(locale2), is("ru Локализованное сообщение5 с '10' и 'test'."));
        assertThat(message6.getString(locale2), is("This is a message6 with '10' and 'test'."));
        
        locale2 = new Locale("ru", "RU");
        
        assertThat(message1.getString(locale2), is("ru_RU Локализованное сообщение1."));
        assertThat(message2.getString(locale2), is("ru_RU Локализованное сообщение2 с '10' и 'test'."));
        assertThat(message3.getString(locale2), is("This is a message3."));
        assertThat(message4.getString(locale2), is("ru_RU Локализованное сообщение4 с '10' и 'test'."));
        assertThat(message5.getString(locale2), is("ru_RU Локализованное сообщение5 с '10' и 'test'."));
        assertThat(message6.getString(locale2), is("This is a message6 with '10' and 'test'."));
        
        locale2 = new Locale("ru", "RU", "Win");
        
        assertThat(message1.getString(locale2), is("ru_RU_Win Локализованное сообщение1."));
        assertThat(message2.getString(locale2), is("ru_RU_Win Локализованное сообщение2 с '10' и 'test'."));
        assertThat(message3.getString(locale2), is("This is a message3."));
        assertThat(message4.getString(locale2), is("ru_RU_Win Локализованное сообщение4 с '10' и 'test'."));
        assertThat(message5.getString(locale2), is("ru_RU_Win Локализованное сообщение5 с '10' и 'test'."));
        assertThat(message6.getString(locale2), is("This is a message6 with '10' and 'test'."));
        
        locale2 = new Locale("en", "US");
        
        assertThat(message1.getString(locale2), is("en_US Локализованное сообщение1."));
        assertThat(message2.getString(locale2), is("en_US Локализованное сообщение2 с '10' и 'test'."));
        assertThat(message3.getString(locale2), is("This is a message3."));
        assertThat(message4.getString(locale2), is("en_US Локализованное сообщение4 с '10' и 'test'."));
        assertThat(message5.getString(locale2), is("en_US Локализованное сообщение5 с '10' и 'test'."));
        assertThat(message6.getString(locale2), is("This is a message6 with '10' and 'test'."));
        
        final INestedMessages messages2 = Messages.get(INestedMessages.class);
        message1 = messages2.message1();
        
        assertThat(message1.getString(new Locale("fr")), is("message1 - This is a message1."));
        assertThat(message1.getString(new Locale("ru")), is("message1 - ru Локализованное сообщение1."));
        
        final INestedMessages.INestedNestedMessages messages3 = Messages.get(INestedMessages.INestedNestedMessages.class);
        message1 = messages3.message1();
        
        assertThat(message1.getString(new Locale("fr")), is("This is a message1."));
        assertThat(message1.getString(new Locale("ru")), is("ru Локализованное сообщение1."));
        
        final INestedMessages messages4 = Messages.get(INestedMessages.class, ITypedMessages.class.getName(), ITypedMessages.class.getClassLoader());
        message1 = messages4.message1();
        
        assertThat(message1.getString(new Locale("fr")), is("message1 - This is a message1."));
        assertThat(message1.getString(new Locale("ru")), is("message1 - ru Локализованное сообщение1."));
    }
    
    @Test
    public void testClass() throws Throwable
    {
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Messages.get(Class1.class);
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Messages.get(Interface1.class);
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Messages.get(Interface2.class);
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Messages.get(Interface3.class);
            }
        });
    }
    
    private interface INestedMessages
    {
        @DefaultMessage(value = "This is a message1.", appendId = true)
        ILocalizedMessage message1();
        
        interface INestedNestedMessages
        {
            @DefaultMessage("This is a message1.")
            ILocalizedMessage message1();
        }
    }
    
    private static class Class1
    {
    }
    
    private interface Interface1
    {
        ILocalizedMessage message();
        int message2();
    }
    
    private interface Interface2
    {
        ILocalizedMessage message();
        @DefaultMessage("{0} {2}")
        ILocalizedMessage message2(int param1, int param2, int param3);
    }
    
    private interface Interface3
    {
        ILocalizedMessage message();
        @DefaultMessage("{0} {2}")
        ILocalizedMessage message2(int param1);
    }
}
