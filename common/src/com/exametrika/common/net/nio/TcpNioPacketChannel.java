/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.json.Json;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.ITcpChannelHandshaker;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.ITcpPacketSerializer;
import com.exametrika.common.net.TcpChannelException;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.net.nio.socket.ITcpSocketChannel;
import com.exametrika.common.net.nio.socket.TcpSocketChannel;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Strings;



/**
 * The {@link TcpNioPacketChannel} is a NIO implementaion of {@link ITcpPacketChannel}.
 * 
 * @param <T> packet type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpNioPacketChannel<T> extends TcpNioAbstractChannel implements ITcpPacketChannel<T>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TcpNioPacketChannel.class);
    private static final short MAGIC_HEADER = 0x1717;
    private static final short VERSION = 0x1;
    private final int maxPacketSize;
    private final ITcpChannelHandshaker<T> channelHandshaker;
    private final ITcpPacketSerializer<T> packetSerializer;
    private final boolean disableFileDeletion;
    private volatile TcpPacket currentWritePacket;
    private volatile PacketInfo writeInfo;
    private T currentReadPacket;
    private final ByteBuffer writeHeaderBuffer = ByteBuffer.allocate(20);//magic_header(short) + version(short) + sequence(long) + size(int) + filesCount(int)
    private final ByteBuffer readHeaderBuffer = ByteBuffer.allocate(20);//magic_header(short) + version(short) + sequence(long) + size(int) + filesCount(int)
    private PacketInfo readInfo;
    private long readSequenceNumber;
    private long writeSequenceNumber;
    
    public TcpNioPacketChannel(InetSocketAddress remoteAddress, InetAddress bindAddress, ITcpPacketChannel.Parameters parameters, 
        TcpNioDispatcher dispatcher)
    {
        super(remoteAddress, bindAddress, parameters, dispatcher);
        
        this.maxPacketSize = parameters.maxPacketSize;
        this.packetSerializer = parameters.packetSerializer;
        this.channelHandshaker = parameters.channelHandshaker;
        this.disableFileDeletion = parameters.disableFileDeletion;
    }
    
    public TcpNioPacketChannel(ITcpPacketChannel.Parameters parameters, ITcpSocketChannel socketChannel, TcpNioDispatcher dispatcher)
    {
        super(parameters, socketChannel, dispatcher);
        
        this.maxPacketSize = parameters.maxPacketSize;
        this.packetSerializer = parameters.packetSerializer;
        this.channelHandshaker = parameters.channelHandshaker;
        this.disableFileDeletion = parameters.disableFileDeletion;
    }
    
    @Override
    public T read()
    {
        if (currentReadPacket != null)
        {
            T packet = currentReadPacket;
            currentReadPacket = null;
            
            return packet;
        }

        return null;
    }
    
    @Override
    public boolean write(T packet)
    {
        Assert.notNull(packet);
        
        if (currentWritePacket != null)
            return false;
        
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, getMarker(), messages.packetWritten(Strings.wrap(packet.toString(), 4, 120)));

        TcpPacket buffer;
        if (packetSerializer != null)
            buffer = packetSerializer.serialize(packet);
        else
            buffer = (TcpPacket)packet;
        
        Assert.isTrue(buffer.getSize() >= 0 && (maxPacketSize == 0 || buffer.getSize() <= maxPacketSize));
        
        currentWritePacket = buffer;
        return true;
    }
    
    @Override
    public boolean hasReadData()
    {
        return currentReadPacket != null || super.hasReadData();
    }

    @Override
    public void dump(Json json)
    {
        super.dump(json);
        
        json.put("readSequence", readSequenceNumber)
          .put("writeSequence", writeSequenceNumber)
          .put("canWrite", canWrite())
          .put("writePacket", currentWritePacket != null)
          .put("writeInfo", writeInfo != null)
          .put("super.canWrite", super.canWrite());
    }

    @Override
    protected boolean canWrite()
    {
        if (isConnected() && rateController != null && !rateController.canWrite())
            return false;
                    
        if (currentWritePacket != null || writeInfo != null)
            return true;
        
        return super.canWrite();
    }

    @Override
    protected void doRead()
    {
        if (currentReadPacket != null)
        {
            super.doRead();
            
            if (currentReadPacket != null)
                return;
        }

        try
        {
            if (readInfo == null && !readHeader())
                return;
        
            Assert.notNull(readInfo);
            
            long n = 0;
            if (readInfo.buffers[readInfo.buffers.length - 1].hasRemaining())
            {
                n = socketChannel.read(readInfo.buffers);
                if (n == -1)
                {
                    if (readInfo.buffers[readInfo.buffers.length - 1].limit() == 0 && readInfo.filesCount == 0)
                        disconnect();
                    else
                        close();
                    return;
                }
            }
            else if (readInfo.fileIndex < readInfo.filesCount)
            {
                if (readInfo.files == null)
                {
                    readInfo.files = new RandomAccessFile[readInfo.filesCount];
                    readInfo.fileNames = new File[readInfo.filesCount];
                    readInfo.fileSizes = new long[readInfo.filesCount];
                    readInfo.buffers[0].flip();
                    for (int i = 0; i < readInfo.filesCount; i++)
                        readInfo.fileSizes[i] = readInfo.buffers[0].getLong();
                }
                
                RandomAccessFile file;
                if (readInfo.files[readInfo.fileIndex] == null)
                {
                    File fileName = File.createTempFile("exa", ".tmp");
                    file = new RandomAccessFile(fileName, "rw");
                    readInfo.fileNames[readInfo.fileIndex] = fileName;
                    readInfo.files[readInfo.fileIndex] = file;
                }
                else
                    file = readInfo.files[readInfo.fileIndex];
                
                if (socketChannel.getClass() == TcpSocketChannel.class)
                    n = file.getChannel().transferFrom(((TcpSocketChannel)socketChannel).getChannel(), readInfo.filePos, 
                        readInfo.fileSizes[readInfo.fileIndex] - readInfo.filePos);
                else
                {
                    if (readInfo.fileBuffer == null)
                    {
                        readInfo.fileBuffer = ByteBuffer.allocate(0x4000);
                        readInfo.fileBuffer.flip();
                    }
                    
                    if (!readInfo.fileBuffer.hasRemaining())
                    {
                        int remaining = (int)(readInfo.fileSizes[readInfo.fileIndex] - readInfo.filePos);
                        if (remaining > readInfo.fileBuffer.capacity())
                            readInfo.fileBuffer.limit(readInfo.fileBuffer.capacity());
                        else
                            readInfo.fileBuffer.limit(remaining);
                        readInfo.fileBuffer.position(0);
                        if (socketChannel.read(readInfo.fileBuffer) == -1)
                        {
                            close();
                            return;
                        }
                        Assert.checkState(n != -1);
                        readInfo.fileBuffer.flip();
                    }
                    
                    n = file.getChannel().write(readInfo.fileBuffer);
                }
                
                readInfo.filePos += n;
                if (readInfo.filePos == readInfo.fileSizes[readInfo.fileIndex])
                {
                    readInfo.filePos = 0;
                    readInfo.fileIndex++;
                }
            }
            
            if (n > 0)
            {
                
                if (rateController != null)
                    rateController.incrementReadCount(n);

                lastReadTime = dispatcher.getCurrentTime();
            }
            
            if (!readInfo.buffers[readInfo.buffers.length - 1].hasRemaining() && 
                readInfo.fileIndex == readInfo.filesCount)
            {
                List<File> files = null;
                if (readInfo.fileNames != null)
                {
                    readInfo.close(false);
                    
                    files = new ArrayList<File>(readInfo.fileNames.length);
                    for (int i = 0; i < readInfo.fileNames.length; i++)
                        files.add(readInfo.fileNames[i]);
                }
                
                ByteBuffer readBuffer = readInfo.buffers[readInfo.buffers.length - 1];
                readBuffer.flip();
                
                TcpPacket packet = new TcpPacket(Collections.singletonList(new ByteArray(readBuffer.array(), readBuffer.arrayOffset(),
                    readBuffer.limit())), files, null);
                
                if (packetSerializer != null)
                    currentReadPacket = packetSerializer.deserialize(packet);
                else
                    currentReadPacket = (T)packet;
                
                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, getMarker(), messages.packetRead(Strings.wrap(currentReadPacket.toString(), 4, 120)));
                
                readInfo = null;
                
                if (channelHandshaker != null && channelHandshaker.canDisconnect(currentReadPacket))
                    disconnect();
                else 
                    super.doRead();
            }
        }
        catch (IOException e)
        {
            close();
            throw new TcpChannelException(this, e);
        }
    }

    @Override
    protected void doWrite()
    {
        if (!socketChannel.isConnected())
            return;
        
        if (currentWritePacket == null && writeInfo == null)
        {
            super.doWrite();
            
            if (currentWritePacket == null)
            {
                updateWriteStatus();
                return;
            }
        }
        
        try
        {
            if (writeInfo == null && !writeHeader(currentWritePacket))
                return;

            Assert.notNull(writeInfo);
            
            long n = 0;
            if (writeInfo.buffers[writeInfo.buffers.length - 1].hasRemaining())
                n = socketChannel.write(writeInfo.buffers);
            else if (writeInfo.files != null && writeInfo.fileIndex < writeInfo.files.length)
            {
                RandomAccessFile file = writeInfo.files[writeInfo.fileIndex];
                
                if (socketChannel.getClass() == TcpSocketChannel.class)
                    n = file.getChannel().transferTo(writeInfo.filePos, writeInfo.fileSizes[writeInfo.fileIndex] - writeInfo.filePos, 
                        ((TcpSocketChannel)socketChannel).getChannel());
                else
                {
                    if (writeInfo.fileBuffer == null)
                    {
                        writeInfo.fileBuffer = ByteBuffer.allocate(0x4000);
                        writeInfo.fileBuffer.flip();
                    }
                    
                    if (!writeInfo.fileBuffer.hasRemaining())
                    {
                        writeInfo.fileBuffer.limit(writeInfo.fileBuffer.capacity());
                        writeInfo.fileBuffer.position(0);
                        Assert.checkState(file.getChannel().read(writeInfo.fileBuffer) != -1);
                        writeInfo.fileBuffer.flip();
                    }
                    
                    n = socketChannel.write(writeInfo.fileBuffer);
                }
                
                writeInfo.filePos += n;
                if (writeInfo.filePos == writeInfo.fileSizes[writeInfo.fileIndex])
                {
                    writeInfo.filePos = 0;
                    writeInfo.fileIndex++;
                }
            }
            else if (socketChannel.flush())
            {
                writeInfo.close(!disableFileDeletion);
                writeInfo = null;
            }
            
            if (n > 0)
            {
                if (rateController != null)
                    rateController.incrementWriteCount(n);
                
                lastWriteTime = dispatcher.getCurrentTime();
            }
        }
        catch (IOException e)
        {
            close();
            throw new TcpChannelException(this, e);
        }
    }
    
    @Override
    protected void doClose()
    {
        super.doClose();
        
        currentReadPacket = null;
        currentWritePacket = null;
        if (readInfo != null)
            readInfo.close(true);
        readInfo = null;
        if (writeInfo != null)
            writeInfo.close(true);
        writeInfo = null;
        readHeaderBuffer.rewind();
        writeHeaderBuffer.rewind();
    }
    
    @Override
    protected boolean doHandshake()
    {
        return channelHandshaker != null ? channelHandshaker.handshake(this) : true;
    }
    
    @Override
    protected boolean doDisconnect()
    {
        return channelHandshaker != null ? channelHandshaker.disconnect(this) : true;
    }

    @Override
    protected boolean flush()
    {
        if (currentWritePacket != null || writeInfo != null)
            doWrite();
        
        return currentWritePacket == null && writeInfo == null;
    }

    private boolean readHeader() throws IOException
    {
        int count = socketChannel.read(readHeaderBuffer);
        if (count == -1)
        {
            disconnect();
            return false;
        }
        
        if (readHeaderBuffer.hasRemaining())
            return false;

        if (rateController != null)
            rateController.incrementReadCount(readHeaderBuffer.limit());
        
        lastReadTime = dispatcher.getCurrentTime();
        
        readHeaderBuffer.flip();
        short magicHeader = readHeaderBuffer.getShort();
        if (magicHeader != MAGIC_HEADER)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, getMarker(), messages.invalidMessageFormat());
            
            close();
            return false;
        }
        
        short version = readHeaderBuffer.getShort();
        if (version != VERSION)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, getMarker(), messages.invalidMessageVersion());
            
            close();
            return false;
        }
        
        long sequenceNumber = readHeaderBuffer.getLong();
        if (sequenceNumber != readSequenceNumber)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, getMarker(), messages.sequenceOutOfOrder());
            
            close();
            return false;
        }
        
        readSequenceNumber++;
        
        int size = readHeaderBuffer.getInt();
        if ((maxPacketSize > 0 && size > maxPacketSize))
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, getMarker(), messages.tooLargeMessage());
            
            close();
            return false;
        }

        readInfo = new PacketInfo();
        readInfo.filesCount = readHeaderBuffer.getInt();
        
        if (readInfo.filesCount > 0)
        {
            readInfo.buffers = new ByteBuffer[2];
            readInfo.buffers[0] = ByteBuffer.allocate(readInfo.filesCount * 8);
            readInfo.buffers[1] = ByteBuffer.allocate(size);
        }
        else
        {
            readInfo.buffers = new ByteBuffer[1];
            readInfo.buffers[0] = ByteBuffer.allocate(size);
        }
        
        readHeaderBuffer.rewind();
        
        return true;
    }

    private boolean writeHeader(TcpPacket packet) throws IOException
    {
        Assert.notNull(currentWritePacket);
        
        if (writeHeaderBuffer.position() == 0)
        {
            writeHeaderBuffer.putShort(MAGIC_HEADER);
            writeHeaderBuffer.putShort(VERSION);
            writeHeaderBuffer.putLong(writeSequenceNumber++);
            writeHeaderBuffer.putInt(packet.getSize());
            
            if (packet.getFiles() != null)
                writeHeaderBuffer.putInt(packet.getFiles().size());
            else
                writeHeaderBuffer.putInt(0);
            
            writeHeaderBuffer.flip();
        }
        
        socketChannel.write(writeHeaderBuffer);
        
        if (writeHeaderBuffer.hasRemaining())
            return false;
        
        if (rateController != null)
            rateController.incrementWriteCount(writeHeaderBuffer.limit());
        
        lastWriteTime = dispatcher.getCurrentTime();
        
        PacketInfo info = new PacketInfo();
        List<File> fileNames = packet.getFiles();
        
        if (fileNames != null)
        {
            int size = fileNames.size();
            ByteBuffer filesHeader = ByteBuffer.allocate(size * 8);
            info.fileNames = fileNames.toArray(new File[size]);
            info.files = new RandomAccessFile[size];
            info.fileSizes = new long[size];
            for (int i = 0; i < size; i++)
            {
                info.files[i] = new RandomAccessFile(fileNames.get(i), "r");
                info.fileSizes[i] = info.files[i].length(); 
                filesHeader.putLong(info.fileSizes[i]);
            }
            
            filesHeader.flip();
            
            info.buffers = new ByteBuffer[1 + packet.getBuffers().size()];
            info.buffers[0] = filesHeader;
            
            for (int i = 0; i < packet.getBuffers().size(); i++)
            {
                ByteArray buffer = packet.getBuffers().get(i);
                info.buffers[1 + i] = ByteBuffer.wrap(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            }
        }
        else
        {
            info.buffers = new ByteBuffer[packet.getBuffers().size()];
            
            for (int i = 0; i < packet.getBuffers().size(); i++)
            {
                ByteArray buffer = packet.getBuffers().get(i);
                info.buffers[i] = ByteBuffer.wrap(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            }
        }
        
        currentWritePacket = null;
        writeHeaderBuffer.rewind();
        writeInfo = info;
        return true;
    }

    private static class PacketInfo
    {
        private ByteBuffer[] buffers;
        private int filesCount;
        private File[] fileNames;
        private RandomAccessFile[] files;
        private long[] fileSizes;
        private int fileIndex;
        private long filePos;
        private ByteBuffer fileBuffer;
        
        public void close(boolean deleteFiles)
        {
            if (files != null)
            {
                for (int i = 0; i < files.length; i++)
                {
                    IOs.close(files[i]);
                    
                    if (deleteFiles)
                        fileNames[i].delete();
                }
            }
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Invalid message has been received from channel.")
        ILocalizedMessage invalidMessageFormat();
        @DefaultMessage("Message with unsupported version has been received from channel.")
        ILocalizedMessage invalidMessageVersion();
        @DefaultMessage("Too large message has been received from channel.")
        ILocalizedMessage tooLargeMessage();
        @DefaultMessage("Packet has been read:\n{0}")
        ILocalizedMessage packetRead(String packet);
        @DefaultMessage("Packet has been written:\n{0}")
        ILocalizedMessage packetWritten(String packet);
        @DefaultMessage("Read sequence number out of order.")
        ILocalizedMessage sequenceOutOfOrder();
    }
}
