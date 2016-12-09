package com.exametrika.common.mail;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.exametrika.common.utils.Threads;

public class Mailer
{
    private static final String CHARACTER_ENCODING = "UTF-8";
    private final Session session;
    private MailTransportStrategy transportStrategy = MailTransportStrategy.SMTP_PLAIN;
    private MailAddressValidationCriteria emailAddressValidationCriteria;
    private final long sendDelay;

    public Mailer(Session session)
    {
        this.session = session;
        this.emailAddressValidationCriteria = new MailAddressValidationCriteria(true, true);
        this.sendDelay = 1000;
    }

    public Mailer(String host, Integer port, String username, String password)
    {
        this(host, port, username, password, MailTransportStrategy.SMTP_PLAIN, 1000);
    }

    public Mailer(String host, Integer port, String username, String password,
        MailTransportStrategy transportStrategy, long sendDelay)
    {
        if (host == null || host.trim().equals(""))
            throw new MailException(MailException.MISSING_HOST);
        else if ((password != null && !password.trim().equals("")) && (username == null || username.trim().equals("")))
            throw new MailException(MailException.MISSING_USERNAME);

        this.transportStrategy = transportStrategy;
        this.session = createMailSession(host, port, username, password);
        this.emailAddressValidationCriteria = new MailAddressValidationCriteria(true, true);
        this.sendDelay = sendDelay;
    }

    public void setDebug(boolean debug)
    {
        session.setDebug(debug);
    }

    public void setEmailAddressValidationCriteria(MailAddressValidationCriteria emailAddressValidationCriteria)
    {
        this.emailAddressValidationCriteria = emailAddressValidationCriteria;
    }

    public void sendMail(MailMessage message) throws MailException
    {
        sendMail(Collections.singletonList(message));
    }
    
    public void sendMail(List<MailMessage> messages) throws MailException
    {
        try
        {
            Transport transport = session.getTransport();
            transport.connect();
            
            for (MailMessage message : messages)
            {
                validate(message);
                
                MimeEmailMessageWrapper messageRoot = new MimeEmailMessageWrapper();
                Message mailMessage = prepareMessage(message, messageRoot);
                
                mailMessage.saveChanges();
                transport.sendMessage(mailMessage, mailMessage.getAllRecipients());
                
                Threads.sleep(sendDelay);
            }
            
            transport.close();
        }
        catch (UnsupportedEncodingException e)
        {
            throw new MailException(String.format(MailException.INVALID_ENCODING, e.getMessage()));
        }
        catch (MessagingException e)
        {
            throw new MailException(String.format(MailException.GENERIC_ERROR, e.getMessage()), e);
        }
    }

    private void validate(MailMessage email) throws MailException
    {
        if (email.getText() == null && email.getTextHTML() == null)
            throw new MailException(MailException.MISSING_CONTENT);
        else if (email.getSubject() == null || email.getSubject().equals(""))
            throw new MailException(MailException.MISSING_SUBJECT);
        else if (email.getRecipients().size() == 0)
            throw new MailException(MailException.MISSING_RECIPIENT);
        else if (email.getFromRecipient() == null)
            throw new MailException(MailException.MISSING_SENDER);
        else
        {
            if (!MailValidator.isValid(email.getFromRecipient().getAddress(), emailAddressValidationCriteria))
                throw new MailException(String.format(MailException.INVALID_SENDER, email));
            
            for (MailRecipient recipient : email.getRecipients())
            {
                if (!MailValidator.isValid(recipient.getAddress(), emailAddressValidationCriteria))
                    throw new MailException(String.format(MailException.INVALID_RECIPIENT, email));
            }

            if (email.getReplyToRecipient() != null)
            {
                if (!MailValidator.isValid(email.getReplyToRecipient().getAddress(), emailAddressValidationCriteria))
                    throw new MailException(String.format(MailException.INVALID_REPLYTO, email));
            }
        }
    }

