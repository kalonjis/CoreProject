package be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions;

public class SipConfigValidationException extends SipConfigDomainException {

    private static final String ERROR_CODE = "SIP_CONFIG_VALIDATION";

    @Override
    public String getErrorCode() { return ERROR_CODE; }

    public SipConfigValidationException(String message) { super(message, 400); }

    public static SipConfigValidationException usernameAlreadyTaken(String sipUsername) {
        return new SipConfigValidationException(
                "SIP username '" + sipUsername + "' is already assigned to another commercial");
    }

    public static SipConfigValidationException alreadyConfigured(String userPublicId) {
        return new SipConfigValidationException(
                "Commercial '" + userPublicId + "' already has a SIP config — use PUT to update it");
    }
}
