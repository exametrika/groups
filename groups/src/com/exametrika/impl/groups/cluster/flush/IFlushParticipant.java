/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.flush;

/**
 * The {@link IFlushParticipant} is a participant of flush protocol.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFlushParticipant
{
    /**
     * Is processing phase of flush protocol required by this participant for specified flush? Result must be calculated
     * based on {@link IFlush} contents only and must not use any external state.
     *
     * @return true - if processing phase is required for specified flush, false - if not required
     */
    boolean isFlushProcessingRequired();
    
    /**
     * Sets participant as coordinator when local node has become a coordinator of the group.
     */
    void setCoordinator();
    
    /**
     * Called by flush protocol when stabilizing phase of flush protocol is started. After receiving this call participant must
     * send its messages in new membership. When all messages sent in old membership has been delivered,
     * participant must call {@link IFlush#grantFlush(IFlushParticipant)}. 
     * <p>
     * Implementation of this method must be idempotent, because if some node is failed during this phase, phase is restarted
     * by calling this method againg.
     *
     * @param flush current flush
     */
    void startFlush(IFlush flush);

    /**
     * Called by flush protocol before processing phase of flush protocol is started. Participant must deliver those old messages
     * that can be delivered, delete old undeliverable messages and new messages that depend on undeliverable old messages.
     * This provides gap freedom guarantee. Participant can also perform additional cleanup of stabilizing phase in this
     * method. 
     */
    void beforeProcessFlush();
    
    /**
     * Called by flush protocol when processing phase of flush protocol is started. Processing is performed by sending unicast messages
     * between nodes. When all processing is done, participant must call {@link IFlush#grantFlush(IFlushParticipant)}.
     */
    void processFlush();

    /**
     * Called by flush protocol when flush protocol has been completed. Participant can perform delivery of pending new messages in this
     * method.
     */
    void endFlush();
}
