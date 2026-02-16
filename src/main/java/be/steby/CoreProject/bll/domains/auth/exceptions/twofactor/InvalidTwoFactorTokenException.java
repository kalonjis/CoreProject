package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

/**
 * Exception thrown when a two-factor authentication token validation fails.
 *
 * <p>This exception is thrown by {@link be.steby.CoreProject.bll.domains.twofactor.services.jwt.TwoFactorJwtService}
 * when a 2FA token cannot be validated due to:
 * <ul>
 *   <li>Invalid signature (token was tampered with)</li>
 *   <li>Expired token</li>
 *   <li>Wrong token purpose (e.g., using session token for verification)</li>
 *   <li>Missing required claims (publicId, verificationCodeHash, etc.)</li>
 * </ul>
 *
 * <p><b>HTTP Status:</b> 401 Unauthorized (inherited from {@link TwoFactorDomainException}).
 *
 * @author Steby Team
 * @since 2.1.0
 * @see TwoFactorDomainException
 */
public class InvalidTwoFactorTokenException extends TwoFactorDomainException {

    /**
     * Constructs exception with a descriptive message.
     * HTTP status defaults to 401 Unauthorized.
     *
     * @param message description of why the token is invalid
     */
    public InvalidTwoFactorTokenException(String message) {
        super(message);
    }

    /**
     * Constructs exception with message and root cause.
     * HTTP status defaults to 401 Unauthorized.
     *
     * @param message description of why the token is invalid
     * @param cause   the underlying exception (e.g., JwtException)
     */
    public InvalidTwoFactorTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}