/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

/**
 * The {@link IShellPromptProvider} is a provider of shell prompt.
 * 
 * @author Medvedev-A
 */
public interface IShellPromptProvider
{
    /**
     * Returns left and right shell prompts.
     *
     * @param context context
     * @return shell prompts, first - left (optional can be null), second - right (optional can be null)
     */
    String[] getPrompt(IShellContext context);
}