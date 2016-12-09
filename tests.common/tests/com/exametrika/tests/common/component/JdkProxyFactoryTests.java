/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.component;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.exametrika.common.component.proxy.IInterceptor;
import com.exametrika.common.component.proxy.IInvocation;
import com.exametrika.common.component.proxy.jdk.JdkProxyFactory;


/**
 * The {@link JdkProxyFactoryTests} are tests for {@link JdkProxyFactory}.
 * 
 * @see JdkProxyFactory
 * @author Medvedev-A
 */
public class JdkProxyFactoryTests
{
    @Test
    public void testProxy()
    {
        TestClass target = new TestClass();
        TestInterceptor interceptor = new TestInterceptor(target);
        Object proxy = new JdkProxyFactory().createProxy(getClass().getClassLoader(), Arrays.asList(TestInterface1.class,
            TestInterface2.class), interceptor);
        
        assertThat(proxy.getClass().getClassLoader() == getClass().getClassLoader(), is(true));
        assertThat(proxy instanceof TestInterface1, is(true));
        assertThat(proxy instanceof TestInterface2, is(true));
        
        TestInterface1 interface1 = (TestInterface1)proxy;
        assertThat(interface1.test(10, true), is("hello"));
        
        TestInterface2 interface2 = (TestInterface2)proxy;
        assertThat(interface2.test2(), is(11));
        
        assertThat(interceptor.invocations.size(), is(2));
        assertThat(Arrays.equals(interceptor.invocations.get(0).getArguments(), new Object[]{10, true}), is(true));
        assertThat(interceptor.invocations.get(0).getThis() == proxy, is(true));
        assertThat(interceptor.invocations.get(0).getTarget() == target, is(true));
        
        assertThat(interceptor.invocations.get(1).getArguments(), nullValue());
        assertThat(interceptor.invocations.get(1).getThis() == proxy, is(true));
        assertThat(interceptor.invocations.get(1).getTarget() == target, is(true));
    }
    
    public interface TestInterface1
    {
        String test(int arg1, boolean arg2);
    }
    
    public interface TestInterface2
    {
        int test2();
    }
    
    public static class TestClass implements TestInterface1, TestInterface2
    {
        @Override
        public String test(int arg1, boolean arg2)
        {
            return "hello";
        }

        @Override
        public int test2()
        {
            return 11;
        }
    }
    
    private static class TestInterceptor implements IInterceptor
    {
        List<IInvocation> invocations = new ArrayList<IInvocation>();
        private final TestClass target;
        
        public TestInterceptor(TestClass target)
        {
            this.target = target;
        }
        
        @Override
        public <T> T invoke(IInvocation invocation)
        {
            invocations.add(invocation);
            invocation.setTarget(target);
            return (T)invocation.proceed();
        }
    }
}