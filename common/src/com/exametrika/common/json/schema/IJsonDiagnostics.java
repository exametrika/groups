/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.l10n.ILocalizedMessage;




/**
 * The {@link IJsonDiagnostics} represents a JSON diagnostics object.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IJsonDiagnostics
{
    /**
     * Returns diagnostics path.
     *
     * @return diagnostics path
     */
    String getPath();
    
    /**
     * Adds error to diagnostics.
     *
     * @param error error
     */
    void addError(ILocalizedMessage error);
}
