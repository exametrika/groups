/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.List;

/**
 * The {@link IShellParameterCompleter} is used to complete parameter value.
 * 
 * @author Medvedev-A
 */
public interface IShellParameterCompleter
{
    /** Completion candidate. */
    class Candidate
    {
        /** Value to be inserted.*/
        public String value;
        /**Display name.*/
        public String displayName;
        /** Description.*/
        public String description;
    }
    
    /**
     * Reurns variants of parameter value completion.
     *
     * @param context context
     * @param value string parameter representation
     * @return variants of parameter value completion
     */
    List<Candidate> complete(IShellContext context, String value);
}