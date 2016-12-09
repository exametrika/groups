/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.component;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.component.container.ComponentContainer;
import com.exametrika.common.component.container.CompositeFactoryResolver;
import com.exametrika.common.component.container.FactoryNotFoundException;
import com.exametrika.common.component.container.IComponentContainer;
import com.exametrika.common.component.container.IComponentContainerAware;
import com.exametrika.common.component.container.IComponentFactory;
import com.exametrika.common.component.container.IComponentProcessor;
import com.exametrika.common.component.container.IQualifier;
import com.exametrika.common.component.container.ITypeProcessor;
import com.exametrika.common.component.factory.external.ExternalComponentFactory;
import com.exametrika.common.component.factory.prototype.AbstractPrototypeComponentFactory;
import com.exametrika.common.component.factory.scope.ScopedComponentFactory;
import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;
import com.exametrika.common.component.factory.threadlocal.AbstractThreadComponentFactory;
import com.exametrika.common.component.factory.threadlocal.IThreadComponentManager;
import com.exametrika.common.component.factory.threadlocal.ThreadComponentManager;
import com.exametrika.common.component.proxy.IInterceptor;
import com.exametrika.common.component.proxy.IInvocation;
import com.exametrika.common.component.proxy.jdk.JdkProxyFactory;
import com.exametrika.common.component.scope.Scope;
import com.exametrika.common.component.scope.ScopeManager;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link ComponentContainerTests} are tests for {@link ComponentContainer}.
 * 
 * @see ComponentContainer
 * @author Medvedev-A
 */
