package com.exametrika.common.mail;

public class MailAddressValidationCriteria
{
    private final boolean allowDomainLiterals;
    private final boolean allowQuotedIdentifiers;

    public MailAddressValidationCriteria(boolean allowDomainLiterals, boolean allowQuotedIdentifiers)
    {
        this.allowDomainLiterals = allowDomainLiterals;
        this.allowQuotedIdentifiers = allowQuotedIdentifiers;
    }

    public final boolean isAllowDomainLiterals()
    {
        return allowDomainLiterals;
    }

    public final boolean isAllowQuotedIdentifiers()
    {
        return allowQuotedIdentifiers;
    }
}