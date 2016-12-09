/**
 * Copyright 2009 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import com.exametrika.common.utils.Classes;


/**
 * The {@link JdkDeserializationInputStream} is an extension of {@link ObjectInputStream} that integrated with serialization 
 * framework and uses specified classloader to load classes of deserialized objects.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JdkDeserializationInputStream extends ObjectInputStream
{
    private final ClassLoader loader;
    
    /**
     * Creates a new object.
     *
     * @param stream input stream
     * @param loader class loader to load classes from. Can be null
     * @exception IOException if some IO exception occured
     */
    public JdkDeserializationInputStream(InputStream stream, ClassLoader loader) throws IOException
    {
        super(stream);
        
        this.loader = loader;
    }

    @Override
    protected Class resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException
    {
        return Classes.loadClass(classDesc.getName(), loader);
    }
}
