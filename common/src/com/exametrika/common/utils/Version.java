/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.Serializable;


/**
 * The {@link Version} represents a semantic version accordingly to <a href="http://semver.org"/>http://semver.org</a>.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Version implements Comparable<Version>, Serializable
{
    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetadata;
    
    /**
     * Creates a new version.
     * 
     * @param major major version number
     * @param minor minor version number
     * @param patch patch number
     */
    public Version(int major, int minor, int patch)
    {
        this(major, minor, patch, null, null);
    }
    
    /**
     * Creates a new version.
     * 
     * @param major major version number
     * @param minor minor version number
     * @param patch patch number
     * @param preRelease optional pre-release suffix. Can be null
     * @param buildMetadata optional build metadata suffix. Can be null
     */
    public Version(int major, int minor, int patch, String preRelease, String buildMetadata)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.buildMetadata = buildMetadata;
    }
    
    /**
     * Returns major version number.
     *
     * @return major version number
     */
    public int getMajor()
    {
        return major;
    }
    
    /**
     * Returns minor version number.
     *
     * @return minor version number
     */
    public int getMinor()
    {
        return minor;
    }
    
    /**
     * Returns patch number.
     *
     * @return patch number
     */
    public int getPatch()
    {
        return patch;
    }
    
    /**
     * Returns pre-release suffix.
     *
     * @return pre-release suffix or null if version does not have a pre-release suffix
     */
    public String getPreRelease()
    {
        return preRelease;
    }
    
    /**
     * Returns build metadata suffix.
     *
     * @return build metadata suffix or null if version does not have a build metadata suffix
     */
    public String getBuildMetadata()
    {
        return buildMetadata;
    }
    
    /**
     * Is current version compatible with given required version?
     *
     * @param version version to check with
     * @return true if versions are compatible
     */
    public boolean isCompatible(Version version)
    {
        Assert.notNull(version);
        
        if (major != version.major)
            return false;
        
        return compareTo(version) >= 0;
    }
    
    @Override
    public int compareTo(Version o)
    {
        if (o == null)
            return -1;

        if (major > o.major)
            return 1;
        else if (major < o.major)
            return -1;
        else if (minor > o.minor)
            return 1;
        else if (minor < o.minor)
            return -1;
        else if (patch > o.patch)
            return 1;
        else if (patch < o.patch)
            return -1;
        else if (Objects.equals(preRelease, o.preRelease))
            return 0;
        else if (preRelease == null)
            return 1;
        else if (o.preRelease == null)
            return -1;
        else
            return preRelease.compareTo(o.preRelease);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        
        if (!(o instanceof Version))
            return false;
        
        Version version = (Version)o;

        return major == version.major && minor == version.minor && patch == version.patch && Objects.equals(preRelease, version.preRelease);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(major, minor, patch, preRelease);
    }
    
    public static Version parse(String value)
    {
        Assert.notNull(value);
        
        String buildMetadata = null;
        int pos = value.lastIndexOf('+');
        if (pos != -1)
        {
            buildMetadata = value.substring(pos + 1);
            value = value.substring(0, pos);
        }
        
        String preRelease = null;
        pos = value.lastIndexOf('-');
        if (pos != -1)
        {
            preRelease = value.substring(pos + 1);
            value = value.substring(0, pos);
        }
        
        String[] parts = value.split("[.]");
        Assert.isTrue(parts.length >= 1 && parts.length <= 3);
        
        int major = Integer.parseInt(parts[0]);
        
        int minor = 0;
        if (parts.length >= 2)
            minor = Integer.parseInt(parts[1]);
        
        int patch = 0;
        if (parts.length == 3)
            patch = Integer.parseInt(parts[2]);
        
        return new Version(major, minor, patch, preRelease, buildMetadata);
    }
    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(major);
        buf.append('.');
        buf.append(minor);
        buf.append('.');
        buf.append(patch);
        if (preRelease != null)
        {
            buf.append('-');
            buf.append(preRelease);
        }
        if (buildMetadata != null)
        {
            buf.append('+');
            buf.append(buildMetadata);
        }
 
        return buf.toString();
    }
}
