package be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions;

public class SipConfigNotFoundException extends SipConfigDomainException {

    private static final String ERROR_CODE = "SIP_CONFIG_NOT_FOUND";

    @Override
    public String getErrorCode() { return ERROR_CODE; }

    public SipConfigNotFoundException(String message) { super(message, 404); }

    public static SipConfigNotFoundException forUser(String userPublicId) {
        return new SipConfigNotFoundException("No SIP config found for user: " + userPublicId);
    }

    public static SipConfigNotFoundException byPublicId(String publicId) {
        return new SipConfigNotFoundException("SIP config not found with publicId: " + publicId);
    }
}
