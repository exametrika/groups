/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupJoinMessagePart} is a group join message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupJoinMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final INode joiningNode;

    public GroupJoinMessagePart(INode joiningNode)
    {
        Assert.notNull(joiningNode);
        
        this.joiningNode = joiningNode;
    }
    
    public INode getJoiningNode()
    {
        return joiningNode;
    }
    
    @Override
    public int getSize()
    {
        return 1000;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(joiningNode).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("joining node: {0}")
        ILocalizedMessage toString(INode joiningNode);
    }
}

