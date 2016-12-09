/**
 * Copyright 2009 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;
import java.util.MissingResourceException;

/**
 * The {@link DefaultMessage} indicates that interface method represents a localized string. Method name or identifier specified
 * in this annotation is used as message identifier in resource bundle, method arguments are used as localized message arguments.
 * Method must have return value of type {@link ILocalizedMessage}.
 * 
 * @author Medvedev-A
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DefaultMessage
{
    /** Identifier of localized message. Method name is used if not specified. */
    String id() default "";
    
    /** If true identifier of message will be appended as prefix to resulting message. */
    boolean appendId() default false;
    
    /** Default value of localized message. {@link MissingResourceException} is thrown if default value is not specified 
     * or message is not found in resource bundle. Default value can have format as specified in {@link MessageFormat}.
     */
    String value() default "";
}
