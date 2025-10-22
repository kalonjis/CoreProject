package be.steby.CoreProject.bll.domains.profile.exceptions.phone;

import be.steby.CoreProject.bll.domains.profile.exceptions.ProfileDomainException;

/**
 * Exception thrown when phone verification fails.
 * 
 * This exception is intentionally vague to prevent information disclosure
 * about whether the verification token or code was invalid. This approach
 * helps prevent timing attacks and reduces the ability for attackers to
 * enumerate valid verification states.
 * 
 * Possible causes (not disclosed to client):
 * - Invalid verification code
 * - Expired verification token
 * - Malformed verification token
 * - Token/code mismatch
 * - Too many verification attempts
 */
public class PhoneVerificationFailedException extends ProfileDomainException {

    /**
     * Creates a new phone verification failed exception with default message.
     * Uses a generic message to avoid information disclosure.
     */
    public PhoneVerificationFailedException() {
        super("Phone verification failed", 400);
    }

    /**
     * Creates a new phone verification failed exception with custom message.
     * 
     * @param message The error message (should remain generic for security)
     */
    public PhoneVerificationFailedException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new phone verification failed exception with message and cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public PhoneVerificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}