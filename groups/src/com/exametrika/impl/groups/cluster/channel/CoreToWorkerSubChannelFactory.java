/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;

import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.impl.groups.cluster.discovery.CoreClusterDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.discovery.IWorkerNodeDiscoverer;
import com.exametrika.impl.groups.cluster.failuredetection.CoreClusterFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerControllerNodeTrackingStrategy;

/**
 * The {@link CoreToWorkerSubChannelFactory} is a core to worker node sub-channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreToWorkerSubChannelFactory extends AbstractChannelFactory
{
    private CoreClusterFailureDetectionProtocol clusterFailureDetectionProtocol;
    private IGroupMembershipService membershipService;
    private IGroupFailureDetector failureDetector;
    private CoreClusterDiscoveryProtocol clusterDiscoveryProtocol;
    
    public CoreToWorkerSubChannelFactory()
    {
        this(new NodeFactoryParameters());
    }
    
    public CoreToWorkerSubChannelFactory(NodeFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public void setMembershipService(IGroupMembershipService membershipService)
    {
        this.membershipService = membershipService;
    }

    public void setFailureDetector(IGroupFailureDetector failureDetector)
    {
        this.failureDetector = failureDetector;
    }

    public IWorkerNodeDiscoverer getWorkerNodeDiscoverer()
    {
        return clusterDiscoveryProtocol;
    }
    
    @Override
    protected INodeTrackingStrategy createNodeTrackingStrategy()
    {
        return new WorkerControllerNodeTrackingStrategy(clusterFailureDetectionProtocol);
    }
    
    @Override
    protected void createProtocols(ChannelParameters parameters, String channelName, IMessageFactory messageFactory, 
        ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
        List<AbstractProtocol> protocols)
    {
        NodeParameters nodeParameters = (NodeParameters)parameters;
        
        clusterDiscoveryProtocol = new CoreClusterDiscoveryProtocol(channelName, messageFactory);
        protocols.add(clusterDiscoveryProtocol);
        
        
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        
    }
    
    protected void wireSubChannel()
    {
        clusterDiscoveryProtocol.setFailureDetector(failureDetector);
        clusterDiscoveryProtocol.setMembershipService(membershipService);
    }
    
    @Override
    protected SubChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        return new CoreToWorkerSubChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
            connectionManager, compartment);
    }
}
