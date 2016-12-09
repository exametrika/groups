/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

import com.exametrika.common.config.resource.ClassPathResourceLoader;
import com.exametrika.common.config.resource.FileResourceLoader;
import com.exametrika.common.config.resource.IResourceLoader;
import com.exametrika.common.config.resource.ResourceManager;




/**
 * The {@link IOs} contains different utility methods for IO.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class IOs
{
    /**
     * Closes lifecycle object.
     *
     * @param lifecycle object to stop. Can be null
     */
    public static void close(ILifecycle lifecycle)
    {
        if (lifecycle == null)
            return;
        
        try
        {   
            lifecycle.stop();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Closes closeable object.
     *
     * @param closeable object to close. Can be null
     */
    public static void close(Closeable closeable)
    {
        if (closeable == null)
            return;
        
        try
        {   
            closeable.close();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Closes socket.
     *
     * @param socket socket to close. Can be null
     */
    public static void close(Socket socket)
    {
        if (socket == null)
            return;
        
        try
        {   
            socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Closes server socket.
     *
     * @param socket server socket to close. Can be null
     */
    public static void close(ServerSocket socket)
    {
        if (socket == null)
            return;
        
        try
        {   
            socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Closes selector.
     *
     * @param selector selector to close. Can be null
     */
    public static void close(Selector selector)
    {
        if (selector == null)
            return;
        
        try
        {   
            selector.close();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Closes file lock.
     *
     * @param lock lock to close. Can be null
     */
    public static void close(FileLock lock)
    {
        if (lock == null)
            return;
        
        try
        {   
            lock.release();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Copies source stream to destination stream.
     *
     * @param source source stream
     * @param destination destination stream
     * @exception IOException if IO exception occurs
     */
    public static void copy(InputStream source, OutputStream destination) throws IOException
    {
        copy(source, destination, 8192);
    }
    
    /**
     * Copies source stream to destination stream.
     *
     * @param source source stream
     * @param destination destination stream
     * @param bufferSize size of copy buffer
     * @exception InvalidArgumentException if bufferSize < 0
     * @exception IOException if IO exception occurs
     */
    public static void copy(InputStream source, OutputStream destination, int bufferSize) throws IOException
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        if (bufferSize < 0)
            throw new InvalidArgumentException();
        
        byte[] buffer = new byte[bufferSize];
        
        while (true)
        {
            int length = source.read(buffer);
            if (length == -1)
                break;
            
            destination.write(buffer, 0, length);
        }
    }

    /**
     * Copies source reader to destination writer.
     *
     * @param source source reader
     * @param destination destination writer
     * @exception IOException if IO exception occurs
     */
    public static void copy(Reader source, Writer destination) throws IOException
    {
        copy(source, destination, 8192);
    }
    
    /**
     * Copies source reader to destination writer.
     *
     * @param source source reader
     * @param destination destination writer
     * @param bufferSize size of copy buffer
     * @exception InvalidArgumentException if bufferSize < 0
     * @exception IOException if IO exception occurs
     */
    public static void copy(Reader source, Writer destination, int bufferSize) throws IOException
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        if (bufferSize < 0)
            throw new InvalidArgumentException();
        
        char[] buffer = new char[bufferSize];
        
        while (true)
        {
            int length = source.read(buffer);
            if (length == -1)
                break;
            
            destination.write(buffer, 0, length);
        }
    }
    
    /**
     * Does source stream equal to destination stream?
     *
     * @param source source stream
     * @param destination destination stream
     * @return true if source stream equals to destination stream
     * @exception IOException if IO exception occurs
     */
    public static boolean equals(InputStream source, InputStream destination) throws IOException
    {
        return equals(source, destination, 8192);
    }
    
    /**
     * Does source stream equal to destination stream?
     *
     * @param source source stream
     * @param destination destination stream
     * @param bufferSize size of compare buffer
     * @return true if source stream equals to destination stream
     * @exception InvalidArgumentException if bufferSize < 0
     * @exception IOException if IO exception occurs
     */
    public static boolean equals(InputStream source, InputStream destination, int bufferSize) throws IOException
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        if (bufferSize < 0)
            throw new InvalidArgumentException();
        
        byte[] sourceBuffer = new byte[bufferSize];
        byte[] destinationBuffer = new byte[bufferSize];
        
        while (true)
        {
            int sourceLength = source.read(sourceBuffer);
            int destinationLength = destination.read(destinationBuffer);
            if (sourceLength == destinationLength && destinationLength == -1)
                return true;
            
            if (sourceLength != destinationLength)
                return false;
            
            for (int i = 0; i < sourceLength; i++)
            {
                if (sourceBuffer[i] != destinationBuffer[i])
                    return false;
            }
        }
    }
    
    /**
     * Reads data from source buffer and writes it to destination. Length of read and written data is a minimum of
     * remaining space of source and destination. Increments positions of source and destination buffer to length.
     *
     * @param source source buffer
     * @param destination destination buffer
     * @return number of bytes read and written
     */
    public static int put(ByteBuffer source, ByteBuffer destination)
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        
        int length = source.remaining();

        if (length > destination.remaining())
            length = destination.remaining();
        
        if (length == 0)
            return 0;

        if (source.hasArray())
        {
            destination.put(source.array(), source.arrayOffset() + source.position(),
                length);
            source.position(source.position() + length);            
        }
        else
        {
            for (int i = 0; i < length; i++)
                destination.put(source.get());
        }
        
        return length;
    }
    
    /**
     * Reads contents of text input stream into string.
     *
     * @param stream stream
     * @param charset stream charset
     * @return stream contents
     * @exception IOException if IO exception occurs
     */
    public static String read(InputStream stream, String charset) throws IOException
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(stream, charset));
            StringBuilder builder = new StringBuilder();
            String lineSeparator = System.getProperty("line.separator");
            boolean first = true;
            while (true)
            {
                String str = reader.readLine();
                if (str == null)
                    break;
                
                if (first)
                    first = false;
                else
                    builder.append(lineSeparator);
                
                builder.append(str);
            }
            
            return builder.toString();
        }
        finally
        {
            close(reader);
            close(stream);
        }
    }
    
    /**
     * Writes contents of string into output stream.
     *
     * @param stream stream
     * @param value value
     * @param charset stream charset
     * @exception IOException if IO exception occurs
     */
    public static void write(OutputStream stream, String value, String charset) throws IOException
    {
        BufferedWriter writer = null;

        try
        {
            writer = new BufferedWriter(new OutputStreamWriter(stream, charset));
            writer.write(value);
        }
        finally
        {
            close(writer);
            close(stream);
        }
    }
    
    /**
     * Loads contents of resource into string.
     *
     * @param resourceLocation resource location
     * @param charset charset
     * @return resource contents
     * @throws IOException
     */
    public static String load(String resourceLocation, String charset) throws IOException
    {
        Map<String, IResourceLoader> resourceLoaders = new HashMap<String, IResourceLoader>();
        resourceLoaders.put(FileResourceLoader.SCHEMA, new FileResourceLoader());
        resourceLoaders.put(ClassPathResourceLoader.SCHEMA, new ClassPathResourceLoader());
        ResourceManager resourceManager = new ResourceManager(resourceLoaders, "file");
        InputStream stream = null;
        try
        {
            stream = resourceManager.getResource(resourceLocation);
            return read(stream, charset);
        }
        catch (Exception e)
        {
            return Exceptions.wrapAndThrow(e);
        }
        finally
        {
            IOs.close(stream);
        }
    }
    
    private IOs()
    {
    }
}
