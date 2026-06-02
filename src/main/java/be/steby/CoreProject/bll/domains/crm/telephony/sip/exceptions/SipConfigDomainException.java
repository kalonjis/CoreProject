package be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

public abstract class SipConfigDomainException extends CoreProjectException {

    private static final String ERROR_CODE = "SIP_CONFIG_DOMAIN";

    @Override
    public String getErrorCode() { return ERROR_CODE; }

    public SipConfigDomainException(String message) { super(message, 400); }
    public SipConfigDomainException(String message, int status) { super(message, status); }
}
