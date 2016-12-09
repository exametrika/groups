/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.parsers.JsonConfigurationParser;
import com.exametrika.common.config.property.MapPropertyResolver;
import com.exametrika.common.config.property.SystemPropertyResolver;
import com.exametrika.common.config.resource.ClassPathResourceLoader;
import com.exametrika.common.config.resource.FileResourceLoader;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonMacroses.Argument;
import com.exametrika.common.json.JsonMacroses.IMacro;
import com.exametrika.common.json.JsonMacroses.MacroDefinition;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.schema.JsonRegExpValidator;
import com.exametrika.common.json.schema.JsonValidationContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ConfigurationLoaderTests} are tests for {@link ConfigurationLoader}.
 * 
 * @see ConfigurationLoader
 * @author Medvedev-A
 */
public class ConfigurationLoaderTests
{
    @Test
    public void testLoader()
    {
        Parameters parameters = new Parameters();
        parameters.resourceLoaders.put(FileResourceLoader.SCHEMA, new FileResourceLoader());
        parameters.resourceLoaders.put(ClassPathResourceLoader.SCHEMA, new ClassPathResourceLoader());
        
        parameters.propertyResolvers.add(new SystemPropertyResolver());
        Map<String, String> propertyMap = new HashMap<String, String>();
        propertyMap.put("property1", "value1");
        propertyMap.put("property2", "value2");
        parameters.propertyResolvers.add(new MapPropertyResolver(propertyMap));
        
        parameters.configurationParsers.put("json",  new JsonConfigurationParser());
        parameters.configurationParsers.put("",  new JsonConfigurationParser());
        
        parameters.schemaMappings.put("schema1", new Pair("classpath:com/exametrika/tests/common/config/schema1.json", true));
        parameters.schemaMappings.put("schema2", new Pair("classpath:com/exametrika/tests/common/config/schema2.json", false));
        parameters.schemaMappings.put("schema3", new Pair("classpath:com/exametrika/tests/common/config/schema3.json", false));
        parameters.validators.put("validator1", new JsonRegExpValidator());
        parameters.elementLoaders.put("type1", new TestElementProcessor1());
        parameters.elementLoaders.put("type2", new TestElementProcessor2());
        parameters.elementLoaders.put("type3", new TestElementProcessor3());
        
        parameters.typeLoaders.put("type3", new TestTypeProcessor3());
        parameters.contextFactories.put("context", new TestContextFactory());
        parameters.topLevelElements.put("type1", new Pair("type1", true));
        parameters.topLevelElements.put("type2", new Pair("type2", true));
        parameters.topLevelElements.put("type3", new Pair("type3", true));
        
        ConfigurationLoader loader = new ConfigurationLoader(parameters, Collections.<String, Object>emptyMap(), null, false);
        TestConfiguration configuration = loader.loadConfiguration("classpath:com/exametrika/tests/common/config/config1.json").get("context");
        assertThat(configuration.test1.getClass() == Test1.class, is(true));
        assertThat(configuration.test1.prop0, is("[0-9]"));
        assertThat(configuration.test1.prop1, is("classpath:com/exametrika/tests/common/config"));
        assertThat(configuration.test1.prop2.prop1, is("value1"));
        assertThat(configuration.test1.prop2.prop2, nullValue());
        assertThat(configuration.test1.prop2.prop3.prop1, is("value2"));
        
        assertThat(configuration.test2.prop1, is("classpath:com/exametrika/tests/common/config"));
        assertThat(configuration.test2.prop2.getClass() == Test3.class, is(true));
        
        assertThat(configuration.test3.prop1, is("classpath:com/exametrika/tests/common/config"));
        assertThat(configuration.test3.prop2.prop1, is("value1"));
        assertThat(configuration.test3.prop2.prop2.getClass() == Test3.class, is(true));
        assertThat(configuration.test3.prop2.prop3.prop1, is("value2"));
        
        String inlineConfiguration = loader.createInlineConfiguration("classpath:com/exametrika/tests/common/config/config1.json", 
            new MapPropertyResolver(java.util.Collections.<String, String>emptyMap()));
        configuration = loader.loadInlineConfiguration(inlineConfiguration).get("context");
        assertThat(configuration.test1.getClass() == Test1.class, is(true));
        assertThat(configuration.test1.prop0, is("[0-9]"));
        assertThat(configuration.test1.prop1, is(""));
        assertThat(configuration.test1.prop2.prop1, is("value1"));
        assertThat(configuration.test1.prop2.prop2, nullValue());
        assertThat(configuration.test1.prop2.prop3.prop1, is("value2"));
        
        assertThat(configuration.test2.prop1, is(""));
        assertThat(configuration.test2.prop2.getClass() == Test3.class, is(true));
        
        assertThat(configuration.test3.prop1, is(""));
        assertThat(configuration.test3.prop2.prop1, is("value1"));
        assertThat(configuration.test3.prop2.prop2.getClass() == Test3.class, is(true));
        assertThat(configuration.test3.prop2.prop3.prop1, is("value2"));
    }
    
