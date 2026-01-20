package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.dl.enums.TwoFactorType;

import java.util.List;

/**
 * Exception thrown when 2FA verification code delivery fails.
 *
 * Contains information about:
 * - The method that failed (EMAIL, SMS)
 * - The reason for failure
 * - Available alternative methods the user can try
 *
 * This exception is thrown during:
 * - chooseTwoFactorMethod (when code delivery fails)
 * - resendTwoFactorCode (when resend fails)
 *
 * The presentation layer should catch this and return 503 with alternatives.
 *
 * @author Steby Team
 * @since 2.0.0
 */
public class TwoFactorCodeDeliveryException extends CoreProjectException {

    private final TwoFactorType failedMethod;
    private final List<TwoFactorType> alternativeMethods;

    public TwoFactorCodeDeliveryException(
            String message,
            TwoFactorType failedMethod,
            List<TwoFactorType> alternativeMethods) {
        super(message);
        this.failedMethod = failedMethod;
        this.alternativeMethods = alternativeMethods != null ? alternativeMethods : List.of();
    }

    public TwoFactorCodeDeliveryException(
            String message,
            TwoFactorType failedMethod,
            List<TwoFactorType> alternativeMethods,
            Throwable cause) {
        super(message, cause);
        this.failedMethod = failedMethod;
        this.alternativeMethods = alternativeMethods != null ? alternativeMethods : List.of();
    }

    /**
     * Gets the 2FA method that failed to deliver.
     *
     * @return the failed method type
     */
    public TwoFactorType getFailedMethod() {
        return failedMethod;
    }

    /**
     * Gets the available alternative 2FA methods.
     *
     * @return list of alternative methods (may be empty)
     */
    public List<TwoFactorType> getAlternativeMethods() {
        return alternativeMethods;
    }

    /**
     * Checks if there are alternative methods available.
     *
     * @return true if alternatives exist
     */
    public boolean hasAlternatives() {
        return !alternativeMethods.isEmpty();
    }
}