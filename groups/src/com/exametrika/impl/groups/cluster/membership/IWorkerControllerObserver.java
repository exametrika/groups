/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.common.messaging.IAddress;


/**
 * The {@link IWorkerControllerObserver} represents an observer of changing current worker controller - core node.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IWorkerControllerObserver
{
    /**
     * Called when controller of local worker node has been changed.
     *
     * @param controller new controller
     */
    void onControllerChanged(IAddress controller);
}