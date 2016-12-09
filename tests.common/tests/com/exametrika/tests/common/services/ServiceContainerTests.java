/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.services;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.services.impl.ChildServiceContainer;
import com.exametrika.common.services.impl.ServiceContainer;
import com.exametrika.common.utils.Collections;


/**
 * The {@link ServiceContainerTests} are tests for {@link ServiceContainer} implementations.
 * 
 * @see ServiceContainer
 * @author Medvedev-A
 */
public class ServiceContainerTests
{
    public static class TestService implements IService, IServiceProvider
    {
        public boolean started;
        public boolean stopped;
        public boolean configurationUpdated;
        public boolean timered;
        
        @Override
        public void register(IServiceRegistrar registrar)
        {
            registrar.register("testService", this);
        }

        @Override
        public void wire(IServiceRegistry registry)
        {
        }
        
        @Override
        public void start(IServiceRegistry registry)
        {
            started = true;
        }

        @Override
        public void stop(boolean fromShutdownHook)
        {
            stopped = true;
        }

        @Override
        public void setConfiguration(ILoadContext context)
        {
            assertThat(context != null, is(true));
            configurationUpdated = true;
        }
        
        @Override
        public void onTimer(long currentTime)
        {
            timered = true;
        }
    }
    
    public static class TestService2 implements IService, IServiceProvider
    {
        public boolean started;
        public boolean stopped;
        public boolean configurationUpdated;
        public boolean timered;
        
        @Override
        public void register(IServiceRegistrar registrar)
        {
            registrar.register("testService2", this);
        }

        @Override
        public void wire(IServiceRegistry registry)
        {
        }
        
        @Override
        public void start(IServiceRegistry registry)
        {
            started = true;
        }

        @Override
        public void stop(boolean fromShutdownHook)
        {
            stopped = true;
        }

        @Override
        public void setConfiguration(ILoadContext context)
        {
            assertThat(context != null, is(true));
            configurationUpdated = true;
        }
        
        @Override
        public void onTimer(long currentTime)
        {
            timered = true;
        }
    }
    
    @Test
    public void testContainer() throws Throwable
    {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "value");
        ServiceContainer container = new ServiceContainer(parameters, java.util.Collections.<File>emptyList());
        
        assertThat(container.findParameter("key").equals("value"), is(true));
        container.start("inline:{}");
        
        TestService service = container.findService("testService");
        assertThat(service.started, is(true));
        assertThat(service.stopped, is(false));
        assertThat(service.configurationUpdated, is(true));
        service.configurationUpdated = false;
        
        container.setConfiguration("inline:{}");
        
        assertThat(service.configurationUpdated, is(true));
        
        Thread.sleep(500);
        assertThat(service.timered, is(true));
        
        container.stop(false);
        
        assertThat(service.stopped, is(true));
        
        assertThat(container.findService("testService"), nullValue());
    }
    
    @Test
    public void testChildContainer() throws Throwable
    {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "value");
        ServiceContainer container = new ServiceContainer(parameters, java.util.Collections.<File>emptyList());
        
        Map<String, Object> parameters2 = new HashMap<String, Object>();
        parameters2.put("key2", "value2");
        ChildServiceContainer child = new ChildServiceContainer("child", parameters2, Collections.asSet("mode1"), 
            Collections.asSet("q1", "q2"), getClass().getClassLoader(), container);
        
        assertThat(child.findParameter("key").equals("value"), is(true));
        assertThat(child.findParameter("key2").equals("value2"), is(true));
        container.start("inline:{}");
        child.start("inline:{}");
        
        TestService service = child.findService("testService");
        assertThat(service.started, is(true));
        assertThat(service.stopped, is(false));
        assertThat(service.configurationUpdated, is(true));
        service.configurationUpdated = false;
        
        TestService2 service2 = child.findService("testService2");
        assertThat(service2.started, is(true));
        assertThat(service2.stopped, is(false));
        assertThat(service2.configurationUpdated, is(true));
        service2.configurationUpdated = false;
        
        child.setConfiguration("inline:{}");
        
        assertThat(service2.configurationUpdated, is(true));
        
        Thread.sleep(500);
        assertThat(service2.timered, is(true));
        
        child.stop(false);
        
        assertThat(service2.stopped, is(true));
        
        assertThat(child.findService("testService2"), nullValue());
    }
}