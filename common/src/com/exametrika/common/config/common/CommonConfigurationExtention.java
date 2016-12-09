/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.common;

import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;





/**
 * The {@link CommonConfigurationExtention} is a helper class that is used to load {@link CommonConfiguration}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CommonConfigurationExtention implements IConfigurationLoaderExtension
{
    @Override
    public Parameters getParameters()
    {
        Parameters parameters = new Parameters();
        CommonConfigurationLoader processor = new CommonConfigurationLoader();
        parameters.elementLoaders.put("common", processor);
        parameters.typeLoaders.put("NameFilter", processor);
        parameters.typeLoaders.put("CompoundNameFilterExpression", processor);
        parameters.contextFactories.put(CommonConfiguration.SCHEMA, new CommonLoadContext());
        parameters.schemaMappings.put(CommonConfiguration.SCHEMA, 
            new Pair("classpath:" + Classes.getResourcePath(CommonConfiguration.class) + "/common.schema", false));
        parameters.topLevelElements.put("common", new Pair("Common", false));
        return parameters;
    }
}
