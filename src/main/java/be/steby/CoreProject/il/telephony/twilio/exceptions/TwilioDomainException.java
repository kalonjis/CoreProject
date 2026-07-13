package be.steby.CoreProject.il.telephony.twilio.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

/**
 * Base exception for all Twilio integration exceptions.
 *
 * <p>Mirrors the BLL per-domain exception pattern for the Twilio
 * infrastructure domain. Raised by IL services (webhook handler,
 * token service) and translated to HTTP responses by the global
 * {@code ControllerAdvisor} — controllers never handle these.</p>
 *
 * <h3>Exception hierarchy</h3>
 * <pre>
 * CoreProjectException
 * └── TwilioDomainException
 *     └── InvalidTwilioSignatureException
 * </pre>
 *
 * @see CoreProjectException
 */
public abstract class TwilioDomainException extends CoreProjectException {

    private static final String ERROR_CODE = "TWILIO_DOMAIN";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public TwilioDomainException(String message) {
        super(message, 400);
    }

    public TwilioDomainException(String message, int status) {
        super(message, status);
    }

    public TwilioDomainException(String message, Throwable cause) {
        super(message, 400, cause);
    }

    public TwilioDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}
