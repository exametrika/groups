package com.exametrika.common.mail;

import javax.mail.Message.RecipientType;

import com.exametrika.common.utils.Assert;

public class MailRecipient
{
    private final String name;
    private final String address;
    private final RecipientType type;

    public MailRecipient(String name, String address, RecipientType type)
    {
        Assert.notNull(name);
        Assert.notNull(address);
        
        this.name = name;
        this.address = address;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public String getAddress()
    {
        return address;
    }

    public RecipientType getType()
    {
        return type;
    }
}