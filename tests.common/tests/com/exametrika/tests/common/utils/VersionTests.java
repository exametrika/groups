/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.utils.Strings;
import com.exametrika.common.utils.Version;


/**
 * The {@link VersionTests} are tests for {@link Version}.
 * 
 * @see Strings
 * @author Medvedev-A
 */
public class VersionTests
{
    @Test
    public void testVersion() throws Throwable
    {
        Version version = new Version(1, 2, 3, "pre", "build");
        assertThat(version.toString(), is("1.2.3-pre+build"));
        version = new Version(1, 2, 3, "pre", null);
        assertThat(version.toString(), is("1.2.3-pre"));
        version = new Version(1, 2, 3, null, "build");
        assertThat(version.toString(), is("1.2.3+build"));
        version = new Version(1, 2, 3);
        assertThat(version.toString(), is("1.2.3"));
        
        assertThat(new Version(1, 0, 0).compareTo(new Version(0, 1, 2)) > 0, is(true));
        assertThat(new Version(1, 2, 0).compareTo(new Version(1, 1, 2)) > 0, is(true));
        assertThat(new Version(1, 2, 3).compareTo(new Version(1, 2, 2)) > 0, is(true));
        assertThat(new Version(1, 2, 3).compareTo(new Version(1, 2, 3, "pre", null)) > 0, is(true));
        assertThat(new Version(1, 2, 3, "pre", "build"), is(new Version(1, 2, 3, "pre", null)));
        
        assertThat(new Version(1, 2, 0).isCompatible(new Version(1, 1, 2)), is(true));
        assertThat(new Version(1, 2, 0).isCompatible(new Version(1, 2, 2)), is(false));
        assertThat(new Version(1, 2, 0).isCompatible(new Version(2, 1, 2)), is(false));
        
        assertThat(Version.parse("1"), is(new Version(1, 0, 0)));
        assertThat(Version.parse("10.20"), is(new Version(10, 20, 0)));
        assertThat(Version.parse("10.20.30"), is(new Version(10, 20, 30)));
        assertThat(Version.parse("10.20.30+build"), is(new Version(10, 20, 30)));
        assertThat(Version.parse("10.20.30+build").getBuildMetadata(), is("build"));
        assertThat(Version.parse("10.20.30-pre"), is(new Version(10, 20, 30, "pre", null)));
        assertThat(Version.parse("10.20.30-pre+build"), is(new Version(10, 20, 30, "pre", null)));
        assertThat(Version.parse("10.20.30-pre+build").getBuildMetadata(), is("build"));
    }
}
