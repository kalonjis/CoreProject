package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when an access token validation fails.
 *
 * <p>This exception is thrown by {@link be.steby.CoreProject.bll.domains.auth.services.jwt.AuthJwtService}
 * when an access token cannot be validated due to:
 * <ul>
 *   <li>Invalid signature (token was tampered with)</li>
 *   <li>Expired token</li>
 *   <li>Malformed token structure</li>
 *   <li>Missing required claims</li>
 * </ul>
 *
 * <p><b>HTTP Status:</b> 401 Unauthorized (inherited from {@link AuthDomainException}).
 *
 * @author Steby Team
 * @since 2.1.0
 * @see AuthDomainException
 */
public class InvalidAccessTokenException extends AuthDomainException {

    /**
     * Constructs exception with a descriptive message.
     * HTTP status defaults to 401 Unauthorized.
     *
     * @param message description of why the token is invalid
     */
    public InvalidAccessTokenException(String message) {
        super(message);
    }

    /**
     * Constructs exception with message and root cause.
     * HTTP status defaults to 401 Unauthorized.
     *
     * @param message description of why the token is invalid
     * @param cause   the underlying exception (e.g., JwtException)
     */
    public InvalidAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}