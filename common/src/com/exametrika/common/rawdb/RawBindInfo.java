/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

/**
 * The {@link RawBindInfo} is a data file bind info.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawBindInfo
{
    /** Read-only flag. If flag is set file is read-only even for writable transaction. */
    public static final int READONLY = 0x1;
    /** Temporary flag. If flag is set temporarily binds file to current transaction. 
     *  Removes temporarily bound file from file cache on transaction completion.
     *  Can be used for file deletion or truncation only when full binding information is not available. */
    public static final int TEMPORARY = 0x2;
    /** Preload data flag. Preloads all file data at first access to page of file. */
    public static final int PRELOAD = 0x4;
    /** No-preload data flag. Disables preloading of all file data at first access to page of file. */
    public static final int NOPRELOAD = 0x8;
    /** Data file is owner of it's directory. Directory of file is deleted when file is deleted. */
    public static final int DIRECTORY_OWNER = 0x10;
    
    private int pathIndex;
    private String name;
    private int pageTypeIndex;
    private String categoryType;
    private String category;
    private long maxFileSize;
    private int flags;
    
    /** 
     * Returns index of database main path where file resides.
     *  
     * @return index of database main path where file resides.
     */
    public int getPathIndex()
    {
        return pathIndex;
    }
    
    /** 
     * Sets index of database main path where file resides.
     * 
     * @param pathIndex index of database main path where file resides
     */
    public void setPathIndex(int pathIndex)
    {
        this.pathIndex = pathIndex;
    }
    
    /** 
     * Returns file name (excluding path) in database directory or null if default name is used.
     * 
     * @return file name (excluding path) in database directory or null if default name is used
     */
    public String getName()
    {
        return name;
    }
    
    /** 
     * Sets file name (excluding path) in database directory or null if default name is used.
     * 
     * @param name file name (excluding path) in database directory or null if default name is used
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    /** 
     * Return index of type of file pages.
     * 
     * @return index of type of file pages
     */
    public int getPageTypeIndex()
    {
        return pageTypeIndex;
    }
    
    /** 
     * Sets index of type of file pages.
     * 
     * @param pageTypeIndex index of type of file pages
     */
    public void setPageTypeIndex(int pageTypeIndex)
    {
        this.pageTypeIndex = pageTypeIndex;
    }
    
    /** 
     * Returns page category type pages of file belongs to. If category type is null or empty string default category type is used.
     * 
     * @return page category type pages of file belongs to. If category type is null or empty string default category is used
     */
    public String getCategoryType()
    {
        return categoryType;
    }
    
    /** 
     * Sets page category type pages of file belongs to. If category type is null or empty string default category type is used.
     * 
     * @param category page category type pages of file belongs to. If category type is null or empty string default category type is used
     */
    public void setCategoryType(String category)
    {
        this.categoryType = category;
    }
    
    /** 
     * Returns page category pages of file belongs to. If category is null or empty string default category is used.
     * 
     * @return page category pages of file belongs to. If category is null or empty string default category is used
     */
    public String getCategory()
    {
        return category;
    }
    
    /** 
     * Sets page category pages of file belongs to. If category is null or empty string default category is used.
     * 
     * @param category page category pages of file belongs to. If category is null or empty string default category is used
     */
    public void setCategory(String category)
    {
        this.category = category;
    }
    
    /** 
     * Returns maximum size of file. If max file size is 0 then default database max file size is used.
     * 
     * @return maximum size of file. If max file size is 0 then default database max file size is used
     */
    public long getMaxFileSize()
    {
        return maxFileSize;
    }
    
    /** 
     * Sets maximum size of file. If max file size is 0 then default database max file size is used.
     * 
     * @param maxFileSize maximum size of file. If max file size is 0 then default database max file size is used
     */
    public void setMaxFileSize(long maxFileSize)
    {
        this.maxFileSize = maxFileSize;
    }
    
    /** 
     * Returns bind flags.
     * 
     * @return bind flags
     */
    public int getFlags()
    {
        return flags;
    }
    
    /** 
     * Sets bind flags.
     * 
     * @param flags bind flags
     */
    public void setFlags(int flags)
    {
        this.flags = flags;
    }
}