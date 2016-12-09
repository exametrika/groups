/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ThreadInterruptedException;


/**
 * The {@link FileWatcher} is used to watch for changes in specified directories or for specified files.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FileWatcher
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(FileWatcher.class);
    private volatile long watchingPeriod;
    private volatile long fireEventPeriod;
    private volatile LinkedHashMap<WatchId, WatchInfo> watches = new LinkedHashMap<WatchId, WatchInfo>();
    private long lastWatchingTime;

    /**
     * The {@link IFileListener} is listener of {@link FileWatcher} events.
     * 
     * @author medvedev
     */
    public interface IFileListener
    {
        /**
         * Called when changes for specified path has been detected.
         *
         * @param path changed path
         * @param key key associated with path or null if key is not set
         */
        void onChanged(File path, Object key);
    }
    
    /**
     * Creates an object.
     *
     * @param watchingPeriod period in milliseconds to watch file changes
     * @param fireEventPeriod period in milliseconds between last file changes and subsequent firing of file change event
     */
    public FileWatcher(long watchingPeriod, long fireEventPeriod)
    {
        this.watchingPeriod = watchingPeriod;
        this.fireEventPeriod = fireEventPeriod;
    }

    public void setWatchingPeriod(long watchingPeriod)
    {
        this.watchingPeriod = watchingPeriod;
    }
    
    public void setFireEventPeriod(long fireEventPeriod)
    {
        this.fireEventPeriod = fireEventPeriod;
    }
    
    public synchronized void register(File path, IFileListener listener, Object key)
    {
        Assert.notNull(path);
        Assert.notNull(listener);
        
        WatchId id = new WatchId(path, listener);
        if (this.watches.containsKey(id))
            return;
        
        LinkedHashMap<WatchId, WatchInfo> watches = (LinkedHashMap<WatchId, WatchInfo>)this.watches.clone();
        
        WatchInfo info = new WatchInfo(path, listener, key);
        detectChanges(info.fileChanges, info.path);
        
        watches.put(id, info);
        this.watches = watches;
    }
    
    public synchronized void unregister(File path, IFileListener listener)
    {
        Assert.notNull(path);
        Assert.notNull(listener);
        
        WatchId id = new WatchId(path, listener);
        if (!this.watches.containsKey(id))
            return;
        
        LinkedHashMap<WatchId, WatchInfo> watches = (LinkedHashMap<WatchId, WatchInfo>)this.watches.clone();
        watches.remove(id);
        this.watches = watches;
    }
    
    public synchronized void unregisterAll()
    {
        this.watches = new LinkedHashMap<WatchId, WatchInfo>();
    }
    
    public void onTimer()
    {
        long currentTime = Times.getCurrentTime();
        if (currentTime < lastWatchingTime + watchingPeriod)
            return;
        
        lastWatchingTime = currentTime;
        
        for (WatchInfo info : watches.values())
        {
            if (detectChanges(info.fileChanges, info.path))
                info.nextDetectionTime = currentTime + fireEventPeriod;
            
            if (info.nextDetectionTime != 0 && currentTime > info.nextDetectionTime)
            {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.pathChanged(info.path));
                
                try
                {
                    info.listener.onChanged(info.path, info.key);
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
                        logger.log(LogLevel.ERROR, e);
                }
                
                info.nextDetectionTime = 0;
            }
        }
    }

    private boolean detectChanges(Map<File, FileInfo> changesMap, File dir)
    {
        if (!dir.isDirectory())
            return false;
        
        boolean modified = false;
        for (File file : dir.listFiles())
        {
            if (file.isDirectory())
                modified = detectChanges(changesMap, file) || modified;
            else
            {
                long lastModified = file.lastModified();
                FileInfo info = changesMap.get(file);
                if (info == null)
                {
                    info = new FileInfo();
                    info.lastModified = lastModified;
                    changesMap.put(file, info);
                    
                    modified = true;
                }
                else if (info.lastModified != lastModified)
                {
                    info.lastModified = lastModified;
                    modified = true;
                }
            }
        }
        
        for (Iterator<Entry<File, FileInfo>> it = changesMap.entrySet().iterator(); it.hasNext(); )
        {
            File file = it.next().getKey();
            if (!file.exists())
            {
                it.remove();
                modified = true;
            }
        }

        return modified;
    }
    
    private static class FileInfo
    {
        long lastModified;
    }
    
    private static class WatchId
    {
        private final File path;
        private final IFileListener listener;

        public WatchId(File path, IFileListener listener)
        {
            Assert.notNull(path);
            Assert.notNull(listener);
            
            this.path = path;
            this.listener = listener;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof WatchId))
                return false;
            
            WatchId id = (WatchId)o;
            return path.equals(id.path) && listener.equals(id.listener);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(path, listener);
        }
    }
    
    private static class WatchInfo
    {
        private final File path;
        private final IFileListener listener;
        private final Object key;
        private final Map<File, FileInfo> fileChanges = new HashMap<File, FileInfo>();
        private long nextDetectionTime;
        
        public WatchInfo(File path, IFileListener listener, Object key)
        {
            Assert.notNull(path);
            Assert.notNull(listener);
            
            this.path = path;
            this.listener = listener;
            this.key = key;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Path ''{0}'' has been changed.")
        ILocalizedMessage pathChanged(File path);
    }
}
