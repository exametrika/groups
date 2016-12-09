/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.FileWatcher.IFileListener;




/**
 * The {@link PathSynchronizer} is used to maintain archive of specified path, updating archive when contents of original path are changed.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PathSynchronizer implements IFileListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(PathSynchronizer.class);
    private final FileWatcher fileWatcher;

    public PathSynchronizer(long watchingPeriod, long fireEventPeriod)
    {
        this.fileWatcher = new FileWatcher(watchingPeriod, fireEventPeriod);
    }
    
    public void setWatchingPeriod(long watchingPeriod)
    {
        fileWatcher.setWatchingPeriod(watchingPeriod);
    }
    
    public void setFireEventPeriod(long fireEventPeriod)
    {
        fileWatcher.setFireEventPeriod(fireEventPeriod);
    }
    
    public void onTimer()
    {
        fileWatcher.onTimer();
    }

    @Override
    public void onChanged(File path, Object key)
    {
        WatchKey watch = (WatchKey)key;
        synchronizePath(path, watch.archivePath, watch.hashesPath, null, null, false);
        
        watch.listener.onChanged(path, watch.key);
    }
    
    public void synchronizePath(File path, File archivePath, File hashesPath, IFileListener listener, Object key)
    {
        Assert.notNull(archivePath);
        Assert.notNull(hashesPath);
        Assert.notNull(listener);
        
        synchronizePath(path, archivePath, hashesPath, listener, key, true);
    }
    
    public String getArchiveHash(File hashesPath)
    {
        PathHashes hashes = loadPathHashes(hashesPath);
        if (hashes != null)
            return hashes.archiveHash;
        
        return null;
    }
    
    public void reset()
    {
        fileWatcher.unregisterAll();
    }

    private synchronized void synchronizePath(File path, File archivePath, File hashesPath, IFileListener listener, 
        Object key, boolean register)
    {
        if (path != null && path.exists())
        {
            PathHashes hashes = loadPathHashes(hashesPath);
            
            String dirHash = Files.md5Hash(path);

            if (!Objects.equals(dirHash, hashes.dirHash))
            {
                boolean archiveExists = archivePath.delete();
                
                Files.zip(path, archivePath);
    
                hashes.dirHash = dirHash;
                hashes.archiveHash = Files.md5Hash(archivePath);
                
                savePathHashes(hashesPath, hashes);
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                {
                    if (!archiveExists)
                        logger.log(LogLevel.DEBUG, messages.archiveCreated(path, archivePath, dirHash, hashes.archiveHash));
                    else
                        logger.log(LogLevel.DEBUG, messages.archiveUpdated(path, archivePath, dirHash, hashes.archiveHash));
                }
            }
            
            if (register)
                fileWatcher.register(path, this, new WatchKey(archivePath, hashesPath, listener, key));
        }
        else
        {
            archivePath.delete();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.archiveDeleted(path, archivePath));
        }
    }

    private PathHashes loadPathHashes(File hashesPath)
    {
        PathHashes hashes = new PathHashes();
        
        if (hashesPath.exists())
        {
            try
            {
                JsonObject hashesObject = JsonSerializers.read(IOs.read(new FileInputStream(hashesPath), "UTF-8"), false);
                
                hashes.dirHash = hashesObject.get("dirHash", null);
                hashes.archiveHash = hashesObject.get("archiveHash", null);
            }
            catch (IOException e)
            {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }

        return hashes;
    }
    
    private void savePathHashes(File hashesPath, PathHashes hashes)
    {
        FileWriter writer = null;
        try
        {
            
            JsonObject hashesObject = Json.object().put("dirHash", hashes.dirHash).put("archiveHash", hashes.archiveHash).toObject();
            
            writer = new FileWriter(hashesPath);
            JsonSerializers.write(writer, hashesObject, true);
        }
        catch (IOException e)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
        finally
        {
            IOs.close(writer);
        }
    }
    
    private static class PathHashes
    {
        String dirHash;
        String archiveHash;
    }
    
    private static class WatchKey
    {
        private final File archivePath;
        private final File hashesPath;
        private final IFileListener listener;
        private final Object key;

        public WatchKey(File archivePath, File hashesPath, IFileListener listener, Object key)
        {
            this.archivePath = archivePath;
            this.hashesPath = hashesPath;
            this.listener = listener;
            this.key = key;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Archive ''{0}'' of ''{1}'' has been created. Path hash: {2}, archive hash: {3}.")
        ILocalizedMessage archiveCreated(File path, File archivePath, String dirHash, String archiveHash);
        @DefaultMessage("Archive ''{0}'' of ''{1}'' has been updated. Path hash: {2}, archive hash: {3}.")
        ILocalizedMessage archiveUpdated(File path, File archivePath, String dirHash, String archiveHash);
        @DefaultMessage("Archive ''{0}'' of ''{1}'' has been deleted.")
        ILocalizedMessage archiveDeleted(File path, File archivePath);
    }
}
