/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IChannelListener;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.IStreamReceiveHandler;
import com.exametrika.common.messaging.IStreamSendHandler;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.FactoryParameters;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.common.net.TcpChannelTests;


/**
 * The {@link ChannelPerfTests} are performance tests for client-server {@link Channel}.
 * 
 * @see Channel
 * @author Medvedev-A
 */
public class ChannelPerfTests
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ChannelPerfTests.class);
    private static final int SMALL_COUNT = 1000000;
    private static final int MEDIUM_COUNT = 100000;
    private static final int LARGE_COUNT = 10000;
    private static final int SMALL_SIZE = 10;
    private static final int MEDIUM_SIZE = 1000;
    private static final int LARGE_SIZE = 100000;
    private static final int CONNECT_TIMEOUT = 60000;
    private static final int SEND_TIMEOUT = 600000;
    private IChannel client;
    private TestReceiver receiver;
    private TestFeed feed;
    private IChannel server;
    private volatile Sequencer connectionSequencer;
    private volatile Sequencer receiveSequencer;
    
    @Test
    public void testChannel() throws Throwable
    {
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.parameters(SMALL_COUNT, SMALL_SIZE, MEDIUM_COUNT, MEDIUM_SIZE, LARGE_COUNT, LARGE_SIZE));
        
        boolean[] b = new boolean[]{false, true};
        int i = 0;
        
        for (Type type : Type.values())
            for (boolean streamOriented : b)
                for (boolean compressed : b)
                    for (boolean pull : b)
                        for (boolean secured : b)
                            for (boolean multiThreaded : b)
                                testCaseChannel(i++, 96, type, streamOriented, compressed, pull, secured, multiThreaded, false);
  
        logger.log(LogLevel.INFO, messages.separator());
        
        i = 0;
        for (boolean pull : b)
            for (boolean secured : b)
                for (boolean multiThreaded : b)
                    for (int k = 0; k < 100; k++)
                        testCaseChannel(i++, 800, Type.SMALL, false, false, pull, secured, multiThreaded, true);
    }
    
    private void testCaseChannel(int index, int totalCount, final Type type, final boolean streamOriented,   
        final boolean compressed, final boolean pull, boolean secured, final boolean multiThreaded, boolean singleCount) throws Throwable
    {
        // Do test
        int s = 0;
        int c = 0;
        switch (type)
        {
        case SMALL:
            s = SMALL_SIZE;
            c = SMALL_COUNT;
            break;
        case MEDIUM:
            s = MEDIUM_SIZE;
            c = MEDIUM_COUNT;
            break;
        case LARGE:
            s = LARGE_SIZE;
            c = LARGE_COUNT;
            break;
        default:
            Assert.error();    
        }
        
        if (singleCount)
            c = 1;
        
        final int size = s;
        final int count = c;
        
        logger.log(LogLevel.INFO, messages.smallSeparator());
        logger.log(LogLevel.INFO, messages.testChannel(index, totalCount, type, streamOriented, compressed, pull, secured, multiThreaded));
        
        try
        {
            createChannels(secured, streamOriented, compressed, pull, multiThreaded, size, count);
            logger.log(LogLevel.INFO, messages.benchmark(new Benchmark(new Probe()
            {
                @Override
                public void runOnce()
                {
                    doTest(type, streamOriented, compressed, pull, multiThreaded, size, count);
                }
            }, 1, 0)));
        }
        finally
        {
            destroyChannels();
        }
    }

    private void createChannels(boolean secured, boolean streamOriented, boolean compressed, boolean pull, boolean multiThreaded,
        int size, int count) throws Throwable
    {
        ChannelFactory factory = new ChannelFactory(new FactoryParameters());
        ChannelFactory.Parameters parameters = new ChannelFactory.Parameters();
        
        parameters.channelName = "server";
        receiver = new TestReceiver(count, size, streamOriented, compressed);
        parameters.receiver = receiver;
        parameters.serverPart = true;
        parameters.secured = secured;
        parameters.keyStorePassword = "testtest";
        parameters.keyStorePath = "classpath:" + Classes.getResourcePath(TcpChannelTests.class) + "/keystore.jks";
        parameters.serializationRegistrars.add(new TestStreamMessagePartSerializer());
        parameters.serializationRegistrars.add(new TestMessagePartSerializer());
        parameters.multiThreaded = multiThreaded;
        
        server = factory.createChannel(parameters);
        server.getChannelObserver().addChannelListener(new TestChannelListener());
        server.start();
        
        parameters.serverPart = false;
        parameters.clientPart = true;

        parameters.receiver = new ReceiverMock();
        parameters.channelName = "client";
        client = factory.createChannel(parameters);
        client.getChannelObserver().addChannelListener(new TestChannelListener());
        client.start();

        connectionSequencer = new Sequencer();
        receiveSequencer = new Sequencer();
        
        if (pull)
        {
            feed = new TestFeed(streamOriented, compressed, size, count);
            client.register(server.getLiveNodeProvider().getLocalNode(), feed);
        }

        client.connect(server.getLiveNodeProvider().getLocalNode().getConnection());
        
        connectionSequencer.waitAll(2, CONNECT_TIMEOUT, 0, "Connection.");
    }
    
    private void destroyChannels()
    {
        if (server != null)
            server.stop();
        if (client != null)
            client.stop();
    }

    private void doTest(Type type, boolean streamOriented, boolean compressed, boolean pull, boolean multiThreaded, int size, int count)
    {
        if (!pull)
        {
            int batchCount = 1;
            if (!multiThreaded && count > 1)
            {
                switch (type)
                {
                case SMALL:
                    batchCount = 100;
                    break;
                case MEDIUM:
                    batchCount = 10;
                    break;
                case LARGE:
                    batchCount = 1;
                    break;
                default:
                    batchCount = Assert.error();
                }
            }
            
            int n = 0;
            for (int k = 0; k < count / batchCount; k++)            
            {
                List<IMessage> batch = new ArrayList<IMessage>(batchCount);
                for (int i = 0; i < batchCount; i++)
                {
                    int flags;
                    IMessagePart part;
                    
                    if (!streamOriented)
                        part = new TestMessagePart(n, createBuffer(n, size));
                    else
                        part = new TestStreamSendMessagePart(n, Collections.singletonList(createBuffer(n, size)));
                    
                    if (!compressed)
                        flags = MessageFlags.NO_COMPRESS;
                    else
                        flags = 0;
                    
                    IMessage request = client.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, 
                        flags);
                    batch.add(request);
                    
                    n++;
                }
                
                client.send(batch);
            }
        }
        
        receiveSequencer.waitAll(1, SEND_TIMEOUT, 0, "Receive messages.");
    }

    private ByteArray createBuffer(int base, int size)
    {
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; i++)
            buffer[i] = (byte)(i + base);
        
        return new ByteArray(buffer);
    }
    
    private enum Type
    {
        SMALL,
        MEDIUM,
        LARGE
    }

    private class TestReceiver implements IReceiver
    {
        public int index;
        private final int size;
        private final boolean streamOriented;
        private final boolean compressed;
        private final int count;
        
        public TestReceiver(int count, int size, boolean streamOriented, boolean compressed)
        {
            this.count = count;
            this.size = size;
            this.streamOriented = streamOriented;
            this.compressed = compressed;
        }
        
        @Override
        public synchronized void receive(IMessage message)
        {
            Assert.notNull(message);
            
            assertThat(message.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
            assertThat(message.getSource(), is(client.getLiveNodeProvider().getLocalNode()));
            
            assertThat(message.hasFlags(MessageFlags.NO_COMPRESS), is(!compressed));
            
            if (!streamOriented)
            {
                TestMessagePart part = message.getPart();
                assertThat(part, is(new TestMessagePart(index, createBuffer(index, size))));
            }
            else
            {
                TestStreamReceiveMessagePart part = message.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart(index)));
                assertThat(part.data, is(Collections.singletonList(createBuffer(index, size))));
            }

            index++;

            if (index == count)
                receiveSequencer.allowSingle("Received " + message.getSource());
        }
    }
    
    public static class TestMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("405ea4d2-f79c-4560-8aa6-59a2151c7def");
     
        public TestMessagePartSerializer()
        {
            super(ID, TestMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestMessagePart part = (TestMessagePart)object;

            serialization.writeInt(part.value);
            serialization.writeByteArray(part.buffer);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            int value = deserialization.readInt();
            ByteArray buffer = deserialization.readByteArray();
            
            return new TestMessagePart(value, buffer);
        }
    }
    
    public static class TestMessagePart implements IMessagePart
    {
        public final int value;
        private final ByteArray buffer;
        
        public TestMessagePart(int value, ByteArray buffer)
        {
            this.buffer = buffer;
            this.value = value;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof TestMessagePart))
                return true;
            
            TestMessagePart part = (TestMessagePart)o;
            return value == part.value && buffer.equals(part.buffer);
        }
        
        @Override
        public int hashCode()
        {
            return 31 * buffer.hashCode() + value;
        }
        
        @Override
        public int getSize()
        {
            return 4 + buffer.getLength();
        }
        
        @Override
        public String toString()
        {
            return Integer.toString(value);
        }
    }

    public static List<ByteArray> createBuffers(int count, int size)
    {
        List<ByteArray> buffers = new ArrayList<ByteArray>();
        for (int k = 0; k < count; k++)
        {
            byte[] buffer = new byte[size];
            for (int i = 0; i < buffer.length; i++)
                buffer[i] = (byte)(i + k);
            
            buffers.add(new ByteArray(buffer));
        }
        
        return buffers;
    }
    
    public static class TestStreamMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("568b5ef9-cd25-4c33-9bd0-33e39b82207a");
     
        public TestStreamMessagePartSerializer()
        {
            super(ID, TestStreamSendMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestStreamSendMessagePart part = (TestStreamSendMessagePart)object;

            serialization.writeInt(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            int value = deserialization.readInt();
            
            return new TestStreamReceiveMessagePart(value);
        }
    }
    
    public static class TestStreamSendMessagePart extends TestMessagePart implements IStreamSendHandler
    {
        public boolean sendStarted;
        public boolean sendCompleted;
        public boolean sendCanceled;
        public final List<ByteArray> data;
        public ByteInputStream in;
        public int size;
        
        public TestStreamSendMessagePart(int value, List<ByteArray> data)
        {
            super(value, new ByteArray(new byte[]{}));
            this.data = data;
            for (ByteArray v : data)
                size += v.getLength();
        }

        @Override
        public int getSize()
        {
            return size;
        }
        
        @Override
        public int getStreamCount()
        {
            return data.size();
        }

        @Override
        public boolean hasData()
        {
            return in.available() != 0;
        }
        
        @Override
        public int read(byte[] sendBuffer)
        {
            return in.read(sendBuffer);
        }

        @Override
        public void sendStreamStarted(int streamIndex)
        {
            ByteArray buffer = data.get(streamIndex);
            in = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        }

        @Override
        public void sendStreamCompleted()
        {
            in = null;
        }

        @Override
        public void sendStarted()
        {
            sendStarted = true;
        }
        
        @Override
        public void sendCompleted()
        {
            sendCompleted = true;
        }

        @Override
        public void sendCanceled()
        {
            sendCanceled = true;
        }
    }
    
    public static class TestStreamReceiveMessagePart extends TestMessagePart implements IStreamReceiveHandler
    {
        public boolean receiveStarted;
        public boolean receiveCompleted;
        public boolean receiveCanceled;
        public List<ByteArray> data;
        public ByteOutputStream out;
        
        public TestStreamReceiveMessagePart(int value)
        {
            super(value, new ByteArray(new byte[]{}));
        }
        
        @Override
        public void write(byte[] buffer, int offset, int length)
        {
            out.write(buffer, offset, length);
        }

        @Override
        public void receiveStreamStarted(int streamIndex)
        {
            out = new ByteOutputStream();
        }
        
        @Override
        public void receiveStreamCompleted()
        {
            data.add(new ByteArray(out.getBuffer(), 0, out.getLength()));
            out = null;
        }
        
        @Override
        public void receiveStarted(int streamCount)
        {
            receiveStarted = true;
            data = new ArrayList<ByteArray>();
        }
        
        @Override
        public void receiveCompleted()
        {
            receiveCompleted = true;
        }

        @Override
        public void receiveCanceled()
        {
            receiveCanceled = true;
        }
    }

    public class TestFeed implements IFeed
    {
        private final boolean streamOriented;
        private final boolean compressed;
        private final int size;
        private int index;
        private final int count;

        public TestFeed(boolean streamOriented, boolean compressed, int size, int count)
        {
            this.streamOriented = streamOriented;
            this.compressed = compressed;
            this.size = size;
            this.count = count;
        }
        
        @Override
        public synchronized void feed(ISink sink)
        {
            if (index == count)
            {
                sink.setReady(false);
                return;
            }
            
            while (index < count)
            {
                int flags;
                IMessagePart part;
                
                if (!streamOriented)
                    part = new TestMessagePart(index, createBuffer(index, size));
                else
                    part = new TestStreamSendMessagePart(index, Collections.singletonList(createBuffer(index, size)));
                
                if (!compressed)
                    flags = MessageFlags.NO_COMPRESS;
                else
                    flags = 0;
                
                IMessage request = client.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, 
                    flags);
                index++;
                
                if (!sink.send(request))
                    break;
            }
        }
    }
    
    public class TestChannelListener implements IChannelListener
    {
        @Override
        public void onNodeConnected(IAddress node)
        {
            connectionSequencer.allowSingle("Connected " + node);
        }

        @Override
        public void onNodeFailed(IAddress node)
        {
        }

        @Override
        public void onNodeDisconnected(IAddress node)
        {
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("====================================================================")
        ILocalizedMessage separator();
        @DefaultMessage("--------------------------------------------------------------------")
        ILocalizedMessage smallSeparator();
        @DefaultMessage("small count: {0}, small size: {1}, medium count:{2}, medium size:{3}, large count:{4}, large size:{5}")
        ILocalizedMessage parameters(int smallCount, int smallSize, int mediumCount, int mediumSize, int largeCount, int largeSize);
        @DefaultMessage("[{0} of {1}. Channel send test (type:{2}, stream-oriented:{3}, compressed: {4}, pull: {5}, secured:{6}, multiThreaded: {7})]")
        ILocalizedMessage testChannel(int index, int count, Type type, boolean streamOriented, boolean compressed, 
            boolean pull, boolean secured, boolean multiThreaded);
        @DefaultMessage("{0}.")
        ILocalizedMessage benchmark(Benchmark benchmark);
    }
}
