/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.api.groups.core.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.core.INode;
import com.exametrika.api.groups.core.MembershipEvent;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.groups.core.discovery.INodeDiscoverer;
import com.exametrika.spi.groups.IPropertyProvider;

/**
 * The {@link MembershipManager} manages membership information.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class MembershipManager implements IMembershipManager, ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(MembershipManager.class);
    private final String channelName;
    private final ILiveNodeProvider liveNodeProvider;
    private INodeDiscoverer nodeDiscoverer;
    private final IPropertyProvider propertyProvider;
    private final Set<IPreparedMembershipListener> preparedMembershipListeners;
    private final Set<IMembershipListener> privateMembershipListeners;
    private volatile Set<IMembershipListener> publicMembershipListeners = new HashSet<IMembershipListener>();
    private final IMarker marker;
    private volatile INode localNode;
    private IMembership preparedMembership;
    private volatile IMembership membership;
    private IMembershipChange membershipChange;
    
    public MembershipManager(String channelName, ILiveNodeProvider liveNodeProvider, IPropertyProvider propertyProvider,
        Set<IPreparedMembershipListener> preparedMembershipListeners, Set<IMembershipListener> membershipListeners)
    {
        Assert.notNull(channelName);
        Assert.notNull(liveNodeProvider);
        Assert.notNull(propertyProvider);
        Assert.notNull(preparedMembershipListeners);
        Assert.notNull(membershipListeners);

        this.channelName = channelName;
        this.liveNodeProvider = liveNodeProvider;
        this.propertyProvider = propertyProvider;
        this.preparedMembershipListeners = preparedMembershipListeners;
        this.privateMembershipListeners = membershipListeners;
        marker = Loggers.getMarker(channelName);
    }
    
    public void setNodeDiscoverer(INodeDiscoverer nodeDiscoverer)
    {
        Assert.notNull(nodeDiscoverer);
        Assert.isNull(this.nodeDiscoverer);
        
        this.nodeDiscoverer = nodeDiscoverer;
    }
    
    @Override
    public INode getLocalNode()
    {
        Assert.notNull(localNode);
        
        return localNode;
    }

    @Override
    public IMembership getMembership()
    {
        return membership;
    }

    @Override
    public synchronized void addMembershipListener(IMembershipListener listener)
    {
        Assert.notNull(listener);
        
        if (publicMembershipListeners.contains(listener))
            return;
        
        Set<IMembershipListener> listeners = new HashSet<IMembershipListener>(publicMembershipListeners);
        listeners.add(listener);
        
        publicMembershipListeners = listeners;
    }

    @Override
    public synchronized void removeMembershipListener(IMembershipListener listener)
    {
        Assert.notNull(listener);
        
        if (!publicMembershipListeners.contains(listener))
            return;
        
        Set<IMembershipListener> listeners = new HashSet<IMembershipListener>(publicMembershipListeners);
        listeners.remove(listener);
        
        publicMembershipListeners = listeners;
    }

    @Override
    public synchronized void removeAllMembershipListeners()
    {
        publicMembershipListeners = new HashSet<IMembershipListener>();
    }

    @Override
    public IMembership getPreparedMembership()
    {
        return preparedMembership;
    }
    
    @Override
    public void prepareInstallMembership(IMembership membership)
    {
        Assert.checkState(localNode != null);
        Assert.notNull(membership);
        Assert.notNull(membership.getGroup().findMember(localNode.getId()));
        Assert.checkState(this.membership == null);
        Assert.checkState(preparedMembership == null);

        this.preparedMembership = membership;
        this.membershipChange = null;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.preparedToInstall(Strings.wrap(membership.toString(), 4, 120)));

        onPreparedMembershipChanged(null, membership, null);
    }

    @Override
    public void prepareChangeMembership(IMembership membership, IMembershipChange membershipChange)
    {
        Assert.checkState(localNode != null);
        Assert.notNull(membership);
        Assert.notNull(membershipChange);
        Assert.notNull(membership.getGroup().findMember(localNode.getId()));
        Assert.checkState(this.membership != null);
        Assert.isTrue(this.membership.getId() + 1 == membership.getId());
        Assert.checkState(Objects.equals(this.membership, preparedMembership));
        
        this.preparedMembership = membership;
        this.membershipChange = membershipChange;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.preparedToChange(
                Strings.wrap(this.membership.toString(), 4, 120), Strings.wrap(this.preparedMembership.toString(), 4, 120),
                Strings.wrap(this.membershipChange.toString(), 4, 120)));
        
        onPreparedMembershipChanged(this.membership, membership, membershipChange);
    }

    @Override
    public void commitMembership()
    {
        if (membership == null)
            commitInstallMembership();
        else
            commitChangeMembership();
    }
    
    @Override
    public void uninstallMembership(LeaveReason reason)
    {
        if (membership == null)
            return;
        
        membership = null;
        preparedMembership = null;
        membershipChange = null;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.uninstalled(reason));
        
        onLeft(reason);
    }
    
    @Override
    public void start()
    {
        Assert.checkState(nodeDiscoverer != null);
        
        localNode = new Node(liveNodeProvider.getLocalNode().getName(), 
            liveNodeProvider.getLocalNode(), propertyProvider.getProperties(), Memberships.CORE_DOMAIN);
        
        nodeDiscoverer.startDiscovery();
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
    
    private void commitInstallMembership()
    {
        Assert.checkState(preparedMembership != null);
        Assert.checkState(membership == null);
        
        this.membership = preparedMembership;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.committedToInstall(Strings.wrap(preparedMembership.toString(), 4, 120)));

        onJoined();
    }
    
    private void commitChangeMembership()
    {
        MembershipEvent event;

        Assert.checkState(preparedMembership != null);
        Assert.checkState(membership.getId() + 1 == preparedMembership.getId());
        
        event = new MembershipEvent(membership, preparedMembership, membershipChange);
        membership = preparedMembership;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.committedToChange(Strings.wrap(membership.toString(), 4, 120)));
        
        onMembershipChanged(event);
    }

    private void onJoined()
    {
        for (IMembershipListener listener : privateMembershipListeners)
            onJoined(listener);
        for (IMembershipListener listener : publicMembershipListeners)
            onJoined(listener);
    }

    private void onJoined(IMembershipListener listener)
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
        for (IMembershipListener listener : privateMembershipListeners)
            onLeft(reason, listener);
        for (IMembershipListener listener : publicMembershipListeners)
            onLeft(reason, listener);
    }

    private void onLeft(LeaveReason reason, IMembershipListener listener)
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
    
    private void onMembershipChanged(MembershipEvent event)
    {
        for (IMembershipListener listener : privateMembershipListeners)
            onMembershipChanged(event, listener);
        for (IMembershipListener listener : publicMembershipListeners)
            onMembershipChanged(event, listener);
    }

    private void onMembershipChanged(MembershipEvent event, IMembershipListener listener)
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
    
    private void onPreparedMembershipChanged(IMembership oldMembership, IMembership newMembership, IMembershipChange membershipChange)
    {
        for (IPreparedMembershipListener preparedMembershipListener : preparedMembershipListeners)
        {
            try
            {
                preparedMembershipListener.onPreparedMembershipChanged(oldMembership, newMembership, membershipChange);
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
    }
    
    private interface IMessages
    {
        @DefaultMessage("Membership installation has been prepared. Membership:\n{0}.")
        ILocalizedMessage preparedToInstall(String membership);
        @DefaultMessage("Membership installation has been committed. Membership:\n{0}.")
        ILocalizedMessage committedToInstall(String membership);
        @DefaultMessage("Membership change has been prepared. Old membership:\n{0}\nNew membership:\n{1}\nMembership change:\n{2}.")
        ILocalizedMessage preparedToChange(String oldMembership, String newMembership, String membershipChange);
        @DefaultMessage("Membership change has been committed. Membership:\n{0}.")
        ILocalizedMessage committedToChange(String membership);
        @DefaultMessage("Membership has been uninstalled (reason = {0}).")
        ILocalizedMessage uninstalled(LeaveReason reason);
    }
}
