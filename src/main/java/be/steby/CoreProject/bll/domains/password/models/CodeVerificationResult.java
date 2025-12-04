package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Result of password reset code verification (EMAIL_CODE or SMS_CODE).
 *
 * <p>Contains the verification status and optionally a permission token
 * that grants access to the password reset page.
 *
 * <p><strong>Flow:</strong>
 * <ol>
 *   <li>User receives 6-digit code via email or SMS</li>
 *   <li>User enters code on verification page</li>
 *   <li>Code is validated against hashed value in database</li>
 *   <li>If valid, permission token is generated and returned</li>
 *   <li>Permission token is stored in HTTP-only cookie</li>
 *   <li>User can access password reset page with this cookie</li>
 * </ol>
 *
 * <p>The permission token has a limited lifespan (typically 15 minutes)
 * to minimize the window of exposure.
 *
 * @param success whether the code verification was successful
 * @param resetPermissionToken JWT token that grants permission to reset password (null if failed)
 *
 * @see PasswordResetType
 * @see CodePasswordResetResult
 */
public record CodeVerificationResult(
        boolean success,
        String resetPermissionToken
) {

    /**
     * Creates a successful verification result with permission token.
     *
     * <p>Used when the user-provided code matches the expected code
     * and they are granted permission to reset their password.
     *
     * @param resetPermissionToken the permission token for password reset page access
     * @return successful code verification result
     */
    public static CodeVerificationResult success(String resetPermissionToken) {
        return new CodeVerificationResult(true, resetPermissionToken);
    }

    /**
     * Creates a failed verification result.
     *
     * <p>Used in the following scenarios:
     * <ul>
     *   <li>Code doesn't match expected value</li>
     *   <li>Code has expired</li>
     *   <li>Too many failed attempts</li>
     *   <li>Invalid or expired JWT token</li>
     *   <li>Token reference not found in database</li>
     * </ul>
     *
     * @return failed code verification result
     */
    public static CodeVerificationResult failure() {
        return new CodeVerificationResult(false, null);
    }

    /**
     * Checks if this result has a permission token.
     *
     * @return true if verification succeeded and permission token is available
     */
    public boolean hasPermissionToken() {
        return resetPermissionToken != null && !resetPermissionToken.isBlank();
    }
}