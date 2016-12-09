/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.log;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.log.LoggingContext;
import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.LoggerConfiguration;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.log.config.TextAppenderConfiguration;
import com.exametrika.common.log.impl.IAppender;
import com.exametrika.common.log.impl.LogContext;
import com.exametrika.common.log.impl.LogEvent;
import com.exametrika.common.log.impl.LoggingService;
import com.exametrika.common.log.impl.TextAppender;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Times;


/**
 * The {@link LoggerManagerTests} are tests for {@link LoggingConfiguration} class.
 * 
 * @see LoggingConfiguration
 * @author Medvedev_A
 */
public class LoggerManagerTests
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    @Test
    public void testManager() throws Throwable
    {
        TestAppenderConfiguration appenderConfiguration = new TestAppenderConfiguration("appender", LogLevel.DEBUG, "", Collections.<String>emptyList());
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender"), Collections.<String>emptyList(), false);
        LoggerConfiguration info1 = new LoggerConfiguration("test.test2", LogLevel.DEBUG, Arrays.<String>asList("appender"), Collections.<String>emptyList(), false);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appenderConfiguration), root, Arrays.<LoggerConfiguration>asList(info1));
        
        LoggingService manager = new LoggingService(configuration);
        Map<String, IAppender> appenders = Tests.get(manager, "appenders");
        assertThat(appenders.size(), is(1));
        AppenderMock appender = (AppenderMock)appenders.get("appender");
        
        manager.onTimer(Times.getCurrentTime());
        assertThat(appender.flushed, is(true));
        assertThat(appender.started, is(true));
        assertThat(appender.stopped, is(false));
        
        ILogger logger1 = manager.createLogger("test");
        ILogger logger11 = manager.createLogger("test");
        assertThat(logger1 == logger11, is(true));
        ILogger logger2 = manager.createLogger("test.test2");
        ILogger logger3 = manager.createLogger("test.test2.test3");
        
        assertThat(logger1.isLogEnabled(LogLevel.DEBUG), is(false));
        assertThat(logger1.isLogEnabled(LogLevel.INFO), is(true));
        logger1.log(LogLevel.DEBUG, messages.test());
        Thread.sleep(100);
        assertThat(appender.messages.isEmpty(), is(true));
        logger1.log(LogLevel.INFO, new Exception());
        Thread.sleep(100);
        assertThat(appender.messages.size(), is(1));
        appender.messages.clear();
        
        assertThat(logger2.isLogEnabled(LogLevel.TRACE), is(false));
        assertThat(logger2.isLogEnabled(LogLevel.DEBUG), is(true));
        logger3.log(LogLevel.TRACE, messages.test());
        Thread.sleep(100);
        assertThat(appender.messages.isEmpty(), is(true));
        logger2.log(LogLevel.DEBUG, messages.test(), new Exception());
        Thread.sleep(100);
        assertThat(appender.messages.size(), is(1));
        appender.messages.clear();
        
        assertThat(logger3.isLogEnabled(LogLevel.TRACE), is(false));
        assertThat(logger3.isLogEnabled(LogLevel.DEBUG), is(true));
        logger3.log(LogLevel.TRACE, messages.test());
        Thread.sleep(100);
        assertThat(appender.messages.isEmpty(), is(true));
        logger3.log(LogLevel.DEBUG, messages.test());
        Thread.sleep(100);
        assertThat(appender.messages.size(), is(1));
        appender.messages.clear();
        
        TestAppenderConfiguration appenderConfiguration2 = new TestAppenderConfiguration("appender2", LogLevel.DEBUG, "", Collections.<String>emptyList());
        root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender2"), Collections.<String>emptyList(), false);
        info1 = new LoggerConfiguration("test.test2", LogLevel.INFO, Arrays.<String>asList("appender2"), Collections.<String>emptyList(), false);
        configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, Arrays.<AppenderConfiguration>asList(appenderConfiguration2), 
            root, Arrays.<LoggerConfiguration>asList(info1));
        manager.setConfiguration(configuration);
        
        assertThat(appenders.size(), is(1));
        AppenderMock appender2 = (AppenderMock)appenders.get("appender2");
        
        assertThat(appender.stopped, is(true));
        assertThat(appender2.started, is(true));
        
        assertThat(logger1.isLogEnabled(LogLevel.DEBUG), is(false));
        assertThat(logger1.isLogEnabled(LogLevel.INFO), is(true));
        logger1.log(LogLevel.DEBUG, messages.test());
        Thread.sleep(100);
        assertThat(appender2.messages.isEmpty(), is(true));
        logger1.log(LogLevel.INFO, messages.test());
        Thread.sleep(100);
        assertThat(appender2.messages.size(), is(1));
        appender2.messages.clear();
        
        assertThat(logger2.isLogEnabled(LogLevel.DEBUG), is(false));
        assertThat(logger2.isLogEnabled(LogLevel.INFO), is(true));
        logger2.log(LogLevel.DEBUG, messages.test());
        Thread.sleep(100);
        assertThat(appender2.messages.isEmpty(), is(true));
        logger2.log(LogLevel.INFO, messages.test());
        Thread.sleep(100);
        assertThat(appender2.messages.size(), is(1));
        appender2.messages.clear();
        
        assertThat(logger3.isLogEnabled(LogLevel.DEBUG), is(false));
        assertThat(logger3.isLogEnabled(LogLevel.INFO), is(true));
        logger3.log(LogLevel.DEBUG, messages.test());
        Thread.sleep(100);
        assertThat(appender2.messages.isEmpty(), is(true));
        logger3.log(LogLevel.INFO, messages.test());
        Thread.sleep(100);
        assertThat(appender2.messages.size(), is(1));
        appender2.messages.clear();
        
        manager.stop(false);
        appender2.flushed = false;
        
        Thread.sleep(2000);
        
        assertThat(appender2.flushed, is(false));
        assertThat(appender2.stopped, is(true));
    }
    
    @Test
    public void testFilters() throws Throwable
    {
        TestAppenderConfiguration appenderConfiguration = new TestAppenderConfiguration("appender", LogLevel.DEBUG, "test", 
            Arrays.<String>asList("filter('*helloAppender*', message)"));
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender"), 
            Arrays.<String>asList("filter('*helloLogger*', message)"), false);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appenderConfiguration), root, Arrays.<LoggerConfiguration>asList());
        
        LoggingService manager = new LoggingService(configuration);
        Map<String, IAppender> appenders = Tests.get(manager, "appenders");
        assertThat(appenders.size(), is(1));
        AppenderMock appender = (AppenderMock)appenders.get("appender");
        
        manager.onTimer(Times.getCurrentTime());
        assertThat(appender.flushed, is(true));
        assertThat(appender.started, is(true));
        assertThat(appender.stopped, is(false));
        
        ILogger logger1 = manager.createLogger("test");
        logger1.log(LogLevel.INFO, messages.testLogFilter());
        Thread.sleep(100);
        assertThat(appender.messages.isEmpty(), is(true));
        
        logger1.log(LogLevel.INFO, messages.testAppenderFilter());
        Thread.sleep(100);
        assertThat(appender.messages.isEmpty(), is(true));
        
        logger1.log(LogLevel.INFO, messages.testBothFilters());
        Thread.sleep(100);
        assertThat(appender.messages.size(), is(1));
    }
    
    @Test
    public void testLoggerStackTrace() throws Throwable
    {
        TestAppenderConfiguration appenderConfiguration = new TestAppenderConfiguration("appender", LogLevel.DEBUG, "test", 
            Collections.<String>emptyList());
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender"), 
            Collections.<String>emptyList(), true);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appenderConfiguration), root, Arrays.<LoggerConfiguration>asList());
        
        LoggingService manager = new LoggingService(configuration);
        Map<String, IAppender> appenders = Tests.get(manager, "appenders");
        assertThat(appenders.size(), is(1));
        AppenderMock appender = (AppenderMock)appenders.get("appender");
        manager.onTimer(Times.getCurrentTime());
        
        ILogger logger1 = manager.createLogger("test");
        logger1.log(LogLevel.INFO, messages.testLogFilter());
        Thread.sleep(100);
        assertThat(appender.events.size(), is(1));
        assertThat(appender.events.get(0).getStackTrace() != null, is(true));
    }
    
    @Test
    public void testTextAppender() throws Throwable
    {
        TestAppenderConfiguration appenderConfiguration = new TestAppenderConfiguration("appender", LogLevel.DEBUG, "<%message%>", 
            Collections.<String>emptyList());
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender"), 
            Collections.<String>emptyList(), false);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appenderConfiguration), root, Arrays.<LoggerConfiguration>asList());
        
        LoggingService manager = new LoggingService(configuration);
        Map<String, IAppender> appenders = Tests.get(manager, "appenders");
        assertThat(appenders.size(), is(1));
        AppenderMock appender = (AppenderMock)appenders.get("appender");
        manager.onTimer(Times.getCurrentTime());
        
        ILogger logger1 = manager.createLogger("test");
        logger1.log(LogLevel.INFO, new NonLocalizedMessage("test message"));
        Thread.sleep(100);
        assertThat(appender.messages.size(), is(1));
        assertThat(appender.messages.get(0), is("test message"));
    }
    
    @Test
    public void testLoggingContext() throws Throwable
    {
        LoggingContext.put("key1", "value1");
        assertThat((String)LoggingContext.get("key1"), is("value1"));
        
        LoggingContext.put("key2", "value2");
        LoggingContext.remove("key2");
        assertThat((String)LoggingContext.get("key1"), is("value1"));
        assertThat(LoggingContext.get("key2"), nullValue());
        
        LoggingContext.clear();
        assertThat(LoggingContext.get("key1"), nullValue());
        assertThat(LoggingContext.get("key2"), nullValue());
        
        LoggingContext.put("key1", "value1");
        
        TestAppenderConfiguration appenderConfiguration = new TestAppenderConfiguration("appender", LogLevel.DEBUG, "<%context.key1%>", 
            Collections.<String>emptyList());
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender"), 
            Collections.<String>emptyList(), false);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appenderConfiguration), root, Arrays.<LoggerConfiguration>asList());
        
        LoggingService manager = new LoggingService(configuration);
        Map<String, IAppender> appenders = Tests.get(manager, "appenders");
        assertThat(appenders.size(), is(1));
        AppenderMock appender = (AppenderMock)appenders.get("appender");
        manager.onTimer(Times.getCurrentTime());
        
        ILogger logger1 = manager.createLogger("test");
        logger1.log(LogLevel.INFO, new NonLocalizedMessage("test message"));
        Thread.sleep(100);
        assertThat(appender.messages.size(), is(1));
        assertThat(appender.messages.get(0), is("value1"));
    }
    
    @Test
    public void testLogContext() throws Throwable
    {
        TestAppenderConfiguration appenderConfiguration = new TestAppenderConfiguration("appender", LogLevel.DEBUG, "test", 
            Collections.<String>emptyList());
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender"), 
            Collections.<String>emptyList(), true);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appenderConfiguration), root, Arrays.<LoggerConfiguration>asList());
        
        LoggingService manager = new LoggingService(configuration);
        Map<String, IAppender> appenders = Tests.get(manager, "appenders");
        assertThat(appenders.size(), is(1));
        AppenderMock appender = (AppenderMock)appenders.get("appender");
        manager.onTimer(Times.getCurrentTime());
        
        ILogger logger1 = manager.createLogger(LoggerManagerTests.class.getName());
        logger1.log(LogLevel.INFO, Loggers.getMarker("testMarker"), new NonLocalizedMessage("test message"), 
            new Exception("testException", createCause(10, 10)));
        Thread.sleep(100);
        assertThat(appender.events.size(), is(1));
        assertThat(appender.events.get(0).getStackTrace() != null, is(true));
        
        Constructor init = LogContext.class.getDeclaredConstructor();
        init.setAccessible(true);
        LogContext context = (LogContext)init.newInstance();
        Method setEvent = LogContext.class.getDeclaredMethod("setEvent", LogEvent.class);
        setEvent.setAccessible(true);
        setEvent.invoke(context, appender.events.get(0));
        
        assertThat(LogContext.n, is(System.getProperty("line.separator")));
        assertThat(LogContext.separator, is(System.getProperty("line.separator")));
        assertThat(context.getLogger(), is(LoggerManagerTests.class.getName()));
        assertThat(context.logger(0), is(LoggerManagerTests.class.getSimpleName()));
        assertThat(context.logger(30), is("c.e.t.common.log.LoggerManagerTests"));
        assertThat(context.logger(100), is(LoggerManagerTests.class.getName()));
        assertThat(context.getLevel(), is("INFO"));
        assertThat(context.getMarker(), is("testMarker"));
        assertThat(context.getMessage(), is("test message"));
        assertThat(!context.getException().isEmpty(), is(true));
        assertThat(!context.exception(0).isEmpty(), is(true));
        assertThat(!context.exception(3).isEmpty(), is(true));
        assertThat(!context.exception(100).isEmpty(), is(true));
        assertThat(!context.getRootException().isEmpty(), is(true));
        assertThat(!context.rootException(0).isEmpty(), is(true));
        assertThat(!context.rootException(3).isEmpty(), is(true));
        assertThat(!context.rootException(100).isEmpty(), is(true));
        assertThat(context.getThread(), is(Thread.currentThread().getName()));
        assertThat(context.getContext() == LoggingContext.getContext(), is(true));
        assertThat(context.getClassName(), is(LoggerManagerTests.class.getName()));
        assertThat(context.className(0), is(LoggerManagerTests.class.getSimpleName()));
        assertThat(context.className(30), is("c.e.t.common.log.LoggerManagerTests"));
        assertThat(context.className(100), is(LoggerManagerTests.class.getName()));
        assertThat(context.getMethod(), is("testLogContext"));
        assertThat(context.getFile(), is("LoggerManagerTests.java"));
        assertThat(!context.getLine().isEmpty(), is(true));
        assertThat(!context.getCaller().isEmpty(), is(true));
        assertThat(!context.caller(0).isEmpty(), is(true));
        assertThat(!context.caller(3).isEmpty(), is(true));
        assertThat(!context.caller(100).isEmpty(), is(true));
        
        assertThat(context.pad("test", 6), is("test  "));
        assertThat(context.pad("test", -6), is("  test"));
        assertThat(context.pad("test", 3), is("test"));
        assertThat(context.pad("test", -3), is("test"));
        assertThat(context.truncate("test", 3), is("tes"));
        assertThat(context.truncate("test", -3), is("est"));
        assertThat(context.truncate("test", 5), is("test"));
        assertThat(context.truncate("test", -5), is("test"));
        
        assertThat(context.norm("test", 3, 3), is("tes"));
        assertThat(context.norm("te", 3, 3), is("te "));
        assertThat(context.norm("test", -3, -3), is("est"));
        assertThat(context.norm("te", -3, -3), is(" te"));
        assertThat(context.filter("hello*", "helo test"), is(false));
        assertThat(context.filter("hello*", "hello test"), is(true));
        
        int[] styles = new int[]{LogContext.s, LogContext.m, LogContext.l, LogContext.f};
        assertThat(!context.getTime().isEmpty(), is(true));
        assertThat(!context.getDate().isEmpty(), is(true));
        assertThat(!context.getDateTime().isEmpty(), is(true));
        for (int i = 0; i < 4; i++)
        {
            assertThat(!context.date(styles[i]).isEmpty(), is(true));
            assertThat(!context.time(styles[i]).isEmpty(), is(true));
        }
        
        for (int i = 0; i < 4; i++)
            for (int k = 0; k < 4; k++)
            assertThat(!context.dateTime(styles[i], styles[k]).isEmpty(), is(true));
        
        assertThat(!context.date("MM:HH").isEmpty(), is(true));
        
        assertThat(!context.getRelative().isEmpty(), is(true));
        styles = new int[]{LogContext.days, LogContext.hours, LogContext.minutes, LogContext.seconds, LogContext.milliseconds};
        for (int i = 0; i < styles.length; i++)
            assertThat(!context.relative(styles[i]).isEmpty(), is(true));
    }
    
    @Test
    public void testLogger() throws Throwable
    {
        ILogger logger = Loggers.get(getClass());
        logger.log(LogLevel.INFO, new NonLocalizedMessage("Test log message."));
    }
    
    private Throwable createCause(int count, int depth)
    {
        if (depth == 0)
            return new Exception("test exception " + count, count > 0 ? createCause(count - 1, 10) : null);
        
        return createCause(count, depth - 1);
    }
    
    public final class TestAppenderConfiguration extends TextAppenderConfiguration
    {
        public TestAppenderConfiguration(String name, LogLevel level, String template, List<String> filters)
        {
            super(name, level, template, filters);
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof TestAppenderConfiguration))
                return false;
            
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        @Override
        public IAppender createAppender(CompileContext compileContext, LogContext context, 
            Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
        {
            return new AppenderMock(this, compileContext, context, runtimeContext, templateRegistry);
        }
    }

    public static class AppenderMock extends TextAppender
    {
        boolean started;
        boolean stopped;
        boolean flushed;
        List<LogEvent> events = new ArrayList<LogEvent>();
        List<String> messages = new ArrayList<String>();
        
        public AppenderMock(TestAppenderConfiguration configuration, CompileContext compileContext, LogContext context, 
            Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
        {
            super(configuration.getLevel(), configuration.getTemplate(), configuration.getFilters(), compileContext, context,
                runtimeContext, templateRegistry);
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
        protected void doAppend(LogEvent event)
        {
            events.add(event);
            super.doAppend(event);
        }
        
        @Override
        protected void doAppend(String message)
        {
            messages.add(message);
        }

        @Override
        public void flush()
        {
            flushed = true;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Test.")
        ILocalizedMessage test();
        
        @DefaultMessage("Test helloLogger.")
        ILocalizedMessage testLogFilter();
        
        @DefaultMessage("Test helloAppender.")
        ILocalizedMessage testAppenderFilter();
        
        @DefaultMessage("Test helloAppender and helloLogger.")
        ILocalizedMessage testBothFilters();
    }
}
