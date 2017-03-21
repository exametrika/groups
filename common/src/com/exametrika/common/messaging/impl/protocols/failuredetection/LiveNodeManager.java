/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link LiveNodeManager} is a manager of live nodes.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class LiveNodeManager implements IFailureObserver, IConnectionObserver, ILocalNodeAware, ILiveNodeProvider, ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(LiveNodeManager.class);
    private final List<IFailureObserver> failureObservers;
    private final IConnectionObserver connectionObserver;
    private final IMarker marker;
    private volatile long id;
    private final UnicastAddress localNode;
    private volatile List<IAddress> liveNodes = new ArrayList<IAddress>();
    private volatile HashMap<UUID, IAddress> liveNodesById = new HashMap<UUID, IAddress>();
    private volatile HashMap<String, IAddress> liveNodesByName = new HashMap<String, IAddress>();
    private volatile HashMap<String, IAddress> liveNodesByConnection = new HashMap<String, IAddress>();
    
    public LiveNodeManager(String channelName, List<IFailureObserver> failureObservers, IConnectionObserver connectionObserver)
    {
        Assert.notNull(channelName);
        Assert.notNull(failureObservers);

        this.failureObservers = failureObservers;
        this.connectionObserver = connectionObserver;
        this.localNode = new UnicastAddress(UUID.randomUUID(), channelName);
        this.marker = Loggers.getMarker(channelName);
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public IAddress getLocalNode()
    {
        return localNode;
    }

    @Override
    public List<IAddress> getLiveNodes()
    {
        return Immutables.wrap(liveNodes);
    }

    @Override
    public boolean isLive(IAddress address)
    {
        Assert.notNull(address);
        
        Map<UUID, IAddress> liveNodesById = this.liveNodesById;
        return liveNodesById.containsKey(address.getId());
    }

    @Override
    public IAddress findById(UUID id)
    {
        Assert.notNull(id);
        
        Map<UUID, IAddress> liveNodesById = this.liveNodesById;
        return liveNodesById.get(id);
    }

    @Override
    public IAddress findByName(String name)
    {
        Assert.notNull(name);
        
        Map<String, IAddress> liveNodesByName = this.liveNodesByName;
        return liveNodesByName.get(name);
    }

    @Override
    public IAddress findByConnection(String connection)
    {
        Assert.notNull(connection);
        
        Map<String, IAddress> liveNodesByConnection = this.liveNodesByConnection;
        return liveNodesByConnection.get(connection);
    }
    
    @Override
    public UnicastAddress setLocalNode(int transportId, Object address, String connection)
    {
        localNode.setAddress(transportId, address, connection);
        return localNode;
    }
    
    @Override
    public void onNodesConnected(Set<IAddress> nodes)
    {
        Set<IAddress> changedNodes = new HashSet<IAddress>();
        synchronized (this)
        {
            ArrayList<IAddress> liveNodes = (ArrayList<IAddress>)((ArrayList<IAddress>)this.liveNodes).clone();
            HashMap<UUID, IAddress> liveNodesById = (HashMap<UUID, IAddress>)this.liveNodesById.clone();
            HashMap<String, IAddress> liveNodesByName = (HashMap<String, IAddress>)this.liveNodesByName.clone();
            HashMap<String, IAddress> liveNodesByConnection = (HashMap<String, IAddress>)this.liveNodesByConnection.clone();
            
            for (IAddress node : nodes)
            {
                if (liveNodes.contains(node))
                    continue;
                    
                liveNodes.add(node);
                liveNodesById.put(node.getId(), node);
                liveNodesByName.put(node.getName(), node);
                for (int i = 0; i < node.getCount(); i++)
                    liveNodesByConnection.put(node.getConnection(i), node);
                changedNodes.add(node);
            }
            
            if (!changedNodes.isEmpty())
            {
                this.liveNodes = liveNodes;
                this.liveNodesById = liveNodesById;
                this.liveNodesByName = liveNodesByName;
                this.liveNodesByConnection = liveNodesByConnection;
                id++;
            }
        }
        
        if (connectionObserver != null)
            connectionObserver.onNodesConnected(changedNodes);
    }

    @Override
    public void onNodesFailed(Set<IAddress> nodes)
    {
        Set<IAddress> changedNodes = new HashSet<IAddress>();
        synchronized (this)
        {
            ArrayList<IAddress> liveNodes = (ArrayList<IAddress>)((ArrayList<IAddress>)this.liveNodes).clone();
            HashMap<UUID, IAddress> liveNodesById = (HashMap<UUID, IAddress>)this.liveNodesById.clone();
            HashMap<String, IAddress> liveNodesByName = (HashMap<String, IAddress>)this.liveNodesByName.clone();
            HashMap<String, IAddress> liveNodesByConnection = (HashMap<String, IAddress>)this.liveNodesByConnection.clone();
            
            for (IAddress node : nodes)
            {
                if (!liveNodes.contains(node))
                    continue;
                    
                liveNodes.remove(node);
                liveNodesById.remove(node.getId());
                liveNodesByName.remove(node.getName());
                for (int i = 0; i < node.getCount(); i++)
                    liveNodesByConnection.remove(node.getConnection(i));
                changedNodes.add(node);
            }
            
            if (!changedNodes.isEmpty())
            {
                this.liveNodes = liveNodes;
                this.liveNodesById = liveNodesById;
                this.liveNodesByName = liveNodesByName;
                this.liveNodesByConnection = liveNodesByConnection;
            }
            
            id++;
        }
        
        for (IFailureObserver failureObserver : failureObservers)
            failureObserver.onNodesFailed(nodes);
    }

    @Override
    public void onNodesLeft(Set<IAddress> nodes)
    {
        Set<IAddress> changedNodes = new HashSet<IAddress>();
        synchronized (this)
        {
            ArrayList<IAddress> liveNodes = (ArrayList<IAddress>)((ArrayList<IAddress>)this.liveNodes).clone();
            HashMap<UUID, IAddress> liveNodesById = (HashMap<UUID, IAddress>)this.liveNodesById.clone();
            HashMap<String, IAddress> liveNodesByName = (HashMap<String, IAddress>)this.liveNodesByName.clone();
            HashMap<String, IAddress> liveNodesByConnection = (HashMap<String, IAddress>)this.liveNodesByConnection.clone();
            
            for (IAddress node : nodes)
            {
                if (!liveNodes.contains(node))
                    continue;
                    
                liveNodes.remove(node);
                liveNodesById.remove(node.getId());
                liveNodesByName.remove(node.getName());
                for (int i = 0; i < node.getCount(); i++)
                    liveNodesByConnection.remove(node.getConnection(i));
                changedNodes.add(node);
            }
            
            if (!changedNodes.isEmpty())
            {
                this.liveNodes = liveNodes;
                this.liveNodesById = liveNodesById;
                this.liveNodesByName = liveNodesByName;
                this.liveNodesByConnection = liveNodesByConnection;
                id++;
            }
        }
        
        for (IFailureObserver failureObserver : failureObservers)
            failureObserver.onNodesLeft(changedNodes);
    }

    @Override
    public synchronized void start()
    {
        Assert.checkState(localNode != null);
        
        ArrayList<IAddress> liveNodes = (ArrayList<IAddress>)((ArrayList<IAddress>)this.liveNodes).clone();
        HashMap<UUID, IAddress> liveNodesById = (HashMap<UUID, IAddress>)this.liveNodesById.clone();
        HashMap<String, IAddress> liveNodesByName = (HashMap<String, IAddress>)this.liveNodesByName.clone();
        HashMap<String, IAddress> liveNodesByConnection = (HashMap<String, IAddress>)this.liveNodesByConnection.clone();
        
        liveNodes.add(localNode);
        liveNodesById.put(localNode.getId(), localNode);
        liveNodesByName.put(localNode.getName(), localNode);
        for (int i = 0; i < localNode.getCount(); i++)
            liveNodesByConnection.put(localNode.getConnection(i), localNode);
        
        this.liveNodes = liveNodes;
        this.liveNodesById = liveNodesById;
        this.liveNodesByName = liveNodesByName;
        this.liveNodesByConnection = liveNodesByConnection;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.managerStarted());
    }

    @Override
    public void stop()
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.managerStopped());
    }
    
    private interface IMessages
    {
        @DefaultMessage("Live node manager has been started.")
        ILocalizedMessage managerStarted();
        @DefaultMessage("Live node manager has been stopped.")
        ILocalizedMessage managerStopped();
    }
}
