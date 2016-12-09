/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.utils.Fields.IField;


/**
 * The {@link Files} contains different utility methods for files.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Files
{
    public static final boolean nativeAvailable;
    private static final IField descriptorField = Fields.get(FileDescriptor.class, "fd");

    static
    {
        try
        {
            System.loadLibrary("exaj");
        }
        catch (UnsatisfiedLinkError e)
        {
        }

        boolean available = false;
        try
        {
            posixAdvise(0, 0, 0, 0);
            available = true;
        }
        catch (UnsatisfiedLinkError e)
        {
        }

        nativeAvailable = available;
    }

    /** Type of advice. */
    public enum AdviceType
    {
        POSIX_FADV_NORMAL,
        
        POSIX_FADV_SEQUENTIAL,
        
        POSIX_FADV_RANDOM,
        
        POSIX_FADV_WILLNEED,
        
        POSIX_FADV_DONTNEED
    }
    
    /**
     * Moves source into destination.
     *
     * @param source source path
     * @param destination destination path
     */
    public static void move(File source, File destination)
    {
        Assert.notNull(source);
        Assert.isTrue(source.exists());
        Assert.notNull(destination);
        
        if (destination.exists())
        {
            if (destination.isDirectory())
                emptyDir(destination);
            
            Assert.checkState(destination.delete());
        }
        
        copy(source, destination);
        emptyDir(source);
        source.delete();
        //Assert.checkState(source.renameTo(destination));
    }
    
    /**
     * Normalizes specified path.
     *
     * @param path path
     * @return normalized path
     */
    public static String normalize(String path)
    {
        try
        {
            return new File(path).getCanonicalPath();
        }
        catch (IOException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    /**
     * Builds path of file relative to specified base path.
     *
     * @param file file
     * @param base base path
     * @return relative file path or null if file is not contained in base path
     */
    public static String relativize(String file, String base)
    {
        file = new File(file).getPath();
        base = new File(base).getPath();
        
        if (!file.startsWith(base))
            return null;
        
        return file.substring(base.length() + 1);
    }
    
    /**
     * Creates temporary directory with specified prefix.
     *
     * @param prefix directory prefix
     * @return temporary directory
     */
    public static File createTempDirectory(String prefix)
    {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = prefix + "-" + Times.getCurrentTime() + "-";

        for (int counter = 0; counter < 10000; counter++) 
        {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) 
                return tempDir;
        }
        
        Assert.checkState(false);
        return null;
    }
    
    /**
     * Creates temporary file with specified prefix and suffix.
     *
     * @param prefix file prefix
     * @param suffix file suffix
     * @return temporary file
     */
    public static File createTempFile(String prefix, String suffix)
    {
        try
        {
            return File.createTempFile(prefix, suffix);
        }
        catch (IOException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    /**
     * Deletes file or directory. Directory may be non-empty.
     *
     * @param file file or directory to delete
     */
    public static void delete(File file)
    {
        if (file == null)
            return;
        
        if (file.isDirectory())
            emptyDir(file);
        
        file.delete();
    }
    
    /**
     * Returns size of file or directory.
     *
     * @param file file or directory
     * @return size of file or directory
     */
    public static long getSize(File file)
    {
        Assert.notNull(file);
        
        long size = 0;
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
                size += getSize(files[i]);
        }
        else
            size = file.length();
        
        return size;
    }
    
    /**
     * Empties specified directory.
     *
     * @param dir directory
     */
    public static void emptyDir(File dir)
    {
        Assert.notNull(dir);
        
        if (!dir.isDirectory())
            return;
        
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];
            if (file.isDirectory())
                emptyDir(file);
            
            file.delete();
        }
    }
    
    /**
     * Copies source file or directory to specified destination.
     *
     * @param source source file or directory
     * @param destination file or directory
     */
    public static void copy(File source, File destination)
    {
        copy(source, destination, null);
    }
    
    /**
     * Copies source file or directory to specified destination.
     *
     * @param source source file or directory
     * @param destination file or directory
     * @param filter filter. Can be null if not used
     */
    public static void copy(File source, File destination, FileFilter filter)
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        Assert.isTrue(source.exists());
        
        if (filter != null && !filter.accept(source))
            return;
        
        if (source.isFile())
            copyFile(source, destination);
        else
            copyDir(source, destination, filter);
    }
    
    /**
     * Computes MD5 hash of the specified file or directory.
     *
     * @param file file or directory
     * @return MD5 hash
     */
    public static String md5Hash(File file)
    {
        Assert.notNull(file);
        Assert.isTrue(file.exists());
        
        try 
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1000];
            md5Hash(digest, file, buffer);
            
            return Strings.digestToString(digest.digest());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Computes MD5 hash of the specified files or directories.
     *
     * @param files files or directories
     * @return MD5 hash
     */
    public static String md5Hash(List<File> files)
    {
        Assert.notNull(files);
        
        try 
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1000];
            for (File file : files)
            {
                Assert.isTrue(file.exists());
                md5Hash(digest, file, buffer);
            }
            
            return Strings.digestToString(digest.digest());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates zip archive.
     *
     * @param source source file or directory to be compressed. Only contents of specified directory are included in archive.
     * @param destination zip archive
     */
    public static void zip(File source, File destination)
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        Assert.isTrue(source.exists());
        
        byte[] buffer = new byte[0x10000];
        
        FileOutputStream fileStream = null;
        ZipOutputStream stream = null;
        try
        {
            fileStream = new FileOutputStream(destination);
            stream = new ZipOutputStream(new BufferedOutputStream(fileStream));
            zip(stream, source, "", buffer, true);
            IOs.close(stream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(stream);
            IOs.close(fileStream);
        }
    }
    
    /**
     * Creates zip archive.
     *
     * @param sources source files or directories to be compressed
     * @param destination zip archive
     */
    public static void zip(List<File> sources, File destination)
    {
        Assert.notNull(sources);
        Assert.notNull(destination);
        
        byte[] buffer = new byte[0x10000];
        
        FileOutputStream fileStream = null;
        
        try
        {
            fileStream = new FileOutputStream(destination);
            ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(fileStream));
            
            for (File source : sources)
            {
                Assert.isTrue(source.exists());
                zip(stream, source, "", buffer, false);
            }
            
            IOs.close(stream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(fileStream);
        }
    }
    
    /**
     * Unzips contents of zip archive.
     *
     * @param source zip archive
     * @param destination destination directory to unzip contents to
     */
    public static void unzip(File source, File destination)
    {
        Assert.notNull(source);
        Assert.isTrue(source.isFile());
        Assert.notNull(destination);
        Assert.isTrue(source.exists());
        if (!destination.exists())
            destination.mkdirs();
        Assert.isTrue(destination.isDirectory());
        
        FileInputStream fileStream = null;
        
        try
        {
            fileStream = new FileInputStream(source);
            ZipInputStream stream = new ZipInputStream(new BufferedInputStream(fileStream));
            while (true)
            {
                ZipEntry entry = stream.getNextEntry();
                if (entry == null)
                    break;
                
                if (entry.isDirectory())
                    new File(destination, entry.getName()).mkdirs();
                else
                {
                    FileOutputStream fileOut = null;
                    try
                    {
                        fileOut = new FileOutputStream(new File(destination, entry.getName()));
                        IOs.copy(stream, fileOut);
                    }
                    finally
                    {
                        IOs.close(fileOut);
                    }
                }
            }
            IOs.close(stream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(fileStream);
        }
    }
    
    /**
     * Creates jar archive.
     *
     * @param source source file or directory to be compressed. Only contents of specified directory are included in archive.
     * @param destination jar archive
     * @param manifest manifest
     */
    public static void jar(File source, File destination, Manifest manifest)
    {
        Assert.notNull(source);
        Assert.notNull(destination);
        Assert.notNull(manifest);
        Assert.isTrue(source.exists());
        
        byte[] buffer = new byte[0x10000];
        
        FileOutputStream fileStream = null;
        JarOutputStream stream = null;
        try
        {
            fileStream = new FileOutputStream(destination);
            stream = new JarOutputStream(new BufferedOutputStream(fileStream), manifest);
            zip(stream, source, "", buffer, true);
            IOs.close(stream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(stream);
            IOs.close(fileStream);
        }
    }
    
    /**
     * Unpacks contents of jar archive.
     *
     * @param source jar archive
     * @param destination destination directory to unjar contents to
     * @return manifest
     */
    public static Manifest unjar(File source, File destination)
    {
        return unjar(source, destination, null);
    }
    
    /**
     * Unpacks contents of jar archive.
     *
     * @param source jar archive
     * @param destination destination directory to unjar contents to
     * @param filter filter. Can be null if not used
     * @return manifest
     */
    public static Manifest unjar(File source, File destination, ICondition<String> filter)
    {
        Assert.notNull(source);
        Assert.isTrue(source.isFile());
        Assert.notNull(destination);
        Assert.isTrue(source.exists());
        if (!destination.exists())
            destination.mkdirs();
        Assert.isTrue(destination.isDirectory());
        
        FileInputStream fileStream = null;
        
        try
        {
            fileStream = new FileInputStream(source);
            JarInputStream stream = new JarInputStream(new BufferedInputStream(fileStream));
            while (true)
            {
                ZipEntry entry = stream.getNextEntry();
                if (entry == null)
                    break;
                if (filter != null && !filter.evaluate(entry.getName()))
                    continue;
                
                if (entry.isDirectory())
                    new File(destination, entry.getName()).mkdirs();
                else
                {
                    FileOutputStream fileOut = null;
                    try
                    {
                        File file = new File(destination, entry.getName());
                        if (!file.getParentFile().exists())
                            file.getParentFile().mkdirs();
                        
                        fileOut = new FileOutputStream(file);
                        IOs.copy(stream, fileOut);
                    }
                    finally
                    {
                        IOs.close(fileOut);
                    }
                }
            }
            IOs.close(stream);
            
            return stream.getManifest();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(fileStream);
        }
    }
    
    /**
     * Checks support of sync all functionality.
     *
     * @return true if sync all is supported
     */
    public static boolean isSyncAllSupported()
    {
        if (nativeAvailable)
            return isSyncSupported();
        else
            return false;
    }
    
    /**
     * Synchronizes contents of disk cache with underlying storage device.
     * 
     * @return true if operation has beed succeeded
     */
    public static boolean syncAll()
    {
        if (nativeAvailable)
        {
            sync();
            return true;
        }
        else
            return false;
    }
    
    /**
     * Performs advice to IO operation. Works as posix_advise function.
     * 
     * @param descriptor file descriptor
     * @param offset file region offset
     * @param length file region length
     * @param adviceType advice type
     * @return true if operation has beed succeeded
     */
    public static boolean advise(FileDescriptor descriptor, long offset, long length, AdviceType adviceType)
    {
        Assert.notNull(descriptor);
        Assert.isTrue(offset >= 0 && length >= 0);
        Assert.notNull(adviceType);
        
        if (nativeAvailable && descriptorField != null)
        {
            int advice;
            switch (adviceType)
            {
            case POSIX_FADV_NORMAL:
                advice = 0;
                break;
            case POSIX_FADV_RANDOM:
                advice = 1;
                break;
            case POSIX_FADV_SEQUENTIAL:
                advice = 2;
                break;
            case POSIX_FADV_WILLNEED:
                advice = 3;
                break;
            case POSIX_FADV_DONTNEED:
                advice = 4;
                break;
            default:
                return Assert.error();
            }
            int fd = descriptorField.getInt(descriptor);
            posixAdvise(fd, offset, length, advice);
            return true;
        }
        else
            return false;
    }
    
    public static String read(File file)
    {
        try
        {
            return IOs.read(new FileInputStream(file), "UTF-8");
        }
        catch (IOException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    public static ByteArray readBytes(File file)
    {
        InputStream in = null;
        try
        {
            in = new BufferedInputStream(new FileInputStream(file));
            ByteOutputStream out = new ByteOutputStream();
            IOs.copy(in, out);
            return new ByteArray(out.getBuffer(), 0, out.getLength());
        }
        catch (IOException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
        finally
        {
            IOs.close(in);
        }
    }
    
    public static void write(File file, String value)
    {
        try
        {
            IOs.write(new FileOutputStream(file), value, "UTF-8");
        }
        catch (IOException e)
        {
            Exceptions.wrapAndThrow(e);
        }
    }
    
    public static void writeBytes(File file, ByteArray value)
    {
        OutputStream out = null;
        try
        {
            out = new BufferedOutputStream(new FileOutputStream(file));
            ByteInputStream in = new ByteInputStream(value.getBuffer(), value.getOffset(), value.getLength());
            IOs.copy(in, out);
        }
        catch (IOException e)
        {
            Exceptions.wrapAndThrow(e);
        }
        finally
        {
            IOs.close(out);
        }
    }
    
    private static void copyDir(File sourceDir, File destinationDir, FileFilter filter)
    {
        destinationDir.mkdirs();
        File[] files = sourceDir.listFiles(filter);
        for (int i = 0; i < files.length; i++)
        {
            File sourceFile = files[i];
            File destinationFile = new File(destinationDir, sourceFile.getName());
            if (sourceFile.isDirectory())
                copyDir(sourceFile, destinationFile, filter);
            else
                copyFile(sourceFile, destinationFile);
        }
    }

    private static void copyFile(File source, File destination)
    {
        destination.getParentFile().mkdirs();
        
        InputStream in = null;
        OutputStream out = null;
        
        try
        {
            in = new BufferedInputStream(new FileInputStream(source));
            out = new BufferedOutputStream(new FileOutputStream(destination));
            
            IOs.copy(in, out);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOs.close(in);
            IOs.close(out);
        }
    }
    
    private static void md5Hash(MessageDigest digest, File file, byte[] buffer)
    {
        if(file.isDirectory())
        {
            File[] files = file.listFiles();
            Arrays.sort(files, null);
            for(File subFile : files)
                md5Hash(digest, subFile, buffer);
        }
        else
        {
            InputStream stream = null;
            try 
            {
                stream = new FileInputStream(file);
                stream = new DigestInputStream(new BufferedInputStream(stream), digest);
                
                while (stream.read(buffer) != -1);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            finally 
            {
                IOs.close(stream);
            }
        }
    }
    
    private static void zip(ZipOutputStream zipStream, File file, String path, byte[] buffer, boolean contentsOnly) throws IOException
    {
        if(file.isDirectory())
        {
            String subPath;
            if (!contentsOnly)
            {
                subPath = path + file.getName() + "/";
                ZipEntry entry = new ZipEntry(subPath);
                zipStream.putNextEntry(entry);
            }
            else
            {
                Assert.isTrue(path.isEmpty());
                subPath = path;
            }
            
            for(File subFile : file.listFiles())
                zip(zipStream, subFile, subPath, buffer, false);

            if (!contentsOnly)
                zipStream.closeEntry();
        }
        else
        {
            FileInputStream fileStream = null;
            InputStream stream = null;
            try
            {
                fileStream = new FileInputStream(file);
                stream = new BufferedInputStream(fileStream);
            
                ZipEntry entry = new ZipEntry(path + file.getName());
                zipStream.putNextEntry(entry);
                
                while(true)
                {
                    int length = stream.read(buffer);
                    if (length == -1)
                        break;
                    
                    zipStream.write(buffer, 0, length);
                }
                
                zipStream.closeEntry();
            }
            finally
            {
                IOs.close(stream);
                IOs.close(fileStream);
            }
        }
    }
    
    private Files()
    {
    }
    
    private static native boolean isSyncSupported();
    private static native void sync();
    private static native void posixAdvise(int fd, long offset, long length, int advice);
}
