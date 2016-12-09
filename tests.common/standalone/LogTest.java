import java.util.Arrays;
import java.util.Collections;

import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration.Target;
import com.exametrika.common.log.config.LoggerConfiguration;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.log.impl.ILoggingService;
import com.exametrika.tests.common.log.LoggerManagerTests;


public class LogTest
{
    public static void main(String[] args) throws Throwable
    {
        ConsoleAppenderConfiguration appenderConfiguration = new ConsoleAppenderConfiguration("appender", LogLevel.TRACE, 
            "<%@template(short)%>", 
            Collections.<String>emptyList(), Target.OUTPUT, true);
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.TRACE, Arrays.<String>asList("appender"), 
            Collections.<String>emptyList(), true);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appenderConfiguration), root, Arrays.<LoggerConfiguration>asList());
        
        ILoggingService service = (ILoggingService)Loggers.getLoggerFactory();
        service.setConfiguration(configuration);
        
        ILogger logger1 = service.createLogger(LoggerManagerTests.class.getName());
        logger1.log(LogLevel.TRACE, Loggers.getMarker("testMarker"), new NonLocalizedMessage("test message"), 
            new Exception("testException", createCause(10, 10)));
        logger1.log(LogLevel.ERROR, Loggers.getMarker("testMarker"), new NonLocalizedMessage("test message"), 
            new Exception("testException", createCause(10, 10)));
    }
    
    private static Throwable createCause(int count, int depth)
    {
        if (depth == 0)
            return new Exception("test exception " + count, count > 0 ? createCause(count - 1, 10) : null);
        
        return createCause(count, depth - 1);
    }
}