    @Test
    public void testMacroses()
    {
        Parameters parameters = new Parameters();
        parameters.resourceLoaders.put(FileResourceLoader.SCHEMA, new FileResourceLoader());
        parameters.resourceLoaders.put(ClassPathResourceLoader.SCHEMA, new ClassPathResourceLoader());
        parameters.configurationParsers.put("json",  new JsonConfigurationParser());
        parameters.configurationParsers.put("",  new JsonConfigurationParser());
        parameters.topLevelElements.put("root", new Pair("root", true));
        
        parameters.schemaMappings.put("schema4", new Pair("classpath:com/exametrika/tests/common/config/schema4.json", true));
        parameters.macroses.add(new MacroDefinition("buildin1", Collections.<String, Argument>emptyMap(), new IMacro()
        {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args)
            {
                return "Buildin1 generated";
            }
        }));
        parameters.macroses.add(new MacroDefinition("buildin2", new MapBuilder<String, Argument>()
            .put("arg1", new Argument("arg1", true, true, null))
            .put("arg2", new Argument("arg2", true, true, null))
            .toMap(), new IMacro()
        {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args)
            {
                return Json.object().put("field0", args).put("field2", "field2").put("field3", "field3").toObject();
            }
        }));
        ConfigurationLoader loader = new ConfigurationLoader(parameters, Collections.<String, Object>emptyMap(), null, false);
        String configuration = loader.createInlineConfiguration("classpath:com/exametrika/tests/common/config/config4.json", null);
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "conf");
        tempDir.mkdirs();
        Files.emptyDir(tempDir);
        File resultFile = new File(tempDir, "config4.data");
        Files.write(resultFile, configuration);
        JsonObject ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/config4.data", false);
        JsonObject result = JsonSerializers.read(configuration, false);
        if (!result.equals(ethalon))
        {
            System.out.println("result: " + resultFile);
            System.out.println(new JsonDiff(true).diff(result, ethalon));
            Assert.isTrue(false);
        }
    }
    
    private static class Test1
    {
        String prop0;
        String prop1;
        Test2 prop2;
        Test3 prop3;
    }
    
    private static class Test2
    {
        String prop1;
        Test1 prop2;
        Test3 prop3;
    }
    
    public static class Test3 extends Test1
    {
    }
    
    private static abstract class TestElementProcessor extends AbstractElementLoader
    {
        protected Test1 loadTest1(JsonObject element, ILoadContext context)
        {
            if (element == null)
                return null;
            
            Test1 t = new Test1();
            t.prop0 = element.get("prop0", null);
            t.prop1 = element.get("prop1");
            t.prop2 = loadTest2((JsonObject)element.get("prop2", null), context);
            t.prop3 = loadTest3((JsonObject)element.get("prop3", null), context);
            
            return t;
        }
        
        protected Test2 loadTest2(JsonObject element, ILoadContext context)
        {
            if (element == null)
                return null;
            
            Test2 t = new Test2();
            t.prop1 = element.get("prop1");
            JsonObject o = (JsonObject)element.get("prop2", null);
            if (o != null)
                t.prop2 = ((TestExtensionConfiguration)load("prop2", null, o, context)).createTest();
            t.prop3 = loadTest3((JsonObject)element.get("prop3", null), context);
            
            return t;
        }
        
        protected Test3 loadTest3(JsonObject element, ILoadContext context)
        {
            if (element == null)
                return null;
            
            Test3 t = new Test3();
            t.prop0 = element.get("prop0", null);
            t.prop1 = element.get("prop1");
            t.prop2 = loadTest2((JsonObject)element.get("prop2", null), context);
            t.prop3 = loadTest3((JsonObject)element.get("prop3", null), context);
            
            return t;
        }
    }
    
    private static class TestElementProcessor1 extends TestElementProcessor
    {
        @Override
        public void loadElement(JsonObject element, ILoadContext context)
        {
            TestContext c = context.get("context");
            c.test1 = loadTest1(element, context);
        }
    }
    
    private static class TestElementProcessor2 extends TestElementProcessor
    {
        @Override
        public void loadElement(JsonObject element, ILoadContext context)
        {
            TestContext c = context.get("context");
            c.test2 = loadTest2(element, context);
        }
    }
    
    private static class TestElementProcessor3 extends TestElementProcessor
    {
        @Override
        public void loadElement(JsonObject element, ILoadContext context)
        {
            TestContext c = context.get("context");
            c.test3 = loadTest3(element, context);
        }
    }
    
    private static class TestTypeProcessor3 implements IExtensionLoader
    {
        @Override
        public Object loadExtension(String name, String type, Object element, ILoadContext context)
        {
            return new TestExtensionConfiguration();
        }

        @Override
        public void setExtensionLoader(IExtensionLoader extensionProcessor)
        {
        }
    }
    
    private static class TestConfiguration
    {
        Test1 test1;
        Test2 test2;
        Test3 test3;
    }
    
    private static class TestContext implements IConfigurationFactory
    {
        Test1 test1;
        Test2 test2;
        Test3 test3;
        
        @Override
        public Object createConfiguration(ILoadContext context)
        {
            TestConfiguration configuration = new TestConfiguration();
            configuration.test1 = test1;
            configuration.test2 = test2;
            configuration.test3 = test3;
            return configuration;
        }
    }
    
    private static class TestContextFactory implements IContextFactory
    {
        @Override
        public IConfigurationFactory createContext()
        {
            return new TestContext();
        }
    }
    
    private static class TestExtensionConfiguration
    {
        public Test3 createTest()
        {
            return new Test3();
        }
    }
}