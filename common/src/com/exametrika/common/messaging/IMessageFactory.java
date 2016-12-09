/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


import java.io.File;
import java.util.List;


/**
 * The {@link IMessageFactory} is a factory for {@link IMessage}.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IMessageFactory
{
    /**
     * Creates a message with specified part and empty flags.
     *
     * @param destination message destination
     * @param part initial message part
     * @return message
     */
    IMessage create(IAddress destination, IMessagePart part);
    
    /**
     * Creates a message with specified flags and empty part.
     *
     * @param destination message destination
     * @param flags initial message flags
     * @return message
     */
    IMessage create(IAddress destination, int flags);
    
    /**
     * Creates a message with specified part and flags.
     *
     * @param destination message destination
     * @param part initial message part
     * @param flags initial message flags
     * @return message
     */
    IMessage create(IAddress destination, IMessagePart part, int flags);
    
    /**
     * Creates a message with specified part and flags.
     *
     * @param destination message destination
     * @param part initial message part
     * @param flags initial message flags
     * @param files initial message files or null if message does not contain any files
     * @return message
     */
    IMessage create(IAddress destination, IMessagePart part, int flags, List<File> files);
}
