package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Result of initiating a code-based password reset (EMAIL_CODE or SMS_CODE).
 *
 * <p>Contains the JWT token and success status after initiating a password reset
 * that requires code verification. This allows the presentation layer to set
 * the appropriate cookie with the verification token.
 *
 * <p><strong>Usage:</strong>
 * <ul>
 *   <li>{@link PasswordResetType#EMAIL_CODE}: JWT token for email code verification flow</li>
 *   <li>{@link PasswordResetType#SMS_CODE}: JWT token for SMS code verification flow</li>
 *   <li>{@link PasswordResetType#EMAIL_LINK}: Returns failure (no JWT needed for link-based flow)</li>
 * </ul>
 *
 * <p>The JWT token contains a reference to the database token where the hashed
 * verification code is stored. This lightweight approach keeps the cookie small
 * while maintaining security.
 *
 * @param success whether the password reset was initiated successfully
 * @param jwtToken JWT token containing token reference for verification (null if failed or link-based)
 *
 * @see PasswordResetType
 * @see CodeVerificationResult
 */
public record CodePasswordResetResult(
        boolean success,
        String jwtToken
) {

    /**
     * Creates a successful result with JWT token.
     *
     * <p>Used when a code-based password reset is successfully initiated
     * and the verification code has been sent (via email or SMS).
     *
     * @param jwtToken the JWT token to be stored in verification cookie
     * @return successful code password reset result
     */
    public static CodePasswordResetResult success(String jwtToken) {
        return new CodePasswordResetResult(true, jwtToken);
    }

    /**
     * Creates a failed result (no JWT token).
     *
     * <p>Used in the following scenarios:
     * <ul>
     *   <li>User email not found (silent failure for security)</li>
     *   <li>SMS requirements not met (no verified phone)</li>
     *   <li>Rate limit exceeded</li>
     *   <li>Link-based reset type (no JWT needed)</li>
     *   <li>Any other failure during initiation</li>
     * </ul>
     *
     * @return failed code password reset result
     */
    public static CodePasswordResetResult failure() {
        return new CodePasswordResetResult(false, null);
    }

    /**
     * Creates a result indicating link-based reset was used.
     *
     * <p>For {@link PasswordResetType#EMAIL_LINK}, the reset link is sent via email
     * and no JWT token is needed. This is technically a success but returns
     * no token since the flow doesn't require code verification.
     *
     * @return result for link-based password reset
     */
    public static CodePasswordResetResult linkBased() {
        return new CodePasswordResetResult(true, null);
    }

    /**
     * Checks if this result has a JWT token for cookie storage.
     *
     * @return true if a JWT token is available, false otherwise
     */
    public boolean hasToken() {
        return jwtToken != null && !jwtToken.isBlank();
    }
}