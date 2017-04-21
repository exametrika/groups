/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.exametrika.common.messaging.impl.AbstractChannelFactory.FactoryParameters;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.common.net.TcpChannelTests;


/**
 * The {@link ScalabilityChannelPerfTests} are performance tests for client-server {@link Channel}.
 * 
 * @see Channel
 * @author Medvedev-A
 */
public class ScalabilityChannelPerfTests
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ScalabilityChannelPerfTests.class);
    private static final int CLIENT_COUNT = 100;
    private static final int COUNT = 100;
    private static final int SMALL_SIZE = 10;
    private static final int MEDIUM_SIZE = 1000;
    private static final int LARGE_SIZE = 100000;

    private IChannel[] clients = new IChannel[CLIENT_COUNT];
    private ChannelListener[] listeners = new ChannelListener[CLIENT_COUNT];
    private TestFeed[] feeds = new TestFeed[CLIENT_COUNT];
    private IChannel server;
    private ChannelReceiver receiver;
    private boolean received;
    
    @Test
    public void testChannel() throws Throwable
    {
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.parameters(CLIENT_COUNT));
        
//        testCaseChannel(0, false, false, false, false, Type.SMALL);
//        testCaseChannel(1, true, false, false, false, Type.SMALL);
//        testCaseChannel(2, false, true, false, false, Type.SMALL);
//        testCaseChannel(3, true, true, false, false, Type.SMALL);
//        testCaseChannel(4, false, false, true, false, Type.SMALL);
//        testCaseChannel(5, true, false, true, false, Type.SMALL);
//        testCaseChannel(6, false, true, true, false, Type.SMALL);
//        testCaseChannel(7, true, true, true, false, Type.SMALL);
//        testCaseChannel(8, false, false, false, true, Type.SMALL);
//        testCaseChannel(9, true, false, false, true, Type.SMALL);
//        testCaseChannel(10, false, true, false, true, Type.SMALL);
//        testCaseChannel(11, true, true, false, true, Type.SMALL);
//        testCaseChannel(12, false, false, true, true, Type.SMALL);
//        testCaseChannel(13, true, false, true, true, Type.SMALL);
//        testCaseChannel(14, false, true, true, true, Type.SMALL);
//        testCaseChannel(15, true, true, true, true, Type.SMALL);
        
        testCaseChannel(16, false, false, false, false, Type.MEDIUM);
        testCaseChannel(17, true, false, false, false, Type.MEDIUM);
        testCaseChannel(18, false, true, false, false, Type.MEDIUM);
        testCaseChannel(19, true, true, false, false, Type.MEDIUM);
        testCaseChannel(20, false, false, true, false, Type.MEDIUM);
        testCaseChannel(21, true, false, true, false, Type.MEDIUM);
        testCaseChannel(22, false, true, true, false, Type.MEDIUM);
        testCaseChannel(23, true, true, true, false, Type.MEDIUM);
        testCaseChannel(24, false, false, false, true, Type.MEDIUM);
        testCaseChannel(25, true, false, false, true, Type.MEDIUM);
        testCaseChannel(26, false, true, false, true, Type.MEDIUM);
        testCaseChannel(27, true, true, false, true, Type.MEDIUM);
        testCaseChannel(28, false, false, true, true, Type.MEDIUM);
        testCaseChannel(29, true, false, true, true, Type.MEDIUM);
        testCaseChannel(30, false, true, true, true, Type.MEDIUM);
        testCaseChannel(31, true, true, true, true, Type.MEDIUM);
        
        testCaseChannel(32, false, false, false, false, Type.LARGE);
        testCaseChannel(33, true, false, false, false, Type.LARGE);
        testCaseChannel(34, false, true, false, false, Type.LARGE);
        testCaseChannel(35, true, true, false, false, Type.LARGE);
        testCaseChannel(36, false, false, true, false, Type.LARGE);
        testCaseChannel(37, true, false, true, false, Type.LARGE);
        testCaseChannel(38, false, true, true, false, Type.LARGE);
        testCaseChannel(39, true, true, true, false, Type.LARGE);
        testCaseChannel(40, false, false, false, true, Type.LARGE);
        testCaseChannel(41, true, false, false, true, Type.LARGE);
        testCaseChannel(42, false, true, false, true, Type.LARGE);
        testCaseChannel(43, true, true, false, true, Type.LARGE);
        testCaseChannel(44, false, false, true, true, Type.LARGE);
        testCaseChannel(45, true, false, true, true, Type.LARGE);
        testCaseChannel(46, false, true, true, true, Type.LARGE);
        testCaseChannel(47, true, true, true, true, Type.LARGE);
    }
    
    private void testCaseChannel(int index, boolean secured, final boolean streamOriented, final boolean compressed, final boolean pull, 
        final Type type) throws Throwable
    {
        // Do test
        int size = 0;
        switch (type)
        {
        case SMALL:
            size = SMALL_SIZE;
            break;
        case MEDIUM:
            size = MEDIUM_SIZE;
            break;
        case LARGE:
            size = LARGE_SIZE;
            break;
        }
        
        final ByteArray buffer = createBuffer(size);

        received = false;
        
        createChannels(secured, streamOriented, compressed, pull, buffer);
        logger.log(LogLevel.INFO, messages.smallSeparator());
        logger.log(LogLevel.INFO, messages.testChannel(index, new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                doTest(streamOriented, compressed, pull, buffer);
            }
        }, 1, 0), secured, streamOriented, compressed, pull, type));
    }

    private void createChannels(boolean secured, boolean streamOriented, boolean compressed, boolean pull, ByteArray buffer) throws Throwable
    {
        boolean debug = true;
        FactoryParameters factoryParameters = new FactoryParameters(debug);
        factoryParameters.selectionPeriod = 1000;
        ChannelFactory factory = new ChannelFactory(new FactoryParameters(debug));
        ChannelFactory.Parameters parameters = new ChannelFactory.Parameters();
        
        parameters.channelName = "server";
        receiver = new ChannelReceiver(buffer, streamOriented, compressed);
        parameters.receiver = receiver;
        parameters.serverPart = true;
        parameters.secured = secured;
        parameters.keyStorePassword = "testtest";
        parameters.keyStorePath = "classpath:" + Classes.getResourcePath(TcpChannelTests.class) + "/keystore.jks";
        parameters.serializationRegistrars.add(new TestStreamMessagePartSerializer());
        parameters.serializationRegistrars.add(new TestMessagePartSerializer());
        
        server = factory.createChannel(parameters);
        server.start();
        
        factoryParameters = new FactoryParameters(debug);
        factoryParameters.selectionPeriod = 1000;
        factoryParameters.transportReceiveThreadCount = 1;
        factory = new ChannelFactory(factoryParameters);
        parameters.serverPart = false;
        parameters.clientPart = true;

        for (int i = 0; i < CLIENT_COUNT; i++)
        {
            listeners[i] = new ChannelListener();
            parameters.receiver = new ReceiverMock();
            parameters.channelName = "client" + i;
            clients[i] = factory.createChannel(parameters);
            clients[i].getChannelObserver().addChannelListener(listeners[i]);
            clients[i].start();
            
            if (pull)
            {
                feeds[i] = new TestFeed(streamOriented, compressed, buffer);
                clients[i].register(server.getLiveNodeProvider().getLocalNode(), feeds[i]);
            }
            clients[i].connect(server.getLiveNodeProvider().getLocalNode().getConnection(0));
        }
        
        while (true)
        {
            boolean connected = true;
            for (int i = 0; i < CLIENT_COUNT; i++)
            {
                if (!listeners[i].connected)
                {
                    connected = false;
                    break;
                }
            }
             
            if (connected)
                break;
            
            Thread.sleep(100);
        }
    }
    
    private void destroyChannels()
    {
        server.stop();
        for (int i = 0; i < CLIENT_COUNT; i++)
            clients[i].stop();
    }

    private void doTest(boolean streamOriented, boolean compressed, boolean pull, ByteArray buffer)
    {
        try
        {
            if (!pull)
            {
                for (int k = 0; k < COUNT; k++)            
                {
                    for (int i = 0; i < CLIENT_COUNT; i++)
                    {
                        int flags;
                        IMessagePart part;
                        
                        if (!streamOriented)
                            part = new TestMessagePart(k, buffer);
                        else
                            part = new TestStreamSendMessagePart(k, Collections.singletonList(buffer));
                        
                        if (!compressed)
                            flags = MessageFlags.NO_COMPRESS;
                        else
                            flags = 0;
                    
                        IMessage request = clients[i].getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, 
                            flags);
                        
                        clients[i].send(request);
                    }
                }
            }
            
            receiveWait();
        }
        finally
        {
            destroyChannels();
        }
    }

    private synchronized void receiveWait()
    {
        while (!received)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
    
    private synchronized void receiveNotify()
    {
        received = true;
        notifyAll();
    }

    private ByteArray createBuffer(int size)
    {
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; i++)
            buffer[i] = (byte)i;
        
        return new ByteArray(buffer);
    }
    
    private enum Type
    {
        LARGE,
        MEDIUM,
        SMALL
    }

    private class ChannelReceiver implements IReceiver
    {
        public ByteArray buffer;
        public int index;
        private final boolean streamOriented;
        private final boolean compressed;
        private Map<IAddress, Integer> indexes = new HashMap<IAddress, Integer>();
        
        public ChannelReceiver(ByteArray buffer, boolean streamOriented, boolean compressed)
        {
            this.buffer = buffer;
            this.streamOriented = streamOriented;
            this.compressed = compressed;
        }
        
        @Override
        public synchronized void receive(IMessage message)
        {
            Assert.notNull(message);
            
            assertThat(message.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
            Integer i = indexes.get(message.getSource());
            if (i == null)
                i = -1;
            
            if (!compressed)
                assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
            else
                assertThat(message.getFlags(), is(0));
            
            if (!streamOriented)
            {
                TestMessagePart part = message.getPart();
                assertThat(part, is(new TestMessagePart(i + 1, buffer)));
            }
            else
            {
                TestStreamReceiveMessagePart part = message.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart(i + 1)));
                assertThat(part.data, is(Collections.singletonList(buffer)));
            }

            indexes.put(message.getSource(), ++i);
            index++;

            if (index == CLIENT_COUNT * COUNT)
                receiveNotify();
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
        public int getSize()
        {
            return buffer.getLength() + 4;
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
        public String toString()
        {
            return Integer.toString(value);
        }
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
        
        public TestStreamSendMessagePart(int value, List<ByteArray> data)
        {
            super(value, new ByteArray(new byte[]{}));
            this.data = data;
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
        private final ByteArray buffer;
        private int index;

        public TestFeed(boolean streamOriented, boolean compressed, ByteArray buffer)
        {
            this.streamOriented = streamOriented;
            this.compressed = compressed;
            this.buffer = buffer;
        }
        @Override
        public synchronized void feed(ISink sink)
        {
            if (index == COUNT)
            {
                sink.setReady(false);
                return;
            }
            
            while (index < COUNT)
            {
                int flags;
                IMessagePart part;
                
                if (!streamOriented)
                    part = new TestMessagePart(index, buffer);
                else
                    part = new TestStreamSendMessagePart(index, Collections.singletonList(buffer));
                
                if (!compressed)
                    flags = MessageFlags.NO_COMPRESS;
                else
                    flags = 0;
                
                IMessage request = sink.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, 
                    flags);
                index++;
                
                if (!sink.send(request))
                    break;
            }
        }
    }

    private class ChannelListener implements IChannelListener
    {
        private boolean connected;

        @Override
        public void onNodeConnected(IAddress node)
        {
            assertThat(node, is(server.getLiveNodeProvider().getLocalNode()));
            connected = true;
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
        @DefaultMessage("Client count:{0}")
        ILocalizedMessage parameters(int count);
        @DefaultMessage("[{0}. Channel send test (secured:{2}, stream-oriented:{3}, compressed: {4}, pull: {5}, type:{6})] {1}.")
        ILocalizedMessage testChannel(int index, Benchmark benchmark, boolean secured, boolean streamOriented, boolean compressed, 
            boolean pull, Type type);
    }
}
