/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.parsers;

import com.exametrika.common.config.IConfigurationParser;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;


/**
 * The {@link JsonConfigurationParser} is an implementation of {@link IConfigurationParser} for json format.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JsonConfigurationParser implements IConfigurationParser
{
    @Override
    public JsonObjectBuilder parse(String contents)
    {
        Assert.notNull(contents);

        try
        {
            return (JsonObjectBuilder)JsonSerializers.read(contents, true);
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException(e);
        }
    }

    @Override
    public String escape(String value)
    {
        return JsonUtils.escape(value);
    }
}
