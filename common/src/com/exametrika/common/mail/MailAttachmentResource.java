package com.exametrika.common.mail;

import javax.activation.DataSource;

import com.exametrika.common.utils.Assert;

public class MailAttachmentResource
{
    private final String name;
    private final DataSource dataSource;

    public MailAttachmentResource(String name, DataSource dataSource)
    {
        Assert.notNull(name);
        Assert.notNull(dataSource);
        
        this.name = name;
        this.dataSource = dataSource;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public String getName()
    {
        return name;
    }
}