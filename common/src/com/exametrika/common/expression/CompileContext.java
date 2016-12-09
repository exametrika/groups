/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import com.exametrika.common.expression.impl.StandardClassResolver;
import com.exametrika.common.expression.impl.StandardCollectionProvider;
import com.exametrika.common.expression.impl.StandardConversionProvider;
import com.exametrika.common.utils.Assert;






/**
 * The {@link CompileContext} is an expression compile context.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CompileContext
{
    private final ICollectionProvider collectionProvider;
    private final IClassResolver classResolver;
    private final IConversionProvider conversionProvider;
    
    public CompileContext()
    {
        this(new StandardCollectionProvider(), new StandardClassResolver(), new StandardConversionProvider());
    }
    
    public CompileContext(ICollectionProvider collectionProvider, IClassResolver classResolver, IConversionProvider conversionProvider)
    {
        Assert.notNull(collectionProvider);
        Assert.notNull(classResolver);
        Assert.notNull(conversionProvider);
        
        this.collectionProvider = collectionProvider;
        this.classResolver = classResolver;
        this.conversionProvider = conversionProvider;
    }

    public ICollectionProvider getCollectionProvider()
    {
        return collectionProvider;
    }

    public IClassResolver getClassResolver()
    {
        return classResolver;
    }

    public IConversionProvider getConversionProvider()
    {
        return conversionProvider;
    }
}
