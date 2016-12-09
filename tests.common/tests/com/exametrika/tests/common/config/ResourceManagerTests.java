/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.config;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.config.resource.ClassPathResourceLoader;
import com.exametrika.common.config.resource.CompositeResourceLoader;
import com.exametrika.common.config.resource.ContextResourceLoader;
import com.exametrika.common.config.resource.ContextResourceManager;
import com.exametrika.common.config.resource.FileResourceLoader;
import com.exametrika.common.config.resource.IResourceLoader;
import com.exametrika.common.config.resource.IResourceManager;
import com.exametrika.common.config.resource.InlineResourceLoader;
import com.exametrika.common.config.resource.ResourceManager;
import com.exametrika.common.config.resource.ResourceNotFoundException;
import com.exametrika.common.config.resource.UrlResourceLoader;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link ResourceManagerTests} are tests for {@link IResourceManager} implementations.
 * 
 * @see IResourceManager
 * @author Medvedev-A
 */
public class ResourceManagerTests
{
    @Test
    public void testResourceManager() throws Throwable
    {
        Map<String, IResourceLoader> loaders = new HashMap<String, IResourceLoader>();
        loaders.put("test1", new TestResourceLoader("test1"));
        loaders.put("test2", new TestResourceLoader("test2"));
        loaders.put("null", new TestResourceLoader("null"));
        final ResourceManager manager = new ResourceManager(loaders, "test1");
        
        checkStream(manager.getResource("test1:testResource"), "test1:testResource");
        checkStream(manager.getResource("test2:testResource"), "test2:testResource");
        checkStream(manager.getResource("testResource"), "test1:testResource");
        
        new Expected(ResourceNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                manager.getResource("null:testResource");
            }
        });
        
        assertThat(manager.loadResource("null:testResource"), nullValue());
        
        ResourceManager manager2 = new ResourceManager(new HashMap<String, IResourceLoader>(), "test1", manager);
        checkStream(manager2.getResource("test1:testResource"), "test1:testResource");
        checkStream(manager2.getResource("testResource"), "test1:testResource");
    }
 
    @Test
    public void testContextResourceManager() throws Throwable
    {
        Map<String, IResourceLoader> loaders = new HashMap<String, IResourceLoader>();
        loaders.put("test1", new TestResourceLoader("test1"));
        loaders.put("test2", new TestResourceLoader("test2"));
        loaders.put("null", new TestResourceLoader("null"));
        final ResourceManager manager = new ResourceManager(loaders, "test1");
        final ContextResourceManager contextManager = new ContextResourceManager("test1:test", manager);
        
        checkStream(contextManager.getResource("resource"), "test1:test/resource");
        checkStream(contextManager.getResource("test2:testResource"), "test2:testResource");
        
        new Expected(ResourceNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                contextManager.getResource("null:testResource");
            }
        });
        
        assertThat(contextManager.loadResource("null:testResource"), nullValue());
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new ContextResourceManager("test", manager);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new ContextResourceManager("test1:test/", manager);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                contextManager.getResource("/testResource");
            }
        });
    }
    
    @Test
    public void testClassPathResourceLoader()
    {
        ClassPathResourceLoader loader = new ClassPathResourceLoader(getClass().getClassLoader());
        assertThat(loader.loadResource("classpath:" + getResourcePath() + "/config1.json") != null, is(true));
        assertThat(loader.loadResource("classpath:" + getResourcePath() + "/config100.xml"), nullValue());
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        try
        {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            loader = new ClassPathResourceLoader();
            assertThat(loader.loadResource("classpath:" + getResourcePath() + "/config1.json") != null, is(true));
            assertThat(loader.loadResource("classpath:" + getResourcePath() + "/config100.xml"), nullValue());
            
            Thread.currentThread().setContextClassLoader(null);
            assertThat(loader.loadResource("classpath:" + getResourcePath() + "/config1.json") != null, is(true));
            assertThat(loader.loadResource("classpath:" + getResourcePath() + "/config100.xml"), nullValue());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }
    
    @Test
    public void testFileResourceLoader() throws Throwable
    {
        File tmp = File.createTempFile("test", "test");
        tmp.deleteOnExit();
        
        FileResourceLoader loader = new FileResourceLoader();
        assertThat(loader.loadResource("file:" + tmp.getAbsolutePath()) != null, is(true));
        assertThat(loader.loadResource("file:" + tmp.getAbsolutePath() + "qq"), nullValue());        
    }
    
    @Test
    public void testInlineResourceLoader() throws Throwable
    {
        InlineResourceLoader loader = new InlineResourceLoader();
        assertThat(loader.loadResource("inline:{}") != null, is(true));
    }
    
    @Test
    public void testUrlResourceLoader() throws Throwable
    {
        File tmp = File.createTempFile("test", "test");
        tmp.deleteOnExit();
        
        UrlResourceLoader loader = new UrlResourceLoader();
        assertThat(loader.loadResource("file:" + tmp.getAbsolutePath()) != null, is(true));
        assertThat(loader.loadResource("file:" + tmp.getAbsolutePath() + "qq"), nullValue());        
    }
    
    @Test
    public void testCompositeResourceLoader() throws Throwable
    {
        File tmp = File.createTempFile("test", "test");
        tmp.deleteOnExit();
        
        FileResourceLoader loader1 = new FileResourceLoader();
        ClassPathResourceLoader loader2 = new ClassPathResourceLoader(getClass().getClassLoader());
        CompositeResourceLoader compositeLoader = new CompositeResourceLoader(Arrays.asList(loader1, loader2));
        
        assertThat(compositeLoader.loadResource("file:" + tmp.getAbsolutePath()) != null, is(true));
        assertThat(compositeLoader.loadResource("file:" + tmp.getAbsolutePath() + "qq"), nullValue()); 
        assertThat(compositeLoader.loadResource("classpath:" + getResourcePath() + "/config1.json") != null, is(true));
        assertThat(compositeLoader.loadResource("classpath:" + getResourcePath() + "/config100.xml"), nullValue());
    }
    
    @Test
    public void testContextResourceLoader() throws Throwable
    {
        File tmp = File.createTempFile("test", "test");
        tmp.deleteOnExit();
        
        final FileResourceLoader loader = new FileResourceLoader();
        
        final ContextResourceLoader contextLoader = new ContextResourceLoader("file:" + tmp.getParent(), loader);
        assertThat(contextLoader.loadResource("context:" + tmp.getName()) != null, is(true));
        assertThat(contextLoader.loadResource("context:" + "qq"), nullValue());  
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new ContextResourceLoader("test", loader);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new ContextResourceLoader("test1:test/", loader);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                contextLoader.loadResource("context:/testResource");
            }
        });
    }
    
    private void checkStream(InputStream stream, String str) throws Throwable
    {
        byte[] buf = new byte[100];
        int len = stream.read(buf);
        byte[] strBuf = str.getBytes();
        assertThat(len, is(strBuf.length));
        
        for (int i = 0; i < len; i++)
            assertThat(buf[i], is(strBuf[i]));
    }
    
    private static String getResourcePath()
    {
        String className = ResourceManagerTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
    
    private static class TestResourceLoader implements IResourceLoader
    {
        private final String schema;

        public TestResourceLoader(String schema)
        {
            this.schema = schema;
        }
        
        @Override
        public InputStream loadResource(String resourceLocation)
        {
            if (schema.equals("null"))
                return null;
             
            return new ByteArrayInputStream(resourceLocation.getBytes());
        }
    }
}