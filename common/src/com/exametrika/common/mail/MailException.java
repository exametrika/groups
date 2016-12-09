package com.exametrika.common.mail;

import javax.mail.MessagingException;

public class MailException extends RuntimeException
{
    protected static String GENERIC_ERROR = "Generic error: %s";
    protected static String MISSING_HOST = "Can't send an email without host";
    protected static String MISSING_USERNAME = "Can't have a password without username";
    protected static String INVALID_ENCODING = "Encoding not accepted: %s";
    protected static String INVALID_RECIPIENT = "Invalid TO address: %s";
    protected static String INVALID_REPLYTO = "Invalid REPLY TO address: %s";
    protected static String INVALID_SENDER = "Invalid FROM address: %s";
    protected static String MISSING_SENDER = "Email is not valid: missing sender";
    protected static String MISSING_RECIPIENT = "Email is not valid: missing recipients";
    protected static String MISSING_SUBJECT = "Email is not valid: missing subject";
    protected static String MISSING_CONTENT = "Email is not valid: missing content body";

    protected MailException(String message)
    {
        super(message);
    }

    protected MailException(String message, MessagingException cause)
    {
        super(message, cause);
    }
}