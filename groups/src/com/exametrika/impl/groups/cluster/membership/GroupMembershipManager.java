/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.groups.cluster.GroupMembershipEvent;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
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
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;

/**
 * The {@link GroupMembershipManager} manages group membership information.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class GroupMembershipManager implements IGroupMembershipManager, ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(GroupMembershipManager.class);
    private final String channelName;
    private final LocalNodeProvider localNodeProvider;
    private final Set<IPreparedGroupMembershipListener> preparedMembershipListeners;
    private final Set<IGroupMembershipListener> privateMembershipListeners;
    private volatile Set<IGroupMembershipListener> publicMembershipListeners = new HashSet<IGroupMembershipListener>();
    private final IMarker marker;
    private IGroupMembership preparedMembership;
    private volatile IGroupMembership membership;
    private IGroupMembershipChange membershipChange;
    
    public GroupMembershipManager(String channelName, LocalNodeProvider localNodeProvider,
        Set<IPreparedGroupMembershipListener> preparedMembershipListeners, Set<IGroupMembershipListener> membershipListeners)
    {
        Assert.notNull(channelName);
        Assert.notNull(localNodeProvider);
        Assert.notNull(preparedMembershipListeners);
        Assert.notNull(membershipListeners);

        this.channelName = channelName;
        this.localNodeProvider = localNodeProvider;
        this.preparedMembershipListeners = preparedMembershipListeners;
        this.privateMembershipListeners = membershipListeners;
        marker = Loggers.getMarker(channelName);
    }
    
    @Override
    public INode getLocalNode()
    {
        return localNodeProvider.getLocalNode();
    }

    @Override
    public IGroupMembership getMembership()
    {
        return membership;
    }

    @Override
    public synchronized void addMembershipListener(IGroupMembershipListener listener)
    {
        Assert.notNull(listener);
        
        if (publicMembershipListeners.contains(listener))
            return;
        
        Set<IGroupMembershipListener> listeners = new HashSet<IGroupMembershipListener>(publicMembershipListeners);
        listeners.add(listener);
        
        publicMembershipListeners = listeners;
    }

    @Override
    public synchronized void removeMembershipListener(IGroupMembershipListener listener)
    {
        Assert.notNull(listener);
        
        if (!publicMembershipListeners.contains(listener))
            return;
        
        Set<IGroupMembershipListener> listeners = new HashSet<IGroupMembershipListener>(publicMembershipListeners);
        listeners.remove(listener);
        
        publicMembershipListeners = listeners;
    }

    @Override
    public synchronized void removeAllMembershipListeners()
    {
        publicMembershipListeners = new HashSet<IGroupMembershipListener>();
    }

    @Override
    public IGroupMembership getPreparedMembership()
    {
        return preparedMembership;
    }
    
    @Override
    public void prepareInstallMembership(IGroupMembership membership)
    {
        Assert.checkState(getLocalNode() != null);
        Assert.notNull(membership);
        Assert.notNull(membership.getGroup().findMember(getLocalNode().getId()));
        Assert.checkState(this.membership == null);
        Assert.checkState(preparedMembership == null);

        this.preparedMembership = membership;
        this.membershipChange = null;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.preparedToInstall(Strings.wrap(membership.toString(), 4, 120)));

        onPreparedMembershipChanged(null, membership, null);
    }

    @Override
    public void prepareChangeMembership(IGroupMembership membership, IGroupMembershipChange membershipChange)
    {
        Assert.checkState(getLocalNode() != null);
        Assert.notNull(membership);
        Assert.notNull(membershipChange);
        Assert.notNull(membership.getGroup().findMember(getLocalNode().getId()));
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
        GroupMembershipEvent event;

        Assert.checkState(preparedMembership != null);
        Assert.checkState(membership.getId() + 1 == preparedMembership.getId());
        
        event = new GroupMembershipEvent(membership, preparedMembership, membershipChange);
        membership = preparedMembership;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.committedToChange(Strings.wrap(membership.toString(), 4, 120)));
        
        onMembershipChanged(event);
    }

    private void onJoined()
    {
        for (IGroupMembershipListener listener : privateMembershipListeners)
            onJoined(listener);
        for (IGroupMembershipListener listener : publicMembershipListeners)
            onJoined(listener);
    }

    private void onJoined(IGroupMembershipListener listener)
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
        for (IGroupMembershipListener listener : privateMembershipListeners)
            onLeft(reason, listener);
        for (IGroupMembershipListener listener : publicMembershipListeners)
            onLeft(reason, listener);
    }

    private void onLeft(LeaveReason reason, IGroupMembershipListener listener)
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
    
    private void onMembershipChanged(GroupMembershipEvent event)
    {
        for (IGroupMembershipListener listener : privateMembershipListeners)
            onMembershipChanged(event, listener);
        for (IGroupMembershipListener listener : publicMembershipListeners)
            onMembershipChanged(event, listener);
    }

    private void onMembershipChanged(GroupMembershipEvent event, IGroupMembershipListener listener)
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
    
    private void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange membershipChange)
    {
        for (IPreparedGroupMembershipListener preparedMembershipListener : preparedMembershipListeners)
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
