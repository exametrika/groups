/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipChange;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ClusterMembershipManager} is a cluster membership manager.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipManager implements IClusterMembershipManager, ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ClusterMembershipManager.class);
    private final String channelName;
    private final LocalNodeProvider localNodeProvider;
    private final Set<IClusterMembershipListener> privateMembershipListeners;
    private final IMarker marker;
    private volatile Set<IClusterMembershipListener> publicMembershipListeners = new HashSet<IClusterMembershipListener>();
    private volatile IClusterMembership membership;

    public ClusterMembershipManager(String channelName, LocalNodeProvider localNodeProvider,
        IGroupMembershipService  membershipService, Set<IClusterMembershipListener> membershipListeners)
    {
        Assert.notNull(channelName);
        Assert.notNull(localNodeProvider);
        Assert.notNull(membershipListeners);
        
        this.channelName = channelName;
        this.localNodeProvider = localNodeProvider;
        this.privateMembershipListeners = membershipListeners;
        marker = Loggers.getMarker(channelName);
    }
    
    @Override
    public INode getLocalNode()
    {
        return localNodeProvider.getLocalNode();
    }

    @Override
    public IClusterMembership getMembership()
    {
        return membership;
    }

    @Override
    public synchronized void addMembershipListener(IClusterMembershipListener listener)
    {
        Assert.notNull(listener);
        
        if (publicMembershipListeners.contains(listener))
            return;
        
        Set<IClusterMembershipListener> listeners = new HashSet<IClusterMembershipListener>(publicMembershipListeners);
        listeners.add(listener);
        
        publicMembershipListeners = listeners;
    }

    @Override
    public synchronized void removeMembershipListener(IClusterMembershipListener listener)
    {
        Assert.notNull(listener);
        
        if (!publicMembershipListeners.contains(listener))
            return;
        
        Set<IClusterMembershipListener> listeners = new HashSet<IClusterMembershipListener>(publicMembershipListeners);
        listeners.remove(listener);
        
        publicMembershipListeners = listeners;
    }

    @Override
    public synchronized void removeAllMembershipListeners()
    {
        publicMembershipListeners = new HashSet<IClusterMembershipListener>();
    }
    
    @Override
    public void installMembership(IClusterMembership membership)
    {
        Assert.notNull(membership);
        Assert.isNull(this.membership);
        
        this.membership = membership;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.installedMembership(Strings.wrap(membership.toString(), 4, 120)));
        
        onJoined();
    }

    @Override
    public void changeMembership(IClusterMembership membership, IClusterMembershipChange membershipChange)
    {
        Assert.notNull(membership);
        Assert.notNull(membershipChange);
        Assert.notNull(this.membership);
        Assert.isTrue(this.membership.getId() + 1 == membership.getId());
        
        IClusterMembership oldMembership = this.membership;
        ClusterMembershipEvent event = new ClusterMembershipEvent(oldMembership, membership, membershipChange);
        this.membership = membership;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.changedMembership(
                Strings.wrap(oldMembership.toString(), 4, 120), Strings.wrap(membership.toString(), 4, 120),
                Strings.wrap(membershipChange.toString(), 4, 120)));
        
        onMembershipChanged(event);
    }

    @Override
    public void uninstallMembership(LeaveReason reason)
    {
        if (membership == null)
            return;
        
        membership = null;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.uninstalledMembership(reason));
        
        onLeft(reason);
    }
    
    @Override
    public void start()
    {
    }
    
    @Override
    public void stop()
    {
        if (membership != null)
            uninstallMembership(LeaveReason.FORCEFUL_CLOSE);
    }
    
    @Override
    public String toString()
    {
        return channelName;
    }
    
    private void onJoined()
    {
        for (IClusterMembershipListener listener : privateMembershipListeners)
            onJoined(listener);
        for (IClusterMembershipListener listener : publicMembershipListeners)
            onJoined(listener);
    }

    private void onJoined(IClusterMembershipListener listener)
    {
        try
        {
            listener.onJoined();
        }
        catch (ThreadInterruptedException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception from other listeners
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, e);
        }
    }
    
    private void onLeft(LeaveReason reason)
    {
        for (IClusterMembershipListener listener : privateMembershipListeners)
            onLeft(reason, listener);
        for (IClusterMembershipListener listener : publicMembershipListeners)
            onLeft(reason, listener);
    }

    private void onLeft(LeaveReason reason, IClusterMembershipListener listener)
    {
        try
        {
            listener.onLeft(reason);
        }
        catch (ThreadInterruptedException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception from other listeners
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, e);
        }
    }
    
    private void onMembershipChanged(ClusterMembershipEvent event)
    {
        for (IClusterMembershipListener listener : privateMembershipListeners)
            onMembershipChanged(event, listener);
        for (IClusterMembershipListener listener : publicMembershipListeners)
            onMembershipChanged(event, listener);
    }

    private void onMembershipChanged(ClusterMembershipEvent event, IClusterMembershipListener listener)
    {
        try
        {
            listener.onMembershipChanged(event);
        }
        catch (ThreadInterruptedException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception from other listeners
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, e);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Membership has been installed. Membership:\n{0}.")
        ILocalizedMessage installedMembership(String membership);
        @DefaultMessage("Membership has been changed. Old membership:\n{0}\nNew membership:\n{1}\nMembership change:\n{2}.")
        ILocalizedMessage changedMembership(String oldMembership, String newMembership, String membershipChange);
        @DefaultMessage("Membership has been uninstalled (reason = {0}).")
        ILocalizedMessage uninstalledMembership(LeaveReason reason);
    }
}
