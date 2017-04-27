/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.CoreNodeFactoryParameters;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
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
import com.exametrika.impl.groups.cluster.exchange.CoreFeedbackProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.CoreClusterFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerControllerNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;

/**
 * The {@link CoreToWorkerSubChannelFactory} is a core to worker node sub-channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreToWorkerSubChannelFactory extends AbstractChannelFactory
{
    private Set<IClusterMembershipListener> clusterMembershipListeners;
    private CoreClusterFailureDetectionProtocol clusterFailureDetectionProtocol;
    private ClusterMembershipManager clusterMembershipManager;
    private IGroupFailureDetector failureDetector;
    private CoreClusterDiscoveryProtocol clusterDiscoveryProtocol;
    private ISender bridgeSender;
    private CoreFeedbackProtocol coreToWorkerFeedbackProtocol;
    private ISender workerSender;
    
    public CoreToWorkerSubChannelFactory()
    {
        this(new CoreNodeFactoryParameters());
    }
    
    public CoreToWorkerSubChannelFactory(CoreNodeFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public void setClusterMembershipListeners(Set<IClusterMembershipListener> clusterMembershipListeners)
    {
        this.clusterMembershipListeners = clusterMembershipListeners;
    }
    
    public void setClusterMembershipManager(ClusterMembershipManager clusterMembershipManager)
    {
        this.clusterMembershipManager = clusterMembershipManager;
    }
    
    public void setFailureDetector(IGroupFailureDetector failureDetector)
    {
        this.failureDetector = failureDetector;
    }

    public void setBridgeSender(ISender bridgeSender)
    {
        this.bridgeSender = bridgeSender;
    }
    
    public void setCoreToWorkerFeedbackProtocol(CoreFeedbackProtocol coreToWorkerFeedbackProtocol)
    {
        this.coreToWorkerFeedbackProtocol = coreToWorkerFeedbackProtocol;
    }
    
    public ISender getWorkerSender()
    {
        return workerSender;
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
        CoreNodeFactoryParameters nodeFactoryParameters = (CoreNodeFactoryParameters)factoryParameters;
        
        clusterDiscoveryProtocol = new CoreClusterDiscoveryProtocol(channelName, messageFactory);
        protocols.add(clusterDiscoveryProtocol);
        
        Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
        clusterFailureDetectionProtocol = new CoreClusterFailureDetectionProtocol(
            channelName, messageFactory, clusterMembershipManager, failureDetector, failureDetectionListeners, nodeFactoryParameters.failureUpdatePeriod);
        protocols.add(clusterFailureDetectionProtocol);
        clusterMembershipListeners.add(clusterFailureDetectionProtocol);
        failureObservers.add(clusterFailureDetectionProtocol);
        
        protocols.add(coreToWorkerFeedbackProtocol);
        
        workerSender = protocols.get(protocols.size() - 1);
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        
    }
    
    protected void wireSubChannel()
    {
        clusterDiscoveryProtocol.setFailureDetector(failureDetector);
        clusterDiscoveryProtocol.setBridgeSender(bridgeSender);
        
        clusterFailureDetectionProtocol.setBridgeSender(bridgeSender);
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
