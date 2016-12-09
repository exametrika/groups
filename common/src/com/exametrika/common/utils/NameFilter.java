/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.exametrika.common.config.Configuration;



/**
 * The {@link NameFilter} represents a filter by name.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NameFilter extends Configuration
{
    private final String filterExpression;
    private final Pattern pattern;
    private final List<NameFilter> includeFilters;
    private final List<NameFilter> excludeFilters;
    
    /**
     * Creates a filter.
     *
     * @param filterExpression filter expression. Filter expression has the following format:
     * glob_pattern | #reg_exp_pattern. Where:
     * <li> glob_pattern in format {@link Strings#globToRegEx}
     * <li> reg_exp_pattern - regular expression pattern  
     */
    public NameFilter(String filterExpression)
    {
        this(filterExpression, null, null);
    }

    /**
     * Creates a filter.
     *
     * @param includeFilters filters to include. Can be null if not used
     * @param excludeFilters filters to exclude. Can be null if not used
     */
    public NameFilter(List<NameFilter> includeFilters, List<NameFilter> excludeFilters)
    {
        this(null, includeFilters, excludeFilters);
    }
    
    /**
     * Creates a filter.
     *
     * @param filterExpression filter expression. Filter expression has the following format:
     * glob_pattern | #reg_exp_pattern. Where:
     * <li> glob_pattern in format {@link Strings#globToRegEx}
     * <li> reg_exp_pattern - regular expression pattern
     * @param includeFilters filters to include. Can be null if not used
     * @param excludeFilters filters to exclude. Can be null if not used
     */
    public NameFilter(String filterExpression, List<NameFilter> includeFilters, List<NameFilter> excludeFilters)
    {
        if (filterExpression != null && filterExpression.length() > 0)
            pattern = Strings.createFilterPattern(filterExpression, false);
        else if (includeFilters != null && !includeFilters.isEmpty())
            pattern = Pattern.compile("");
        else
            pattern = null;
        
        this.filterExpression = filterExpression;
        this.includeFilters = includeFilters;
        this.excludeFilters = excludeFilters;
    }

    /**
     * Creates a filter.
     *
     * @param includeFilters filters to include. Can be null if not used
     * @param excludeFilters filters to exclude. Can be null if not used
     * @return filter
     */
    public static NameFilter toFilter(List<String> includeFilters, List<String> excludeFilters)
    {
        return new NameFilter(toFilters(includeFilters), toFilters(excludeFilters));
    }
    
    /**
     * Matches specified name against this filter.
     *
     * @param name name to match
     * @return true if name matches the filter
     */
    public boolean match(String name)
    {
        boolean res = matchName(name);
        
        if (!res && includeFilters != null && !includeFilters.isEmpty())
        {
            for (NameFilter filter : includeFilters)
            {
                if (filter.match(name))
                {
                    res = true;
                    break;
                }
            }
        }
        
        if (res && excludeFilters != null && !excludeFilters.isEmpty())
        {
            for (NameFilter filter : excludeFilters)
            {
                if (filter.match(name))
                {
                    res = false;
                    break;
                }
            }
        }
        
        return res;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof NameFilter))
            return false;
        
        NameFilter filter = (NameFilter)o;
        return Objects.equals(filterExpression, filter.filterExpression) && Objects.equals(includeFilters, filter.includeFilters) &&
            Objects.equals(excludeFilters, filter.excludeFilters);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(filterExpression, includeFilters, excludeFilters);
    }

    private boolean matchName(String name)
    {
        if (pattern == null)
            return true;
        
        return pattern.matcher(name).matches();
    }
    
    private static List<NameFilter> toFilters(List<String> list)
    {
        if (list == null)
            return null;
        
        List<NameFilter> filters = new ArrayList<NameFilter>();
        for (String value : list)
            filters.add(new NameFilter(value));
        
        return filters;
    }
}
