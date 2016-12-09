/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl.config;

import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.json.schema.JsonPathReferenceValidator;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;





/**
 * The {@link LoggingConfigurationExtention} is a helper class that is used to load {@link LoggingConfiguration}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class LoggingConfigurationExtention implements IConfigurationLoaderExtension
{
    @Override
    public Parameters getParameters()
    {
        Parameters parameters = new Parameters();
        parameters.elementLoaders.put("logging", new LoggingConfigurationLoader());
        parameters.contextFactories.put(LoggingConfiguration.SCHEMA, new LoggingLoadContext());
        parameters.schemaMappings.put(LoggingConfiguration.SCHEMA, 
            new Pair("classpath:" + Classes.getResourcePath(LoggingConfiguration.class) + "/logging.schema", false));
        parameters.validators.put("appenderReference", new JsonPathReferenceValidator("logging.appenders"));
        parameters.topLevelElements.put("logging", new Pair("Logging", false));
        return parameters;
    }
}
