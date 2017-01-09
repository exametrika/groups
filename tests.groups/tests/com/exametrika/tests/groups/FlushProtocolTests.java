/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.Parameters;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.groups.core.channel.GroupChannel;
import com.exametrika.impl.groups.core.channel.IChannelReconnector;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.core.discovery.DiscoveryProtocol;
import com.exametrika.impl.groups.core.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.core.failuredetection.FailureDetectionProtocol;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.core.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.core.flush.IFlush;
import com.exametrika.impl.groups.core.flush.IFlushParticipant;
import com.exametrika.impl.groups.core.membership.IPreparedMembershipListener;
import com.exametrika.impl.groups.core.membership.MembershipManager;
import com.exametrika.impl.groups.core.membership.MembershipTracker;
import com.exametrika.spi.groups.IDiscoveryStrategy;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.groups.DiscoveryProtocolTests.GroupJoinStrategyMock;
import com.exametrika.tests.groups.MembershipManagerTests.PropertyProviderMock;

/**
 * The {@link FlushProtocolTests} are tests for flush.
 * 
 * @author Medvedev-A
 */
public class FlushProtocolTests
{
    private static final int COUNT = 10;
    private IChannel[] channels = new IChannel[COUNT];
    
    @After
    public void tearDown()
    {
        for (IChannel channel : channels)
            IOs.close(channel);
    }
    
    @Test
    public void testGroupFormation()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
            channels[i] = channel;
        }
    }
    
    private static class FlushParticipantMock implements IFlushParticipant
    {
        @Override
        public boolean isFlushProcessingRequired(IFlush flush)
        {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isCoordinatorStateSupported()
        {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setCoordinator()
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Object getCoordinatorState()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setCoordinatorState(List<Object> states)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void startFlush(IFlush flush)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void beforeProcessFlush()
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void processFlush()
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void endFlush()
        {
            // TODO Auto-generated method stub
            
        }
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private final IDiscoveryStrategy discoveryStrategy;
        private final long discoveryPeriod = 200;
        private final long groupFormationPeriod = 2000;
        private long failureUpdatePeriod = 500;
        private  long failureHistoryPeriod = 10000;
        private  int maxShunCount = 3;
        private long flushTimeout = 10000;
        private long gracefulCloseTimeout = 10000;
        private List<FlushParticipantMock> flushParticipants = new ArrayList<FlushParticipantMock>();
        private MembershipTracker membershipTracker;
        private MembershipManager membershipManager;
        private List<IGracefulCloseStrategy> gracefulCloseStrategies = new ArrayList<IGracefulCloseStrategy>();
        
        public TestChannelFactory(IDiscoveryStrategy discoveryStrategy)
        {
            super(new FactoryParameters(Debug.isDebug()));
            this.discoveryStrategy = discoveryStrategy;
        }
        
        @Override
        protected void createProtocols(Parameters parameters, String channelName, IMessageFactory messageFactory, 
            ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
            List<AbstractProtocol> protocols)
        {
            Set<IPreparedMembershipListener> preparedMembershipListeners = new HashSet<IPreparedMembershipListener>();
            Set<IMembershipListener> membershipListeners = new HashSet<IMembershipListener>();
            membershipManager = new MembershipManager("test", liveNodeProvider, new PropertyProviderMock(), 
                preparedMembershipListeners, membershipListeners);

            Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
            FailureDetectionProtocol failureDetectionProtocol = new FailureDetectionProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionListeners, failureUpdatePeriod, failureHistoryPeriod, maxShunCount);
            preparedMembershipListeners.add(failureDetectionProtocol);
            protocols.add(failureDetectionProtocol);
            failureObservers.add(failureDetectionProtocol);
            
            GroupJoinStrategyMock joinStrategy = new GroupJoinStrategyMock(); 
            DiscoveryProtocol discoveryProtocol = new DiscoveryProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionProtocol, discoveryStrategy, liveNodeProvider, new GroupJoinStrategyMock(), discoveryPeriod, 
                groupFormationPeriod);
            preparedMembershipListeners.add(discoveryProtocol);
            protocols.add(discoveryProtocol);
            membershipManager.setNodeDiscoverer(discoveryProtocol);
            
            joinStrategy.protocol = discoveryProtocol;
            joinStrategy.membershipService = membershipManager;
            joinStrategy.messageFactory = messageFactory;
            
            FlushParticipantMock flushParticipant = new FlushParticipantMock();
            flushParticipants.add(flushParticipant);
            FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
                Arrays.<IFlushParticipant>asList(flushParticipant), membershipManager, failureDetectionProtocol);
            protocols.add(flushParticipantProtocol);
            FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
                membershipManager, failureDetectionProtocol, flushTimeout, flushParticipantProtocol);
            failureDetectionListeners.add(flushCoordinatorProtocol);
            protocols.add(flushCoordinatorProtocol);

            membershipTracker = new MembershipTracker(1000, membershipManager, discoveryProtocol, 
                failureDetectionProtocol, flushCoordinatorProtocol);
            
            gracefulCloseStrategies.add(flushCoordinatorProtocol);
            gracefulCloseStrategies.add(flushParticipantProtocol);
            gracefulCloseStrategies.add(membershipTracker);
        }
        
        @Override
        protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            FailureDetectionProtocol failureDetectionProtocol = protocolStack.find(FailureDetectionProtocol.class);
            failureDetectionProtocol.setFailureObserver(transport);
            failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
            channel.getCompartment().addProcessor(membershipTracker);
        }
        
        @Override
        protected Channel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
            MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
            ConnectionManager connectionManager, ICompartment compartment)
        {
            return new GroupChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
                connectionManager, compartment, membershipManager, gracefulCloseStrategies, gracefulCloseTimeout);
        }
    }
}
