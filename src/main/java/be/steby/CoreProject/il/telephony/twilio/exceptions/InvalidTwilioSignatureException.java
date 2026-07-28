package be.steby.CoreProject.il.telephony.twilio.exceptions;

/**
 * Exception thrown when a request claiming to come from Twilio fails
 * {@code X-Twilio-Signature} verification.
 *
 * <p>Typical causes:</p>
 * <ul>
 *   <li>Missing {@code X-Twilio-Signature} header</li>
 *   <li>Signature computed with a different Auth Token (forged request)</li>
 *   <li>Signature valid for a different URL or parameter set (replay)</li>
 * </ul>
 *
 * <p>HTTP Status: 403 (Forbidden)</p>
 */
public class InvalidTwilioSignatureException extends TwilioDomainException {

    private static final String ERROR_CODE = "INVALID_TWILIO_SIGNATURE";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public InvalidTwilioSignatureException(String message) {
        super(message, 403);
    }
}
