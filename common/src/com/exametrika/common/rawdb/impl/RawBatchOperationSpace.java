/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;





/**
 * The {@link RawBatchOperationSpace} is a batch operation space.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class RawBatchOperationSpace
{
    public static final int BATCH_OPERATION_SPACE_FILE_INDEX = -1;
    public static final String BATCH_OPERATION_SPACE_FILE_NAME = "batch.dt";
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int HEADER_SIZE = 3;
    private static final short MAGIC = 0x170E;// magic(short) + version(byte)
    private static final byte VERSION = 0x1;
    private final RawTransactionManager transactionManager;
    private final ISerializationRegistry serializationRegistry;
    private IRawPage headerPage;

    public static RawBatchOperationSpace create(RawTransactionManager transactionManager)
    {
        Assert.notNull(transactionManager);
        
        RawBatchOperationSpace space = new RawBatchOperationSpace(transactionManager);
        space.writeHeader();
        
        return space;
    }
    
    public static RawBatchOperationSpace open(RawTransactionManager transactionManager)
    {
        Assert.notNull(transactionManager);
        
        RawBatchOperationSpace space = new RawBatchOperationSpace(transactionManager);
        space.readHeader();
        
        return space;
    }

    public RawDbBatchOperation getOperation(IRawBatchContext batchContext)
    {
        RawPageDeserialization pageDeserialization = new RawPageDeserialization(transactionManager.getTransaction(), 
            BATCH_OPERATION_SPACE_FILE_INDEX, headerPage, HEADER_SIZE);

        if (pageDeserialization.readBoolean())
        {
            ByteArray data = pageDeserialization.readByteArray();
            ByteInputStream stream = new ByteInputStream(data.getBuffer(), data.getOffset(), data.getLength());
            
            Deserialization deserialization = new Deserialization(serializationRegistry, stream);
            deserialization.setExtension(batchContext.getExtensionId(), batchContext.getContext());

            RawDbBatchOperationState state = deserialization.readTypedObject(RawDbBatchOperationState.class);
            RawDatabaseConfiguration configuration = transactionManager.getDatabase().getConfiguration();
            return new RawDbBatchOperation(transactionManager.getDatabase().getBatchManager(), transactionManager, 
                transactionManager.getDatabase().getPageTypeManager(), state, false,
                configuration.getBatchRunPeriod(), configuration.getBatchIdlePeriod());
        }
        else
            return null;
    }

    public void setOperation(IRawBatchContext batchContext, RawDbBatchOperationState state)
    {
        RawPageSerialization pageSerialization = new RawPageSerialization(transactionManager.getTransaction(), 
            BATCH_OPERATION_SPACE_FILE_INDEX, headerPage, HEADER_SIZE);
        
        ByteOutputStream stream = new ByteOutputStream();
        Serialization serialization = new Serialization(serializationRegistry, true, stream);
        serialization.setExtension(batchContext.getExtensionId(), batchContext.getContext());
        
        serialization.writeTypedObject(state);
     
        pageSerialization.writeBoolean(true);
        pageSerialization.writeByteArray(new ByteArray(stream.getBuffer(), 0, stream.getLength()));
    }
    
    public void clearOperation()
    {
        RawPageSerialization pageSerialization = new RawPageSerialization(transactionManager.getTransaction(), 
            BATCH_OPERATION_SPACE_FILE_INDEX, headerPage, HEADER_SIZE);
        
        pageSerialization.writeBoolean(false);
    }
    
    private RawBatchOperationSpace(RawTransactionManager transactionManager)
    {
        Assert.notNull(transactionManager);
        
        this.transactionManager = transactionManager;
        this.serializationRegistry = Serializers.createRegistry();
        this.serializationRegistry.register(new RawDbBatchOperationStateSerializer());
        this.headerPage = transactionManager.getTransaction().getPage(BATCH_OPERATION_SPACE_FILE_INDEX, 0);
    }

    private void readHeader()
    {
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionManager.getTransaction(), 
            BATCH_OPERATION_SPACE_FILE_INDEX, headerPage, 0);
        
        short magic = deserialization.readShort();
        byte version = deserialization.readByte();
        
        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, VERSION));
    }

    private void writeHeader()
    {
        RawPageSerialization serialization = new RawPageSerialization(transactionManager.getTransaction(), 
            BATCH_OPERATION_SPACE_FILE_INDEX, headerPage, 0);
        
        serialization.writeShort(MAGIC);
        serialization.writeByte(VERSION);
        serialization.writeBoolean(false);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
        
        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
