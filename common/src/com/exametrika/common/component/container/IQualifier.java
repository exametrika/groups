/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

import java.util.Map;

/**
 * The {@link IQualifier} is a qualifier. Can be used as qualifier value to match component for specified
 * criteria given by qualifier.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IQualifier
{
    /**
     * Matches specified qualifier to criteria of current qualifier.
     *
     * @param qualifierName name of current qualifier
     * @param qualifiers map of qualifiers that can be matched to current qualifier
     * @return true if specified qualifiers matches to current qualifier
     */
    boolean match(String qualifierName, Map<String, ?> qualifiers);
}
