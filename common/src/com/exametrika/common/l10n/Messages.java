/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import com.exametrika.common.l10n.impl.MessageBundle;
import com.exametrika.common.utils.Assert;



/**
 * The {@link Messages} is a helper class for creating localized messages.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class Messages implements InvocationHandler
{
    private final MessageBundle messageBundle;
    
    /**
     * Creates localized messages for specified localization interface. Interface methods must be marked as {@link DefaultMessage}.
     *
     * @param <T> messages type
     * @param localizationClass localization interface
     * @return localized messages
     */
    public static <T> T get(Class<T> localizationClass)
    {
        return get(localizationClass, getTopLevelClass(localizationClass).getName(), localizationClass.getClassLoader());
    }
    
    /**
     * Creates localized messages for specified localization interface. Interface methods must be marked as {@link DefaultMessage}.
     *
     * @param <T> messages type
     * @param localizationClass localization interface
     * @param resourceBundleName name of resource bundle
     * @param classLoader class loader to define proxy and load resource bundle
     * @return localized messages
     */
    public static <T> T get(Class<T> localizationClass, String resourceBundleName, ClassLoader classLoader)
    {
        Assert.notNull(localizationClass);
        Assert.notNull(resourceBundleName);
        Assert.notNull(classLoader);
        checkClass(localizationClass);
        
        return (T)Proxy.newProxyInstance(classLoader, new Class<?>[]{ localizationClass }, new Messages(resourceBundleName, classLoader));
    }
    
    public static ILocalizedMessage nonLocalized(Object message)
    {
        return new NonLocalizedMessage(message != null ? message.toString() : "");
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        DefaultMessage message = method.getAnnotation(DefaultMessage.class);
        
        String id;
        if (message == null || message.id().isEmpty())
            id = method.getName();
        else
            id = message.id();
        
        return new Message(messageBundle, id, message != null ? message.appendId() : false, 
            message != null ? message.value() : null, args);
    }
    
    private Messages(String name, ClassLoader classLoader)
    {
        Assert.notNull(name);
        Assert.notNull(classLoader);
        
        this.messageBundle = new MessageBundle(name, classLoader);
    }

    private static Class<?> getTopLevelClass(Class<?> clazz)
    {
        if (clazz.getDeclaringClass() == null)
            return clazz;
        
        return getTopLevelClass(clazz.getDeclaringClass());
    }
    
    private static void checkClass(Class<?> clazz)
    {
        Assert.isTrue(clazz.isInterface(), "Specified type ''{0}'' must be an interface.", clazz);
        
        for (Method method : clazz.getMethods())
        {
            Assert.isTrue(method.getReturnType().equals(ILocalizedMessage.class), "Return type of method ''{0}'' must be ''{1}''.", 
                method, ILocalizedMessage.class);
            
            DefaultMessage message = method.getAnnotation(DefaultMessage.class);
            if (message == null || message.value().isEmpty())
                continue;
            
            String defaultString = message.value();

            boolean[] placeHolders = new boolean[100];

            boolean singleQuoteStarted = false;
            boolean parameterStarted = false;
            int parameterStartPos = -1;
            int parameterEndPos = -1;
            for (int k = 0; k < defaultString.length(); k++)
            {
                char c = defaultString.charAt(k);
                if (c == '\'')
                {
                    singleQuoteStarted = !singleQuoteStarted;
                    continue;
                }

                if (singleQuoteStarted)
                    continue;

                if (c == '{' && !parameterStarted)
                {
                    parameterStarted = true;
                    parameterStartPos = k + 1;
                    continue;
                }
                
                if (parameterStarted && parameterEndPos == -1
                    && !Character.isDigit(c))
                {
                    parameterEndPos = k - 1;

                    if (parameterEndPos - parameterStartPos >= 0)
                    {
                        int parameterIndex = Integer.valueOf(defaultString.substring(parameterStartPos, parameterEndPos + 1));

                        if (parameterIndex < placeHolders.length)
                            placeHolders[parameterIndex] = true;
                    }
                }

                if (c == '}')
                {
                    parameterStarted = false;
                    parameterEndPos = -1;
                }
            }

            Class<?>[] parameters = method.getParameterTypes();

            for (int k = 0; k < parameters.length; k++)
                Assert.isTrue(placeHolders[k], "Parameter ''{1}'' of method ''{0}'' is not used.", method, k);

            List<Integer> unboundPlaceholders = new ArrayList<Integer>();
            for (int k = 0; k < placeHolders.length; k++)
            {
                if (placeHolders[k] && k >= parameters.length)
                    unboundPlaceholders.add(k);
            }

            Assert.isTrue(unboundPlaceholders.isEmpty(), "Placeholders ''{0}'' are not bound to method ''{1}''.", 
                unboundPlaceholders, method);
        }
    }
    
    private static class Message implements ILocalizedMessage
    {
        private final MessageBundle messageBundle;
        private final String id;
        private final boolean appendId;
        private final String defaultValue;
        private final Object[] args;
        
        public Message(MessageBundle messageBundle, String id, boolean appendId, String defaultValue, Object[] args)
        {
            this.messageBundle = messageBundle;
            this.id = id;
            this.appendId = appendId;
            this.defaultValue = defaultValue;
            this.args = args;
        }
    
        @Override
        public String getString()
        {
            return getString(Locale.getDefault());
        }

        @Override
        public String getString(Locale locale)
        {
            String messageTemplate = messageBundle.findMessage(id, locale);
            if (messageTemplate == null)
            {
                if (defaultValue != null && !defaultValue.isEmpty())
                    messageTemplate = defaultValue;
                else
                    throw new MissingResourceException(MessageFormat.format(
                        "Message with id ''{0}'' is not found in message bundle ''{1}''.",
                        id, messageBundle.getId()), messageBundle.getId(), id);
            }
            
            return (appendId ? (id + " - ") : "") + MessageFormat.format(messageTemplate, args);
        }
        
        @Override
        public String toString()
        {
            return getString();
        }
    }
}
