/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link SkipElementLoader} is an {@link IElementLoader} that skips element.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SkipElementLoader extends AbstractElementLoader
{
    @Override
    public void loadElement(JsonObject element, ILoadContext context)
    {
    }
}
