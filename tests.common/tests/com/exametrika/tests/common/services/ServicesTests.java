/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.services;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Collections;


/**
 * The {@link ServicesTests} are tests for {@link Services}.
 * 
 * @see Services
 * @author Medvedev-A
 */
public class ServicesTests
{
    public interface ITestService
    {
        
    }
    
    public static class TestService1 implements ITestService
    {
        public final String name;
        public final Set<String> runModes;
        public final JsonObject parameters;

        public TestService1(String name, Set<String> runModes, JsonObject parameters)
        {
            this.name = name;
            this.runModes = runModes;
            this.parameters = parameters;
        }
    }
    
    public static class TestService2 implements ITestService
    {
        public final String name;
        public final Set<String> runModes;
        public final JsonObject parameters;

        public TestService2(String name, Set<String> runModes, JsonObject parameters)
        {
            this.name = name;
            this.runModes = runModes;
            this.parameters = parameters;
        }
        
        public TestService2()
        {
            this.name = null;
            this.runModes = null;
            this.parameters = null;
        }
    }
    
    public static class TestService3 implements ITestService
    {
    }
    
    @Test
    public void testServices() throws Throwable
    {
        List<ITestService> services = Services.loadProviders(ITestService.class, null, null, ServicesTests.class.getClassLoader(), false);
        
        assertThat(services.size(), is(3));
        assertThat(services.get(0), instanceOf(TestService1.class));
        assertThat(((TestService1)services.get(0)).name, is("provider1"));
        assertThat(((TestService1)services.get(0)).runModes, nullValue());
        assertThat(((TestService1)services.get(0)).parameters, is(Json.object()
            .put("class", "com.exametrika.tests.common.services.ServicesTests$TestService1")
            .putArray("runModes").add("mode1").add("mode2").end()
            .putArray("qualifiers").add("q1").add("q2").end()
            .put("param1", "Param1")
            .put("runModeRequired", false)
            .put("qualifiersRequired", false)
            .putArray("param2").add(1).add(2).add(3).end()
            .toObject()));
        
        assertThat(services.get(1), instanceOf(TestService2.class));
        assertThat(((TestService2)services.get(1)).name, is("provider2"));
        assertThat(((TestService2)services.get(1)).runModes, nullValue());
        assertThat(((TestService2)services.get(1)).parameters, is(Json.object()
            .put("class", "com.exametrika.tests.common.services.ServicesTests$TestService2")
            .put("runModeRequired", false)
            .put("qualifiersRequired", false)
            .putArray("runModes").add("mode1&mode2").end()
            .putArray("qualifiers").add("q1").end()
            .putObject("param1").put("arg1", 1).put("arg2", true).put("arg3", "arg").end()
            .toObject()));
        assertThat(services.get(2), instanceOf(TestService3.class));
        
        services = Services.loadProviders(ITestService.class, java.util.Collections.singleton("mode1"), null, 
            getClass().getClassLoader(), false);
        
        assertThat(services.size(), is(2));
        assertThat(services.get(0), instanceOf(TestService1.class));
        assertThat(((TestService1)services.get(0)).name, is("provider1"));
        assertThat(services.get(1), instanceOf(TestService3.class));
        
        services = Services.loadProviders(ITestService.class, java.util.Collections.singleton("mode2"), 
            Collections.asSet("q1", "q2"), getClass().getClassLoader(), false);
        
        assertThat(services.size(), is(1));
        assertThat(services.get(0), instanceOf(TestService1.class));
        assertThat(((TestService1)services.get(0)).name, is("provider1"));
        
        services = Services.loadProviders(ITestService.class, java.util.Collections.singleton("mode2"), 
            Collections.asSet("q1"), getClass().getClassLoader(), false);
        
        assertThat(services.size(), is(1));
        assertThat(services.get(0), instanceOf(TestService1.class));
        assertThat(((TestService1)services.get(0)).name, is("provider1"));
        
        services = Services.loadProviders(ITestService.class, Collections.asSet("mode1", "mode2"), Collections.asSet("q1"),
            getClass().getClassLoader(), false);
        
        assertThat(services.size(), is(2));
        assertThat(services.get(0), instanceOf(TestService1.class));
        assertThat(((TestService1)services.get(0)).name, is("provider1"));
        
        assertThat(services.get(1), instanceOf(TestService2.class));
        assertThat(((TestService2)services.get(1)).name, is("provider2"));
        
        services = Services.loadProviders(ITestService.class, Collections.asSet("mode1"), null, getClass().getClassLoader(), false);
        
        assertThat(services.size(), is(2));
        assertThat(services.get(0), instanceOf(TestService1.class));
        assertThat(((TestService1)services.get(0)).name, is("provider1"));
        assertThat(services.get(1), instanceOf(TestService3.class));
        
        services = Services.loadProviders(ITestService.class, Collections.asSet("mode1"), null, getClass().getClassLoader(), true);
        
        assertThat(services.size(), is(1));
        assertThat(services.get(0), instanceOf(TestService1.class));
        assertThat(((TestService1)services.get(0)).name, is("provider1"));
        
        services = Services.loadProviders(ITestService.class, Collections.asSet("mode5"), null, getClass().getClassLoader(), true);
        assertThat(services.isEmpty(), is(true));
        
        services = Services.loadProviders(ITestService.class, null, Collections.asSet("q5"),
            getClass().getClassLoader(), false);
        assertThat(services.isEmpty(), is(true));
        
        services = Services.loadProviders(ITestService.class, Collections.asSet("mode5"), Collections.asSet("q5"), 
            getClass().getClassLoader(), true);
        
        assertThat(services.size(), is(1));
        assertThat(services.get(0), instanceOf(TestService3.class));
    }
}