    private Session createMailSession(String host, Integer port, final String username, final String password)
    {
        if (transportStrategy == null)
            transportStrategy = MailTransportStrategy.SMTP_PLAIN;

        Properties props = transportStrategy.generateProperties();
        props.put(transportStrategy.propertyNameHost(), host);
        if (port != null)
            props.put(transportStrategy.propertyNamePort(), String.valueOf(port));
        else

        if (username != null)
            props.put(transportStrategy.propertyNameUsername(), username);

        if (password != null)
        {
            props.put(transportStrategy.propertyNameAuthenticate(), "true");
            return Session.getInstance(props, new Authenticator()
            {
                @Override
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(username, password);
                }
            });
        }
        else
            return Session.getInstance(props);
    }

    private Message prepareMessage(MailMessage email, MimeEmailMessageWrapper messageRoot)
        throws MessagingException, UnsupportedEncodingException
    {
        MimeMessage message = new MimeMessage(session);
        // set basic email properties
        message.setSubject(email.getSubject(), CHARACTER_ENCODING);
        message.setFrom(new InternetAddress(email.getFromRecipient().getAddress(), email.getFromRecipient().getName(),
            CHARACTER_ENCODING));
        setReplyTo(email, message);
        setRecipients(email, message);
        // fill multipart structure
        setTexts(email, messageRoot.multipartAlternativeMessages);
        setEmbeddedImages(email, messageRoot.multipartRelated);
        setAttachments(email, messageRoot.multipartRoot);
        message.setContent(messageRoot.multipartRoot);
        setHeaders(email, message);
        message.setSentDate(new Date());
        return message;
    }

    private void setRecipients(MailMessage email, Message message) throws UnsupportedEncodingException,
        MessagingException
    {
        for (MailRecipient recipient : email.getRecipients())
        {
            Address address = new InternetAddress(recipient.getAddress(), recipient.getName());
            message.addRecipient(recipient.getType(), address);
        }
    }

    private void setReplyTo(MailMessage email, Message message) throws UnsupportedEncodingException,
        MessagingException
    {
        MailRecipient replyToRecipient = email.getReplyToRecipient();
        if (replyToRecipient != null)
        {
            InternetAddress replyToAddress = new InternetAddress(replyToRecipient.getAddress(),
                replyToRecipient.getName(), CHARACTER_ENCODING);
            message.setReplyTo(new Address[]{replyToAddress});
        }
    }

    private void setTexts(MailMessage email, MimeMultipart multipartAlternativeMessages)
        throws MessagingException
    {
        if (email.getText() != null)
        {
            MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(email.getText(), CHARACTER_ENCODING);
            multipartAlternativeMessages.addBodyPart(messagePart);
        }
        if (email.getTextHTML() != null)
        {
            MimeBodyPart messagePartHTML = new MimeBodyPart();
            messagePartHTML.setContent(email.getTextHTML(), "text/html; charset=\"" + CHARACTER_ENCODING + "\"");
            multipartAlternativeMessages.addBodyPart(messagePartHTML);
        }
    }

    private void setEmbeddedImages(MailMessage email, MimeMultipart multipartRelated)
        throws MessagingException
    {
        for (MailAttachmentResource embeddedImage : email.getEmbeddedImages())
            multipartRelated.addBodyPart(getBodyPartFromDatasource(embeddedImage, Part.INLINE));
    }

    private void setAttachments(MailMessage email, MimeMultipart multipartRoot) throws MessagingException
    {
        for (MailAttachmentResource resource : email.getAttachments())
            multipartRoot.addBodyPart(getBodyPartFromDatasource(resource, Part.ATTACHMENT));
    }

    private void setHeaders(MailMessage email, Message message) throws UnsupportedEncodingException,
        MessagingException
    {
        for (Map.Entry<String, String> header : email.getHeaders().entrySet())
        {
            String headerName = header.getKey();
            String headerValue = MimeUtility.encodeText(header.getValue(), CHARACTER_ENCODING, null);
            String foldedHeaderValue = MimeUtility.fold(headerName.length() + 2, headerValue);
            message.addHeader(header.getKey(), foldedHeaderValue);
        }
    }

    private BodyPart getBodyPartFromDatasource(MailAttachmentResource resource, String dispositionType)
        throws MessagingException
    {
        BodyPart attachmentPart = new MimeBodyPart();
        DataSource ds = resource.getDataSource();

        attachmentPart.setDataHandler(new DataHandler(resource.getDataSource()));
        attachmentPart.setFileName(resource.getName());
        attachmentPart.setHeader("Content-Type",
            ds.getContentType() + "; filename=" + ds.getName() + "; name=" + ds.getName());
        attachmentPart.setHeader("Content-ID", String.format("<%s>", ds.getName()));
        attachmentPart.setDisposition(dispositionType + "; size=0");
        return attachmentPart;
    }

    private class MimeEmailMessageWrapper
    {
        private MimeMultipart multipartRoot;
        private MimeMultipart multipartRelated;
        private MimeMultipart multipartAlternativeMessages;

        MimeEmailMessageWrapper()
        {
            multipartRoot = new MimeMultipart("mixed");
            MimeBodyPart contentRelated = new MimeBodyPart();
            multipartRelated = new MimeMultipart("related");
            MimeBodyPart contentAlternativeMessages = new MimeBodyPart();
            multipartAlternativeMessages = new MimeMultipart("alternative");
            try
            {
                multipartRoot.addBodyPart(contentRelated);
                contentRelated.setContent(multipartRelated);
                multipartRelated.addBodyPart(contentAlternativeMessages);
                contentAlternativeMessages.setContent(multipartAlternativeMessages);
            }
            catch (MessagingException e)
            {
                throw new MailException(e.getMessage(), e);
            }
        }
    }
}