/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Pattern;

import com.exametrika.common.utils.Strings;



/**
 * The {@link TcpNameFilter} represents a filter by IP address or host name.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpNameFilter
{
    private final Pattern pattern;
    private final boolean nameBased;
    private final boolean canonical;
    private final List<TcpNameFilter> includeFilters;
    private final List<TcpNameFilter> excludeFilters;
    
    /**
     * Creates a filter.
     *
     * @param filterExpression filter expression. Filter expression has the following format:
     * [ip:]glob_pattern | [ip:]#reg_exp_pattern. Where:
     * <li> ip: - IP address is used in filter instead of host name
     * <li> glob_pattern in format {@link Strings#globToRegEx}
     * <li> reg_exp_pattern - regular expression pattern  
     * @param canonical if true canonical host name is used
     */
    public TcpNameFilter(String filterExpression, boolean canonical)
    {
        this(filterExpression, canonical, null, null);
    }

    /**
     * Creates a filter.
     *
     * @param includeFilters filters to include. Can be null if not used
     * @param excludeFilters filters to exclude. Can be null if not used
     */
    public TcpNameFilter(List<TcpNameFilter> includeFilters, List<TcpNameFilter> excludeFilters)
    {
        this(null, false, includeFilters, excludeFilters);
    }
    
    /**
     * Creates a filter.
     *
     * @param filterExpression filter expression. Filter expression has the following format:
     * [ip:]glob_pattern | [ip:]#reg_exp_pattern. Where:
     * <li> ip: - IP address is used in filter instead of host name
     * <li> glob_pattern in format {@link Strings#globToRegEx}
     * <li> reg_exp_pattern - regular expression pattern
     * @param canonical if true canonical host name is used
     * @param includeFilters filters to include. Can be null if not used
     * @param excludeFilters filters to exclude. Can be null if not used
     */
    public TcpNameFilter(String filterExpression, boolean canonical, List<TcpNameFilter> includeFilters, List<TcpNameFilter> excludeFilters)
    {
        if (filterExpression != null && filterExpression.length() > 0)
        {
            if (filterExpression.startsWith("ip:"))
            {
                nameBased = false;
                filterExpression = filterExpression.substring(3);
            }
            else
                nameBased = true;
            
            pattern = Strings.createFilterPattern(filterExpression, false);
        }
        else if (includeFilters != null && !includeFilters.isEmpty())
        {
            pattern = Pattern.compile("");
            nameBased = false;
        }
        else
        {
            pattern = null;
            nameBased = false;
        }
        
        this.canonical = canonical;
        this.includeFilters = includeFilters;
        this.excludeFilters = excludeFilters;
    }

    /**
     * Matches specified address against this filter.
     *
     * @param remoteAddress address to match
     * @return true if address matches the filter
     */
    public boolean match(InetSocketAddress remoteAddress)
    {
        boolean res = matchAddress(remoteAddress);
        
        if (!res && includeFilters != null && !includeFilters.isEmpty())
        {
            for (TcpNameFilter filter : includeFilters)
            {
                if (filter.match(remoteAddress))
                {
                    res = true;
                    break;
                }
            }
        }
        
        if (res && excludeFilters != null && !excludeFilters.isEmpty())
        {
            for (TcpNameFilter filter : excludeFilters)
            {
                if (filter.match(remoteAddress))
                {
                    res = false;
                    break;
                }
            }
        }
        
        return res;
    }

    private boolean matchAddress(InetSocketAddress remoteAddress)
    {
        if (pattern == null)
            return true;
        
        if (nameBased)
            return pattern.matcher((canonical ? remoteAddress.getAddress().getCanonicalHostName() : remoteAddress.getAddress().getHostName()) + 
                ":" + remoteAddress.getPort()).matches();
        else
            return pattern.matcher(remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort()).matches();
    }
}
