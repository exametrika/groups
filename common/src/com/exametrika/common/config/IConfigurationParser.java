/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import com.exametrika.common.json.JsonObjectBuilder;

/**
 * The {@link IConfigurationParser} is used to parse configuration.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IConfigurationParser
{
    /**
     * Parses configuration.
     *
     * @param contents contents of configuration
     * @return parsed configuration
     */
    JsonObjectBuilder parse(String contents);
    
    /**
     * Escapes given value accordingly to parser's rules.
     *
     * @param value valute to escape
     * @return escaped value
     */
    String escape(String value);
}
