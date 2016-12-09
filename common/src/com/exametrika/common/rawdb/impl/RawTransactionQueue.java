/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.exametrika.common.compartment.ICompartmentQueue;
import com.exametrika.common.compartment.impl.CompartmentTaskList;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;


/**
 * The {@link RawTransactionQueue} is a transaction queue.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class RawTransactionQueue implements ICompartmentQueue
{
    private final SimpleDeque<Event> queue = new SimpleDeque<Event>();
    private Event currentBatchTransaction;
    private LinkedList<Event> pendingTransactions = new LinkedList<Event>();
    private int capacity;
    
    @Override
    public int getCapacity()
    {
        return capacity;
    }
    
    @Override
    public void offer(Event event)
    {
        capacity += event.size;
        queue.offer(event);
    }

    @Override
    public Event poll(boolean firstInBatch)
    {
        if (currentBatchTransaction != null)
        {
            RawDbBatchOperation currentBatchOperation = (RawDbBatchOperation)((IRawTransaction)currentBatchTransaction.task).getOperation();
            if (currentBatchOperation.isCompleted())
            {
                for (Event pendingTransaction : currentBatchOperation.getPendingTransactions())
                    pendingTransactions.addLast(pendingTransaction);
                capacity -= currentBatchTransaction.size;
                currentBatchTransaction = null;
            }
            else
            {
                if (currentBatchOperation.canContinue())
                    return currentBatchTransaction;

                Event transaction = currentBatchOperation.takePendingTransaction();
                if (transaction != null)
                {
                    capacity -= transaction.size;
                    return transaction;
                }

                transaction = pollFromQueue();
                if (transaction != null)
                    return transaction;
                else
                    return firstInBatch ? currentBatchTransaction : null;
            }
        }

        Event transaction = pollFromPendingQueue();
        if (transaction == null)
            transaction = pollFromQueue();
        
        return transaction;
    }

    private Event pollFromPendingQueue()
    {
        Assert.checkState(currentBatchTransaction == null);
        
        if (pendingTransactions.isEmpty())
            return null;

        Event transaction = pendingTransactions.removeLast();
        if (((IRawTransaction)transaction.task).getOperation() instanceof RawDbBatchOperation)
            currentBatchTransaction = transaction;
        else
            capacity -= transaction.size;

        return transaction;
    }
    
    private Event pollFromQueue()
    {
        if (currentBatchTransaction == null)
        {
            Assert.checkState(pendingTransactions.isEmpty());
            
            Event event = queue.poll();
            if (event == null)
                return null;
            else if (!(event.task instanceof IRawTransaction))
            {
                capacity -= event.size;
                return event;
            }
            
            if (((IRawTransaction)event.task).getOperation() instanceof RawDbBatchOperation)
                currentBatchTransaction = event;
            else
                capacity -= event.size;

            return event;
        }
        else while (true)
        {
            Event event = queue.poll();
            if (event == null)
                return null;
            
            if (event.task instanceof CompartmentTaskList)
            {
                CompartmentTaskList list = (CompartmentTaskList)event.task;
                if (!(list.getTasks().get(0) instanceof IRawTransaction))
                {
                    capacity -= event.size;
                    return event;
                }
                
                List<IRawTransaction> transactions = new ArrayList<IRawTransaction>();
                int totalSize = 0;
                for (IRawTransaction transaction : (Iterable<IRawTransaction>)list.getTasks())
                {
                    int size = transaction.getOperation().getSize();
                    if (!allow(new Event(transaction, event.compartment, size)))
                        continue;
                    
                    transactions.add(transaction);
                    totalSize += size;
                }
                
                if (!transactions.isEmpty())
                {
                    capacity -= totalSize;
                    return new Event(new CompartmentTaskList(transactions, true), event.compartment, totalSize);
                }
                else
                    continue;
            }
            else if (event.task instanceof IRawTransaction)
            {
                if (!allow(event))
                    continue;

                capacity -= event.size;
                return event;
            }
            else
            {
                capacity -= event.size;
                return event;
            }
        }
    }
    
    private boolean allow(Event event)
    {
        IRawTransaction transaction = (IRawTransaction)event.task;
        if (transaction.getOperation() instanceof RawDbBatchOperation)
        {
            pendingTransactions.addFirst(event);
            return false;
        }
        else
        {
            IRawOperation operation = transaction.getOperation();
            for (Event pendingTransaction : pendingTransactions)
            {
                RawDbBatchOperation pendingBatchOperation = (RawDbBatchOperation)((IRawTransaction)pendingTransaction.task).getOperation();
                if (!pendingBatchOperation.allow(transaction.isReadOnly(), operation))
                {
                    pendingBatchOperation.addPendingTransaction(event);
                    operation = null;
                    break;
                }
            }
            
            if (operation == null)
                return false;
            
            RawDbBatchOperation currentBatchOperation = (RawDbBatchOperation)((IRawTransaction)currentBatchTransaction.task).getOperation();
            if (!currentBatchOperation.allow(transaction.isReadOnly(), operation))
            {
                currentBatchOperation.addPendingTransaction(event);
                return false;
            }
        }
        
        return true;
    }
}
