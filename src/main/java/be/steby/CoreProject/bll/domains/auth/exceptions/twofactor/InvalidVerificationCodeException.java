package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;
import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Exception thrown when an invalid verification code is provided during 2FA operations.
 * 
 * This exception is typically thrown during:
 * - 2FA activation verification (setup phase)
 * - 2FA login verification 
 * - 2FA code resend verification
 * 
 * The exception indicates that the provided code does not match the expected code,
 * is malformed, or has expired.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class InvalidVerificationCodeException extends AuthenticationException {

    /**
     * Constructs a new InvalidVerificationCodeException with the specified detail message.
     * 
     * @param message the detail message explaining the verification failure
     */
    public InvalidVerificationCodeException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidVerificationCodeException with the specified detail message
     * and cause.
     * 
     * @param message the detail message explaining the verification failure
     * @param cause the cause of the verification failure
     */
    public InvalidVerificationCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new InvalidVerificationCodeException with a default message.
     */
    public InvalidVerificationCodeException() {
        super("The provided verification code is invalid or has expired");
    }
}