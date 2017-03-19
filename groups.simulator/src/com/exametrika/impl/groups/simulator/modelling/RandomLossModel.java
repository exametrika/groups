/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.Random;

import com.exametrika.common.messaging.IMessage;


/**
 * The {@link RandomLossModel} is a message loss model that drops messages accidentally.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RandomLossModel implements ILossModel
{
    private final Random random = new Random();
    private final int probability;
    
    /**
     * Creates a new object.
     *
     * @param probability message loss probability in percents
     * @exception IllegalArgumentException if probability is not in range [0..100]
     */
    public RandomLossModel(int probability)
    {
        if (probability < 0 || probability > 100)
            throw new IllegalArgumentException();
        
        this.probability = probability;
    }
    
    @Override
    public boolean canDropMessage(IMessage message)
    {
        int lossFactor = (int)(random.nextDouble() * 99f) + 1;
        return lossFactor <= probability;
    }
}
