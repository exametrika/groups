/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.common;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;



/**
 * The {@link CommonConfiguration} is common configuration.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class CommonConfiguration extends Configuration
{
    public static final String SCHEMA = "com.exametrika.common-1.0";
    
    private final RuntimeMode runtimeMode;

    public CommonConfiguration()
    {
        this(RuntimeMode.DEVELOPMENT);
    }
    
    public CommonConfiguration(RuntimeMode runtimeMode)
    {
        Assert.notNull(runtimeMode);
        
        this.runtimeMode = runtimeMode;
    }
    
    public RuntimeMode getRuntimeMode()
    {
        return runtimeMode;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof CommonConfiguration))
            return false;
        
        CommonConfiguration configuration = (CommonConfiguration)o;
        return runtimeMode.equals(configuration.runtimeMode);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(runtimeMode);
    }
}
