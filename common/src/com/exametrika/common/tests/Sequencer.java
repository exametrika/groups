/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tests;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;


/**
 * The {@link Sequencer} is a sequencer that sequences multithread execution in steps. Sequencer implements following
 * execution patterns:
 * <li> Barrier. Main thread creates a cyclic barrier and waits with other threads when all reached the barrier  
 * <li> Main thread waits until other thread allow or deny it's execution.
 * <li> Other threads wait until main thread allows their execution
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class Sequencer
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(Sequencer.class);
    private int replyCount;
    private RuntimeException denyException;
    private String denyMessage;
    private volatile CyclicBarrier barrier;
    private int allowCount;

    /**
     * Creates cyclic barrier.
     *
     * @param partiesCount number of other parties to wait
     */
    public void createBarrier(int partiesCount)
    {
        createBarrier(partiesCount, "");
    }
    
    /**
     * Creates cyclic barrier.
     *
     * @param partiesCount number of other parties to wait
     * @param message logging message
     */
    public void createBarrier(int partiesCount, String message)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.barrierCreated(message));
        
        barrier = new CyclicBarrier(partiesCount);
    }
    
    /**
     * Waits until all other parties reached the barrier.
     */
    public void waitBarrier()
    {
        waitBarrier(Long.MAX_VALUE, "");
    }
    
    /**
     * Waits until all other parties reached the barrier or timeout occured.
     *
     * @param timeout period in milliseconds to wait until timeout occured
     * @exception IllegalArgumentException if barrier is not created
     */
    public void waitBarrier(long timeout)
    {
        waitBarrier(timeout, "");
    }
    
    /**
     * Waits until all other parties reached the barrier or timeout occured.
     *
     * @param timeout period in milliseconds to wait until timeout occured
     * @param message logging message
     * @exception IllegalArgumentException if barrier is not created
     */
    public void waitBarrier(long timeout, String message) 
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitStarted(message));
        
        Assert.notNull(barrier);
        try
        {
            barrier.await(timeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitCompleted(message));
    }
    
    /**
     * Waits until given number of replies allowed execution.
     *
     * @param replyCount number of replies to wait
     */
    public void waitAll(int replyCount)
    {
        waitAll(replyCount, Long.MAX_VALUE, 0, "");
    }
    
    /**
     * Waits until given number of replies allowed execution or timeout occured.
     *
     * @param replyCount number of replies to wait
     * @param timeout period in milliseconds to wait until timeout occured
     * @param afterDelay delay in milliseconds after wait
     */
    public void waitAll(int replyCount, long timeout, long afterDelay)
    {
        waitAll(replyCount, timeout, afterDelay, "");
    }
    
    /**
     * Waits until given number of replies allowed execution or timeout occured.
     *
     * @param replyCount number of replies to wait
     * @param timeout period in milliseconds to wait until timeout occured
     * @param afterDelay delay in milliseconds after wait
     * @param message logging message
     */
    public synchronized void waitAll(int replyCount, long timeout, long afterDelay, String message)
    {
        if (replyCount < 0 || timeout < 0 || afterDelay < 0)
            throw new IllegalArgumentException();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitAllStarted(message));
        
        long startTime = Times.getCurrentTime();
        
        try
        {
            while (this.replyCount < replyCount)
            {
                if (timeout > 0 && Times.getCurrentTime() - startTime > timeout)
                    throw new TimeoutException();
            
                if (this.replyCount == -1)
                {
                    if (denyException != null)
                        throw denyException;
                    else if (denyMessage != null)
                        throw new AssertionError(denyMessage);
                    else
                        throw new AssertionError();
                }
                
                wait(timeout);
            }
            
            this.replyCount = 0;
            
            if (afterDelay != 0)
                Thread.sleep(afterDelay);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitAllCompleted(message));
    }

    /**
     * Allows execution by single reply.
     */
    public synchronized void allowSingle()
    {
        allowSingle("");
    }
    
    /**
     * Allows execution by single reply.
     * 
     * @param message logging message
     */
    public synchronized void allowSingle(String message)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitAllAllowed(message));
        
        if (replyCount == -1)
            return;
        
        replyCount++;
        notify();
    }

    /**
     * Denies execution by single reply.
     */
    public synchronized void denySingle()
    {
        denySingle("");
    }

    /**
     * Denies execution by single reply with specified exception.
     * 
     * @param exception occured exception
     */
    public synchronized void denySingle(RuntimeException exception)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitAllDenied(exception.getMessage()));
        
        replyCount = -1;
        denyException = exception;
        notify();
    }

    /**
     * Denies execution by single reply with specified message.
     * 
     * @param message deny message
     */
    public synchronized void denySingle(String message)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitAllDenied(message));
        
        replyCount = -1;
        denyMessage = message;
        notify();
    }
    
    /**
     * Waits until main thread allows further execution.
     */
    public synchronized void waitSingle()
    {
        waitSingle(Long.MAX_VALUE, "");
    }
    
    /**
     * Waits until main thread allows further execution or timeout occured.
     *
     * @param timeout period in milliseconds to wait until timeout occured
     */
    public synchronized void waitSingle(long timeout)
    {
        waitSingle(timeout, "");
    }
    
    /**
     * Waits until main thread allows further execution or timeout occured.
     *
     * @param timeout period in milliseconds to wait until timeout occured
     * @param message logging message
     */
    public synchronized void waitSingle(long timeout, String message)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitSingleStarted(message));
        
        long startTime = Times.getCurrentTime();
        
        try
        {
            while (allowCount <= 0)
            {
                wait(timeout);
                
                if (timeout > 0 && Times.getCurrentTime() - startTime > timeout)
                    throw new TimeoutException();
            }
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            allowCount--;
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitSingleCompleted(message));
    }
    
    /**
     * Allows execution of waiting threads.
     * 
     * @param allowCount number of threads whose execution is allowed to proceed
     */
    public synchronized void allowAll(int allowCount)
    {
        allowAll(allowCount, "");
    }
    
    /**
     * Allows execution of waiting threads.
     * 
     * @param allowCount number of threads whose execution is allowed to proceed
     * @param message logging message
     */
    public synchronized void allowAll(int allowCount, String message)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitSingleAllowed(message));
        
        this.allowCount += allowCount;
        notifyAll();
    }
    
    /**
     * Resets internal counters.
     */
    public synchronized void reset()
    {
        replyCount = 0;
        denyException = null;
        denyMessage = null;
        allowCount = 0;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Barrier has been created: {0}.")
        ILocalizedMessage barrierCreated(String message);
        
        @DefaultMessage("Wait on barrier has been started: {0}.")
        ILocalizedMessage waitStarted(String message);
        
        @DefaultMessage("Wait on barrier has been completed: {0}.")
        ILocalizedMessage waitCompleted(String message);

        @DefaultMessage("Wait for all has been started: {0}.")
        ILocalizedMessage waitAllStarted(String message);

        @DefaultMessage("Wait for all has been completed: {0}.")
        ILocalizedMessage waitAllCompleted(String message);
        
        @DefaultMessage("Wait for all has been allowed: {0}.")
        ILocalizedMessage waitAllAllowed(String message);
        
        @DefaultMessage("Wait for all has been denied: {0}.")
        ILocalizedMessage waitAllDenied(String string);
        
        @DefaultMessage("Wait for single has been started: {0}.")
        ILocalizedMessage waitSingleStarted(String message);
        
        @DefaultMessage("Wait for single has been completed: {0}.")
        ILocalizedMessage waitSingleCompleted(String message);
        
        @DefaultMessage("Wait for single has been allowed: {0}.")
        ILocalizedMessage waitSingleAllowed(String message);
    }
}