public class ComponentContainerTests
{
    @Test
    public void testRegister() throws Throwable
    {
        final IComponentContainer container = new ComponentContainer();
        TestFactory factory = new TestFactory();
        container.register("test", factory);
        
        assertThat(factory.container, is(container));
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.register("test", new TestFactory());
            }
        });
        
        final Map<String, String> qualifiers = new HashMap<String, String>();
        qualifiers.put("key1", "value1");
        qualifiers.put("key2", "value2");
        container.register("test", qualifiers, new TestFactory());
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.register("test", qualifiers, new TestFactory());
            }
        });
    }
    
    @Test
    public void testLifecycle() throws Throwable
    {
        final ComponentContainer container = new ComponentContainer();
        TestFactory factory1 = new TestFactory();
        TestFactory factory2 = new TestFactory();
        container.register("test1", factory1);
        container.register("test2", factory2);
        
        assertThat(factory1.started, is(false));
        assertThat(factory2.started, is(false));
        
        container.start();

        assertThat(factory1.started, is(true));
        assertThat(factory2.started, is(true));

        TestFactory factory3 = new TestFactory();
        container.register("test3", factory3);
        assertThat(factory3.started, is(true));
        
        assertThat(factory3.stopped, is(false));
        container.unregister("test3");
        assertThat(factory3.stopped, is(true));
        
        assertThat(factory1.stopped, is(false));
        assertThat(factory2.stopped, is(false));
        
        container.stop();
        
        assertThat(factory1.stopped, is(true));
        assertThat(factory2.stopped, is(true));
    }
    
    @Test
    public void testProcessableFactory()
    {
        ComponentContainer container = new ComponentContainer(null, Arrays.asList(new TestTypeProcessor()), null);
        TestPrototypeProcessableFactory factory1 = new TestPrototypeProcessableFactory();
        TestSingletonProcessableFactory factory2 = new TestSingletonProcessableFactory();
        TestThreadProcessableFactory factory3 = new TestThreadProcessableFactory(new ThreadComponentManager());
        container.register("test1", factory1);
        container.register("test2", factory2);
        container.register("test3", factory3);
        container.start();
        
        assertThat(factory1.componentType.equals(TestComponent2.class), is(true));
        assertThat(factory2.componentType.equals(TestComponent2.class), is(true));
        assertThat(factory3.componentType.equals(TestComponent2.class), is(true));
    }
    
    @Test
    public void testGetComponent() throws Throwable
    {
        final ComponentContainer container = new ComponentContainer();
        TestFactory factory = new TestFactory();
        container.register("test", factory);
        
        Map<String, Object> qualifiers = new HashMap<String, Object>();
        qualifiers.put("majorVersion", 1);
        qualifiers.put("minorVersion", 1);
        
        container.register("test", qualifiers, new TestFactory(1));
        container.register("test2", qualifiers, new TestFactory2(10));
        container.register("test3", new TestFactory(3));
        
        qualifiers = new HashMap<String, Object>();
        qualifiers.put("majorVersion", 1);
        qualifiers.put("minorVersion", 2);
        
        container.register("test", qualifiers, new TestFactory(2));
        
        container.start();
        
        // Check by name wuthout qualifiers
        TestComponent component = container.getComponent("test"); 
        assertThat(component, is(new TestComponent()));
        
        TestComponent2 component2 = container.getComponent("test2"); 
        assertThat(component2, is(new TestComponent2(10)));
        
        // Check by name and simple qualifiers
        qualifiers = new HashMap<String, Object>();
        qualifiers.put("majorVersion", 1);
        qualifiers.put("minorVersion", 1);
        
        component = container.getComponent("test", qualifiers, true);
        assertThat(component, is(new TestComponent(1)));
        
        // Check by name and IQualifier required qualifiers
        qualifiers = new HashMap<String, Object>();
        qualifiers.put("minorVersion", new GreaterOrEqualQualifier(1, true));
        component = container.getComponent("test", qualifiers, true);
        assertThat(component, is(new TestComponent(1)));

        qualifiers = new HashMap<String, Object>();
        qualifiers.put("minorVersion", new GreaterOrEqualQualifier(2, true));
        component = container.getComponent("test", qualifiers, true);
        assertThat(component, is(new TestComponent(2)));
        
        // Check by name and IQualifier optional qualifiers
        qualifiers = new HashMap<String, Object>();
        qualifiers.put("minorVersion", new GreaterOrEqualQualifier(2, false));
        component = container.getComponent("test3", qualifiers, true);
        assertThat(component, is(new TestComponent(3)));

        // Check required not found component by name and required qualifiers
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                final Map<String, Object> qualifiers = new HashMap<String, Object>();
                qualifiers.put("minorVersion", new GreaterOrEqualQualifier(2, true));
                container.getComponent("test3", qualifiers, true);
            }
        });
        
        final Map<String, Object> qualifiers2 = new HashMap<String, Object>();
        qualifiers2.put("majorVersion", 3);
        qualifiers2.put("minorVersion", 3);
        
        // Check required not found component by name
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.getComponent("test100");
            }
        });
        
        // Check required not found component by name and qualifiers
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.getComponent("test", qualifiers2, true);
            }
        });
        
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.getComponent("test100", null, true);
            }
        });
        
        // Check optional not found component by name
        assertThat(container.getComponent("test100", null, false) == null, is(true));
        
        final Map<String, Object> qualifiers1 = new HashMap<String, Object>();
        qualifiers1.put("majorVersion", 1);
        qualifiers1.put("minorVersion", 1);
        
        container.unregister("test", qualifiers1);
        container.unregister("test2", qualifiers1);
        container.unregister("test3");
        
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.getComponent("test", qualifiers1, true);
            }
        });
        
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.getComponent("test2", qualifiers1, true);
            }
        });
        
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container.getComponent("test3", null, true);
            }
        });
    }
    
    @Test
    public void testResolver() throws Throwable
    {
        ComponentContainer container1 = new ComponentContainer();
        TestFactory factory = new TestFactory();
        container1.register("test", factory);
        
        Map<String, Object> qualifiers = new HashMap<String, Object>();
        qualifiers.put("majorVersion", 1);
        qualifiers.put("minorVersion", 1);
        
        ComponentContainer container2 = new ComponentContainer();
        TestFactory2 factory2 = new TestFactory2();
        container2.register("test2", qualifiers, factory2);
        
        final ComponentContainer container3 = new ComponentContainer(
            new CompositeFactoryResolver(Arrays.asList(container1, container2)), null, null);
        TestFactory2 factory3 = new TestFactory2(2);
        container3.register("test2", factory3);
        
        container1.start();
        container2.start();
        container3.start();
        
        TestComponent component1 = container3.getComponent("test", null, true);
        assertThat(component1, is(new TestComponent()));
        
        TestComponent2 component2 = container3.getComponent("test2", qualifiers, true);
        assertThat(component2, is(new TestComponent2()));
        
        TestComponent2 component3 = container3.getComponent("test2", null, true);
        assertThat(component3, is(new TestComponent2(2)));
        
        new Expected(FactoryNotFoundException.class, new Runnable()
        {
            @Override
            public void run()
            {
                container3.getComponent("test100", null, true);
            }
        });
        
        assertThat(container3.getComponent("test100", null, false) == null, is(true));
    }
    
    @Test
    public void testComponentProcessors() throws Throwable
    {
        ComponentContainer container = new ComponentContainer(null, null, Arrays.asList(new TestComponentProcessor(5, 10)));
        TestPrototypeProcessableFactory factory1 = new TestPrototypeProcessableFactory();
        TestSingletonProcessableFactory factory2 = new TestSingletonProcessableFactory();
        TestThreadProcessableFactory factory3 = new TestThreadProcessableFactory(new ThreadComponentManager());
        container.register("test1", factory1);
        container.register("test2", factory2);
        container.register("test3", factory3);
        container.start();
        
        TestComponent component1 = container.getComponent("test1");
        assertThat(component1.before, is(5));
        assertThat(component1.after, is(10));
        
        TestComponent component2 = container.getComponent("test2");
        assertThat(component2.before, is(5));
        assertThat(component2.after, is(10));
        
        TestComponent component3 = container.getComponent("test3");
        assertThat(component3.before, is(5));
        assertThat(component3.after, is(10));
    }
    
    @Test
    public void testExternalComponentFactory() throws Throwable
    {
        TestComponent component = new TestComponent();
        ComponentContainer container = new ComponentContainer();
        container.register("test", new ExternalComponentFactory(component));
        container.start();
        
        assertThat(container.getComponent("test") == component, is(true));
    }
    
    @Test
    public void testSingletonComponentFactory() throws Throwable
    {
        ComponentContainer container = new ComponentContainer();
        container.register("test1", new TestSingletonFactory1(true, "test2"));
        container.register("test2", new TestSingletonFactory2(true, "test1"));
        container.register("test3", new TestSingletonFactory1(false, "test4"));
        container.register("test4", new TestSingletonFactory2(false, "test3"));
        container.start();
        
        TestSingleton1 singleton1 = container.getComponent("test1");
        TestSingleton2 singleton2 = container.getComponent("test2");
        TestSingleton1 singleton3 = container.getComponent("test3");
        TestSingleton2 singleton4 = container.getComponent("test4");
        
        assertThat(container.getComponent("test1") == singleton1, is(true));
        assertThat(container.getComponent("test2") == singleton2, is(true));
        assertThat(container.getComponent("test3") == singleton3, is(true));
        assertThat(container.getComponent("test4") == singleton4, is(true));
        
        assertThat(singleton1.singleton == singleton2, is(true));
        assertThat(singleton2.singleton == singleton1, is(true));
        assertThat(singleton3.singleton == singleton4, is(true));
        assertThat(singleton4.singleton == singleton3, is(true));
        
        assertThat(singleton1.started, is(true));
        assertThat(singleton2.started, is(true));
        assertThat(singleton3.started, is(true));
        assertThat(singleton4.started, is(true));
        
        container.stop();
        
        assertThat(singleton1.stopped, is(true));
        assertThat(singleton2.stopped, is(true));
        assertThat(singleton3.stopped, is(true));
        assertThat(singleton4.stopped, is(true));
    }
    
    @Test
    public void testThreadComponentFactory() throws Throwable
    {
        ThreadComponentManager threadManager = new ThreadComponentManager();
        ComponentContainer container = new ComponentContainer();
        container.register("test1", new TestThreadFactory1(threadManager, true, "test2"));
        container.register("test2", new TestThreadFactory2(threadManager, true, "test1"));
        container.register("test3", new TestThreadFactory1(threadManager, false, "test4"));
        container.register("test4", new TestThreadFactory2(threadManager, false, "test3"));
        container.start();
        
        threadManager.createThreadComponents();
        
        TestSingleton1 singleton1 = container.getComponent("test1");
        TestSingleton2 singleton2 = container.getComponent("test2");
        TestSingleton1 singleton3 = container.getComponent("test3");
        TestSingleton2 singleton4 = container.getComponent("test4");
        
        assertThat(container.getComponent("test1") == singleton1, is(true));
        assertThat(container.getComponent("test2") == singleton2, is(true));
        assertThat(container.getComponent("test3") == singleton3, is(true));
        assertThat(container.getComponent("test4") == singleton4, is(true));
        
        assertThat(singleton1.singleton == singleton2, is(true));
        assertThat(singleton2.singleton == singleton1, is(true));
        assertThat(singleton3.singleton == singleton4, is(true));
        assertThat(singleton4.singleton == singleton3, is(true));
        
        assertThat(singleton1.started, is(true));
        assertThat(singleton2.started, is(true));
        assertThat(singleton3.started, is(true));
        assertThat(singleton4.started, is(true));
        
        threadManager.destroyThreadComponents();
        
        assertThat(singleton1.stopped, is(true));
        assertThat(singleton2.stopped, is(true));
        assertThat(singleton3.stopped, is(true));
        assertThat(singleton4.stopped, is(true));
    }
    
    @Test
    public void testScopedAndPrototypeComponentFactory() throws Throwable
    {
        ScopeManager scopeManager = new ScopeManager();
        Scope scope1 = new Scope();
        scopeManager.attach(scope1);
        
        TestInterceptor interceptor = new TestInterceptor();
        
        ComponentContainer container = new ComponentContainer();
        
        container.register("test1.component", new TestPrototypeFactory1("test2"));
        container.register("test2.component", new TestPrototypeFactory2("test1"));
        
        container.register("test1", new ScopedComponentFactory(new JdkProxyFactory(), scopeManager, 
            Arrays.asList(TestInterface1.class), "test1.component", interceptor));
        container.register("test2", new ScopedComponentFactory(new JdkProxyFactory(), scopeManager, 
            Arrays.asList(TestInterface2.class), "test2.component", interceptor));
        
        container.start();
        
        TestInterface1 singleton1 = container.getComponent("test1");
        TestInterface2 singleton2 = container.getComponent("test2");
        interceptor.invocations.clear();
        
        assertThat(container.getComponent("test1") == singleton1, is(true));
        assertThat(container.getComponent("test2") == singleton2, is(true));
        
        assertThat(singleton1.getSingleton() == singleton2, is(true));
        assertThat(singleton2.getSingleton() == singleton1, is(true));
        
        assertThat(singleton1.isStarted(), is(true));
        assertThat(singleton2.isStarted(), is(true));
        
        scope1.stop();
        
        assertThat(singleton1.isStopped(), is(true));
        assertThat(singleton2.isStopped(), is(true));
        
        assertThat(interceptor.invocations.size(), is(6));
        assertThat(interceptor.invocations.get(0).getThis() == singleton1, is(true));
        assertThat(interceptor.invocations.get(2).getThis() == singleton1, is(true));
        assertThat(interceptor.invocations.get(4).getThis() == singleton1, is(true));
        assertThat(interceptor.invocations.get(1).getThis() == singleton2, is(true));
        assertThat(interceptor.invocations.get(3).getThis() == singleton2, is(true));
        assertThat(interceptor.invocations.get(5).getThis() == singleton2, is(true));
        
        assertThat(interceptor.invocations.get(0).getTarget() == singleton1.getThis(), is(true));
        assertThat(interceptor.invocations.get(0).getTarget() == interceptor.invocations.get(2).getTarget(), is(true));
        assertThat(interceptor.invocations.get(2).getTarget() == interceptor.invocations.get(4).getTarget(), is(true));
        assertThat(interceptor.invocations.get(1).getTarget() == singleton2.getThis(), is(true));
        assertThat(interceptor.invocations.get(1).getTarget() == interceptor.invocations.get(3).getTarget(), is(true));
        assertThat(interceptor.invocations.get(3).getTarget() == interceptor.invocations.get(5).getTarget(), is(true));
        
        Object this1 = singleton1.getThis();
        assertThat(singleton1.getThis() == this1, is(true));
        
        scopeManager.detach();
        
        Scope scope2 = new Scope();
        scopeManager.attach(scope2);
        
        Object this2 = singleton1.getThis();
        assertThat(singleton1.getThis() == this2, is(true));
        
        scopeManager.detach();
        scopeManager.attach(scope1);
        
        assertThat(singleton1.getThis() == this1, is(true));
    }
    
    private static class TestComponent
    {
        int value;
        int before;
        int after;
        
        public TestComponent(int value)
        {
            this.value = value;
        }
        
        public TestComponent()
        {
            this.value = 0;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof TestComponent))
                return false;
            
            TestComponent component = (TestComponent)o;
            return value == component.value;
        }
        
        @Override
        public int hashCode()
        {
            return value;
        }
    }
    
    private static class TestComponent2 extends TestComponent
    {
        public TestComponent2(int value)
        {
            super(value);
        }
        
        public TestComponent2()
        {
        }
    }
    
    private static class TestFactory implements IComponentFactory<TestComponent>, IComponentContainerAware, ILifecycle
    {
        IComponentContainer container;
        private final int value;
        boolean started;
        boolean stopped;
        
        public TestFactory(int value)
        {
            this.value = value;
        }
        
        public TestFactory()
        {
            this.value = 0;
        }
        
        @Override
        public TestComponent createComponent()
        {
            return new TestComponent(value);
        }

        @Override
        public void start()
        {
            started = true;
        }
        
        @Override
        public void stop()
        {
            stopped = true;
        }
        
        @Override
        public void setContainer(IComponentContainer container)
        {
            this.container = container;
        }
    }

    private static class TestFactory2 implements IComponentFactory<TestComponent2>, IComponentContainerAware
    {
        private final int value;
        
        public TestFactory2(int value)
        {
            this.value = value;
        }
        
        public TestFactory2()
        {
            this.value = 0;
        }
        
        @Override
        public TestComponent2 createComponent()
        {
            return new TestComponent2(value);
        }

        @Override
        public void setContainer(IComponentContainer container)
        {
        }
    }

    private static class TestPrototypeProcessableFactory extends AbstractPrototypeComponentFactory<TestComponent>
    {
        Class<?> componentType = TestComponent.class;
        
        @Override
        protected TestComponent createInstance()
        {
            try
            {
                return (TestComponent)componentType.newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException();
            }
        }
        
        @Override
        protected void processType()
        {
            componentType = processType(componentType);
        }
    }
    
    private static class TestSingletonProcessableFactory extends AbstractSingletonComponentFactory<TestComponent>
    {
        Class<?> componentType = TestComponent.class;
        
        public TestSingletonProcessableFactory()
        {
            super(true);
        }
        
        @Override
        protected TestComponent createInstance()
        {
            try
            {
                return (TestComponent)componentType.newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException();
            }
        }
        
        @Override
        protected void processType()
        {
            componentType = processType(componentType);
        }
    }
    
    private static class TestThreadProcessableFactory extends AbstractThreadComponentFactory<TestComponent>
    {
        Class<?> componentType = TestComponent.class;
        
        public TestThreadProcessableFactory(IThreadComponentManager manager)
        {
            super(manager, true);
        }
        
        @Override
        protected TestComponent createInstance()
        {
            try
            {
                return (TestComponent)componentType.newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException();
            }
        }
        
        @Override
        protected void processType()
        {
            componentType = processType(componentType);
        }
    }
    
    private static class TestTypeProcessor implements ITypeProcessor
    {
        @Override
        public Object processType(Object componentType)
        {
            if (componentType.equals(TestComponent.class))
                return TestComponent2.class;
            
            return null;
        }
    }
    
    private static class GreaterOrEqualQualifier implements IQualifier
    {
        private final int value;
        private final boolean required;

        public GreaterOrEqualQualifier(int value, boolean required)
        {
            this.value = value;
            this.required = required;
        }
        
        @Override
        public boolean match(String qualifierName, Map<String, ?> qualifiers)
        {
            if (!required && !qualifiers.containsKey(qualifierName))
                // Skip optional qualifier if not found
                return true;
            
            Object qualifier = qualifiers.get(qualifierName);
            if (!(qualifier instanceof Integer))
                return false;
            
            if ((Integer)qualifier >= value)
                return true;
            
            return false;
        }
    }
    
    private static class TestComponentProcessor implements IComponentProcessor
    {
        private final int before;
        private final int after;

        public TestComponentProcessor(int before, int after)
        {
            this.before = before;
            this.after = after;
        }
        
        @Override
        public Object processBeforeStart(Object component)
        {
            TestComponent c = (TestComponent)component;
            c.before = before;
            return c;
        }
        
        @Override
        public Object processAfterStart(Object component)
        {
            TestComponent c = (TestComponent)component;
            c.after = after;
            return c;
        }
    }
    
    public static interface TestInterface1
    {
        boolean isStarted();
        boolean isStopped();
        TestInterface2 getSingleton();
        Object getThis();
    }
    
    private static class TestSingleton1 implements ILifecycle, TestInterface1
    {
        TestInterface2 singleton;
        boolean started;
        boolean stopped;

        @Override
        public void start()
        {
            started = true;
        }

        @Override
        public void stop()
        {
            stopped = true;
        }
        
        @Override
        public boolean isStarted()
        {
            return started;
        }
        
        @Override
        public boolean isStopped()
        {
            return stopped;
        }
        
        @Override
        public TestInterface2 getSingleton()
        {
            return singleton;
        }
        
        @Override
        public Object getThis()
        {
            return this;
        }
    }
    
    public static interface TestInterface2
    {
        boolean isStarted();
        boolean isStopped();
        TestInterface1 getSingleton();
        Object getThis();
    }
    
    private static class TestSingleton2 implements ILifecycle, TestInterface2
    {
        TestInterface1 singleton;
        boolean started;
        boolean stopped;

        @Override
        public void start()
        {
            started = true;
        }

        @Override
        public void stop()
        {
            stopped = true;
        }
        
        @Override
        public boolean isStarted()
        {
            return started;
        }
        
        @Override
        public boolean isStopped()
        {
            return stopped;
        }
        
        @Override
        public TestInterface1 getSingleton()
        {
            return singleton;
        }
        
        @Override
        public Object getThis()
        {
            return this;
        }
    }
    
    private static class TestSingletonFactory1 extends AbstractSingletonComponentFactory<TestSingleton1>
    {
        private final String componentName;

        public TestSingletonFactory1(boolean lazyInitialization, String componentName)
        {
            super(lazyInitialization);
            this.componentName = componentName;
        }
        
        @Override
        protected TestSingleton1 createInstance()
        {
            return new TestSingleton1();
        }
        
        @Override
        protected void setComponentDependencies(TestSingleton1 instance)
        {
            instance.singleton = getContainer().getComponent(componentName);
        }
    }
    
    private static class TestSingletonFactory2 extends AbstractSingletonComponentFactory<TestSingleton2>
    {
        private final String componentName;

        public TestSingletonFactory2(boolean lazyInitialization, String componentName)
        {
            super(lazyInitialization);
            this.componentName = componentName;
        }
        
        @Override
        protected TestSingleton2 createInstance()
        {
            return new TestSingleton2();
        }
        
        @Override
        protected void setComponentDependencies(TestSingleton2 instance)
        {
            instance.singleton = getContainer().getComponent(componentName);
        }
    }
    
    private static class TestThreadFactory1 extends AbstractThreadComponentFactory<TestSingleton1>
    {
        private final String componentName;
        private IComponentFactory<TestSingleton2> factory2;

        public TestThreadFactory1(ThreadComponentManager threadComponentManager, boolean lazyInitialization, String componentName)
        {
            super(threadComponentManager, lazyInitialization);
            this.componentName = componentName;
        }
        
        @Override
        protected TestSingleton1 createInstance()
        {
            return new TestSingleton1();
        }
        
        @Override
        protected void setFactoryDependencies()
        {
            factory2 = getContainer().getFactory(componentName); 
        }
        
        @Override
        protected void setComponentDependencies(TestSingleton1 instance)
        {
            instance.singleton = factory2.createComponent(); 
        }
    }
    
    private static class TestThreadFactory2 extends AbstractThreadComponentFactory<TestSingleton2>
    {
        private final String componentName;
        private IComponentFactory<TestSingleton1> factory1;

        public TestThreadFactory2(ThreadComponentManager threadComponentManager, boolean lazyInitialization, String componentName)
        {
            super(threadComponentManager, lazyInitialization);
            this.componentName = componentName;
        }
        
        @Override
        protected TestSingleton2 createInstance()
        {
            return new TestSingleton2();
        }
        
        @Override
        protected void setFactoryDependencies()
        {
            factory1= getContainer().getFactory(componentName); 
        }
        
        @Override
        protected void setComponentDependencies(TestSingleton2 instance)
        {
            instance.singleton = factory1.createComponent();
        }
    }

    private static class TestPrototypeFactory1 extends AbstractPrototypeComponentFactory<TestSingleton1>
    {
        private final String componentName;
        private IComponentFactory<TestSingleton2> factory2;

        public TestPrototypeFactory1(String componentName)
        {
            this.componentName = componentName;
        }
        
        @Override
        protected TestSingleton1 createInstance()
        {
            TestSingleton1 singleton =  new TestSingleton1();
            singleton.singleton = factory2.createComponent();
            return singleton;
        }

        @Override
        protected void setFactoryDependencies()
        {
            factory2 = getContainer().getFactory(componentName); 
        }
    }
    
    private static class TestPrototypeFactory2 extends AbstractPrototypeComponentFactory<TestSingleton2>
    {
        private final String componentName;
        private IComponentFactory<TestSingleton1> factory1;

        public TestPrototypeFactory2(String componentName)
        {
            this.componentName = componentName;
        }
        
        @Override
        protected TestSingleton2 createInstance()
        {
            TestSingleton2 singleton =  new TestSingleton2();
            singleton.singleton = factory1.createComponent();
            return singleton;
        }

        @Override
        protected void setFactoryDependencies()
        {
            factory1 = getContainer().getFactory(componentName); 
        }
    }
    
    private static class TestInterceptor implements IInterceptor
    {
        List<IInvocation> invocations = new ArrayList<IInvocation>();

        @Override
        public <T> T invoke(IInvocation invocation)
        {
            invocations.add(invocation);
            return (T)invocation.proceed();
        }
    }
}