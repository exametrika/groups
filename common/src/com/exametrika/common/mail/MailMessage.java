package com.exametrika.common.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.Message.RecipientType;
import javax.mail.util.ByteArrayDataSource;

public class MailMessage
{
    private MailRecipient fromRecipient;
    private MailRecipient replyToRecipient;
    private String text;
    private String textHTML;
    private String subject;
    private List<MailRecipient> recipients;
    private List<MailAttachmentResource> embeddedImages;
    private List<MailAttachmentResource> attachments;
    private Map<String, String> headers;

    public MailMessage()
    {
        recipients = new ArrayList<MailRecipient>();
        embeddedImages = new ArrayList<MailAttachmentResource>();
        attachments = new ArrayList<MailAttachmentResource>();
        headers = new HashMap<String, String>();
    }

    public MailRecipient getFromRecipient()
    {
        return fromRecipient;
    }

    public MailRecipient getReplyToRecipient()
    {
        return replyToRecipient;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getText()
    {
        return text;
    }

    public String getTextHTML()
    {
        return textHTML;
    }

    public List<MailAttachmentResource> getAttachments()
    {
        return Collections.unmodifiableList(attachments);
    }

    public List<MailAttachmentResource> getEmbeddedImages()
    {
        return Collections.unmodifiableList(embeddedImages);
    }

    public List<MailRecipient> getRecipients()
    {
        return Collections.unmodifiableList(recipients);
    }

    public Map<String, String> getHeaders()
    {
        return Collections.unmodifiableMap(headers);
    }
    
    public void setFromAddress(String name, String fromAddress)
    {
        fromRecipient = new MailRecipient(name, fromAddress, null);
    }

    public void setReplyToAddress(String name, String replyToAddress)
    {
        replyToRecipient = new MailRecipient(name, replyToAddress, null);
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setTextHTML(String textHTML)
    {
        this.textHTML = textHTML;
    }

    public void addRecipient(String name, String address, RecipientType type)
    {
        recipients.add(new MailRecipient(name, address, type));
    }

    public void addEmbeddedImage(String name, byte[] data, String mimetype)
    {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(data, mimetype);
        dataSource.setName(name);
        addEmbeddedImage(name, dataSource);
    }

    public void addEmbeddedImage(String name, DataSource imagedata)
    {
        embeddedImages.add(new MailAttachmentResource(name, imagedata));
    }

    public void addHeader(String name, Object value)
    {
        headers.put(name, String.valueOf(value));
    }

    public void addAttachment(String name, byte[] data, String mimetype)
    {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(data, mimetype);
        dataSource.setName(name);
        addAttachment(name, dataSource);
    }

    public void addAttachment(String name, DataSource filedata)
    {
        attachments.add(new MailAttachmentResource(name, filedata));
    }
}