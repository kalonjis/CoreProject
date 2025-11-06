package be.steby.CoreProject.bll.common.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Exception thrown when a JWT token has an invalid or unexpected purpose.
 * 
 * <p>This exception is used to indicate that a token was validated successfully
 * but its purpose claim doesn't match what was expected. This provides an
 * additional layer of security by ensuring tokens are used for their intended
 * purpose only.</p>
 * 
 * <p>Example scenarios where this exception might be thrown:</p>
 * <ul>
 *   <li>Using a 2FA token where an access token was expected</li>
 *   <li>Using a password reset token for phone verification</li>
 *   <li>Token missing a purpose claim entirely</li>
 *   <li>Attempting to use expired tokens for different purposes</li>
 * </ul>
 * 
 * <p>This exception extends CoreProjectException to integrate with the global
 * exception handling system via the ControllerAdvisor. When thrown, it will
 * automatically be caught and converted to an appropriate HTTP response with
 * the configured status code and error message.</p>
 * 
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Uses 401 (Unauthorized) status by default for authentication issues</li>
 *   <li>Provides clear error messages for debugging without exposing sensitive data</li>
 *   <li>Integrates with application-wide exception handling for consistent responses</li>
 * </ul>
 * 
 * @author Steby Team
 * @since 2.0.0
 * @see be.steby.CoreProject.bll.exceptions.CoreProjectException
 * @see be.steby.CoreProject.pl.advisor.ControllerAdvisor
 */
public class InvalidTokenPurposeException extends CoreProjectException {
    
    /**
     * Constructs a new InvalidTokenPurposeException with the specified detail message.
     * 
     * <p>Uses HTTP status 401 (Unauthorized) as this typically indicates an
     * authentication/authorization problem where a token is being misused for
     * an unintended purpose.</p>
     * 
     * @param message the detail message explaining the purpose mismatch
     * @throws IllegalArgumentException if message is null
     */
    public InvalidTokenPurposeException(String message) {
        super(message, 401);
    }
    
    /**
     * Constructs a new InvalidTokenPurposeException with custom status code.
     * 
     * <p>Allows for custom HTTP status codes when the default 401 is not
     * appropriate for the specific use case.</p>
     * 
     * @param message the detail message explaining the purpose mismatch
     * @param status the HTTP status code to return (e.g., 403 for forbidden operations)
     * @throws IllegalArgumentException if message is null or status is invalid
     */
    public InvalidTokenPurposeException(String message, int status) {
        super(message, status);
    }
    
    /**
     * Constructs a new InvalidTokenPurposeException with underlying cause.
     * 
     * <p>This constructor is useful when the purpose validation failure is
     * caused by an underlying issue (e.g., JWT parsing errors, claim extraction
     * problems) that should be preserved for debugging.</p>
     * 
     * @param message the detail message explaining the purpose mismatch
     * @param cause the underlying cause of this exception
     * @throws IllegalArgumentException if message is null
     */
    public InvalidTokenPurposeException(String message, Throwable cause) {
        super(message, 401, cause);
    }
    
    /**
     * Constructs a new InvalidTokenPurposeException with custom status and underlying cause.
     * 
     * @param message the detail message explaining the purpose mismatch
     * @param status the HTTP status code to return
     * @param cause the underlying cause of this exception
     * @throws IllegalArgumentException if message is null or status is invalid
     */
    public InvalidTokenPurposeException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
    
    /**
     * Factory method for creating standard purpose mismatch exceptions.
     * 
     * <p>Provides a consistent format for purpose mismatch error messages,
     * making it easier to create standardized error responses.</p>
     * 
     * @param expectedPurpose the purpose that was expected
     * @param actualPurpose the purpose that was found in the token
     * @return a new InvalidTokenPurposeException with standardized message
     * @throws IllegalArgumentException if expectedPurpose is null
     */
    public static InvalidTokenPurposeException purposeMismatch(String expectedPurpose, String actualPurpose) {
        String message = String.format(
            "Token purpose mismatch. Expected: '%s', Found: '%s'", 
            expectedPurpose, 
            actualPurpose != null ? actualPurpose : "null"
        );
        return new InvalidTokenPurposeException(message);
    }
    
    /**
     * Factory method for missing purpose claim exceptions.
     * 
     * @param expectedPurpose the purpose that was expected to be found
     * @return a new InvalidTokenPurposeException for missing purpose
     * @throws IllegalArgumentException if expectedPurpose is null
     */
    public static InvalidTokenPurposeException missingPurpose(String expectedPurpose) {
        String message = String.format(
            "Token is missing required purpose claim. Expected: '%s'", 
            expectedPurpose
        );
        return new InvalidTokenPurposeException(message);
    }
}