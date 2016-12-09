package com.exametrika.common.mail;

import java.util.regex.Pattern;

public class MailValidator
{
    public static boolean isValid(String email)
    {
        return isValid(email, new MailAddressValidationCriteria(true, true));
    }

    public static boolean isValid(String email, MailAddressValidationCriteria emailAddressValidationCriteria)
    {
        return buildValidEmailPattern(emailAddressValidationCriteria).matcher(email).matches();
    }

    protected static Pattern buildValidEmailPattern(MailAddressValidationCriteria parameterObject)
    {
        // RFC 2822 2.2.2 Structured Header Field Bodies
        String wsp = "[ \\t]"; // space or tab
        String fwsp = wsp + "*";
        // RFC 2822 3.2.1 Primitive tokens
        String dquote = "\\\"";
        // ASCII Control characters excluding white space:
        String noWsCtl = "\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F";
        // all ASCII characters except CR and LF:
        String asciiText = "[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F]";
        // RFC 2822 3.2.2 Quoted characters:
        // single backslash followed by a text char
        String quotedPair = "(\\\\" + asciiText + ")";
        // RFC 2822 3.2.4 Atom:
        String atext = "[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]";
        String atom = fwsp + atext + "+" + fwsp;
        String dotAtomText = atext + "+" + "(" + "\\." + atext + "+)*";
        String dotAtom = fwsp + "(" + dotAtomText + ")" + fwsp;
        // RFC 2822 3.2.5 Quoted strings:
        // noWsCtl and the rest of ASCII except the doublequote and backslash characters:
        String qtext = "[" + noWsCtl + "\\x21\\x23-\\x5B\\x5D-\\x7E]";
        String qcontent = "(" + qtext + "|" + quotedPair + ")";
        String quotedString = dquote + "(" + fwsp + qcontent + ")*" + fwsp + dquote;
        // RFC 2822 3.2.6 Miscellaneous tokens
        String word = "((" + atom + ")|(" + quotedString + "))";
        String phrase = word + "+"; // one or more words.
        // RFC 1035 tokens for domain names:
        String letter = "[a-zA-Z]";
        String letDig = "[a-zA-Z0-9]";
        String letDigHyp = "[a-zA-Z0-9-]";
        String rfcLabel = letDig + "(" + letDigHyp + "{0,61}" + letDig + ")?";
        String rfc1035DomainName = rfcLabel + "(\\." + rfcLabel + ")*\\." + letter + "{2,6}";
        // RFC 2822 3.4 Address specification
        // domain text - non white space controls and the rest of ASCII chars not including [, ], or \:
        String dtext = "[" + noWsCtl + "\\x21-\\x5A\\x5E-\\x7E]";
        String dcontent = dtext + "|" + quotedPair;
        String domainLiteral = "\\[" + "(" + fwsp + dcontent + "+)*" + fwsp + "\\]";
        String rfc2822Domain = "(" + dotAtom + "|" + domainLiteral + ")";
        String domain = parameterObject.isAllowDomainLiterals() ? rfc2822Domain : rfc1035DomainName;
        String localPart = "((" + dotAtom + ")|(" + quotedString + "))";
        String addrSpec = localPart + "@" + domain;
        String angleAddr = "<" + addrSpec + ">";
        String nameAddr = "(" + phrase + ")?" + fwsp + angleAddr;
        String mailbox = nameAddr + "|" + addrSpec;
        // now compile a pattern for efficient re-use:
        // if we're allowing quoted identifiers or not:
        String patternString = parameterObject.isAllowQuotedIdentifiers() ? mailbox : addrSpec;
        return Pattern.compile(patternString);
    }
    
    private MailValidator()
    {
    }
}