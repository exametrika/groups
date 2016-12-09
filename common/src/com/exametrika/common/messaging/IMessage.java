/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


import java.io.File;
import java.util.List;

/**
 * The {@link IMessage} is a message.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IMessage
{
    /**
     * Returns source of message.
     *
     * @return message source
     */
    IAddress getSource();

    /**
     * Returns destination of message.
     *
     * @return message destination
     */
    IAddress getDestination();
    
    /**
     * Returns estimated size of message.
     *
     * @return estimated size of message
     */
    int getSize();

    /**
     * Returns message flags.
     *
     * @return message flags
     */
    int getFlags();

    /**
     * Returns true if message contains specified flags.
     *
     * @param flags flags to check
     * @return true if message contains specified flags
     */
    boolean hasFlags(int flags);
    
    /**
     * Returns true if message contains at least one of specified flags.
     *
     * @param flags flags to check
     * @return true if message contains at least one of specified flags
     */
    boolean hasOneOfFlags(int flags);
    
    /**
     * Returns current message part.
     *
     * @param <T> part type
     * @return message part
     */
    <T extends IMessagePart> T getPart();
    
    /**
     * Returns list of files this message contains. Receiver must delete that files after processing. Sender must send
     * temporary files because files are deleted automatically after send has been completed.
     *
     * @return list of files or null if message does not contain any files
     */
    List<File> getFiles();
    
    /**
     * Creates a message with all contents of current message including specified flags.
     *
     * @param flags flags included with current message
     * @return message with all contents of current message including specified flags
     */
    IMessage addFlags(int flags);

    /**
     * Creates a message with all contents of current message and specified message part.
     *
     * @param part message part to add
     * @return message with all contents of current message and specified message part
     */
    IMessage addPart(IMessagePart part);
    
    /**
     * Creates a message with all contents of current message and specified message part.
     *
     * @param part message part to add
     * @param serialize if true - all previous parts are converted to serialized form
     * @return message with all contents of current message and specified message part
     */
    IMessage addPart(IMessagePart part, boolean serialize);
    
    /**
     * Creates a message with all contents of current message, specified message part and flags.
     *
     * @param part message part to add
     * @param flags flags included with current message
     * @return message with all contents of current message, specified message part and flags
     */
    IMessage addPart(IMessagePart part, int flags);
    
    /**
     * Creates a message with all contents of current message, specified message part and flags.
     *
     * @param part message part to add
     * @param flags flags included with current message
     * @param serialize if true - all previous parts are converted to serialized form
     * @return message with all contents of current message, specified message part and flags
     */
    IMessage addPart(IMessagePart part, int flags, boolean serialize);
    
    /**
     * Creates a message with all contents of current message excluding specified flags.
     *
     * @param flags flags excluded from current message
     * @return message with all contents of current message excluding specified flags
     */
    IMessage removeFlags(int flags);
    
    /**
     * Creates a message with all contents of current message excluding current message part.
     * If previous message part (if any) is in serialized form it's deserialized. 
     *
     * @return message with all contents of current message excluding current message part 
     *     If current message does not have any parts, returns <cc>null<cc>
     */
    IMessage removePart();
    
    /**
     * Creates a message with all contents of current message excluding current message part and specified flags.
     * If previous message part (if any) is in serialized form it's deserialized. 
     *
     * @param flags flags excluded from current message
     * @return message with all contents of current message excluding current message part and specified flags 
     *     If current message does not have any parts, returns <cc>null<cc>
     */
    IMessage removePart(int flags);

    /**
     * Creates a message with all contants of current message including specified files.
     *
     * @param files list of files or null if message does not contain any files
     * @return message with all contents of current message including specified files
     */
    IMessage setFiles(List<File> files);
    
    /**
     * Retargets message by setting its destination.
     *
     * @param destination new destination
     * @return retargeted message
     */
    IMessage retarget(IAddress destination);
}
