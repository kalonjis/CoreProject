package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import be.steby.CoreProject.bll.domains.password.exceptions.PasswordAlreadyDefinedException;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.domains.password.exceptions.PasswordDomainException;
import be.steby.CoreProject.bll.domains.password.models.*;
import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Service interface for password management operations.
 *
 * <p>This service handles all password-related business logic including:
 * <ul>
 *   <li>Password reset requests via multiple delivery methods (EMAIL_LINK, EMAIL_CODE, SMS_CODE)</li>
 *   <li>Verification code validation for code-based reset flows</li>
 *   <li>Password reset completion with tokens or permission cookies</li>
 *   <li>Password changes for authenticated users</li>
 *   <li>Token regeneration for expired reset requests</li>
 * </ul>
 *
 * <p><strong>Security considerations:</strong>
 * <ul>
 *   <li>User enumeration prevention (generic responses)</li>
 *   <li>Rate limiting compliance</li>
 *   <li>Secure token handling</li>
 *   <li>Session invalidation on password change/reset</li>
 * </ul>
 *
 * @see PasswordResetType
 * @see CodePasswordResetResult
 * @see CodeVerificationResult
 */
public interface PasswordService {

    // =========================================================================
    // FORGOT PASSWORD - INITIATE RESET
    // =========================================================================

    /**
     * Initiates a password reset request with support for multiple delivery methods.
     *
     * <p>This method handles all password reset types defined in {@link PasswordResetType}:
     * <ul>
     *   <li>{@code EMAIL_LINK}: Creates a long-lived token and sends reset link via email</li>
     *   <li>{@code EMAIL_CODE}: Creates a short verification code and sends via email</li>
     *   <li>{@code SMS_CODE}: Creates a short verification code and sends via SMS (requires verified phone)</li>
     * </ul>
     *
     * <p>For security reasons, the response is always generic regardless of whether the email exists
     * or whether delivery requirements are met. This prevents user enumeration attacks.
     *
     * <p><strong>Code-based flows:</strong> For {@code EMAIL_CODE} and {@code SMS_CODE}, the returned
     * result contains a JWT token that should be stored in a verification cookie by the controller.
     *
     * @param request contains email and reset type
     * @return result containing JWT token for code-based flows, or failure indicator
     * @throws PasswordDomainException if request data fails business validation
     */
    CodePasswordResetResult requestPasswordReset(ForgotPasswordBLLRequest request);

    // =========================================================================
    // CODE VERIFICATION (EMAIL_CODE and SMS_CODE flows)
    // =========================================================================

    /**
     * Verifies a password reset code and grants permission to reset password.
     *
     * <p>This method handles verification for both {@code EMAIL_CODE} and {@code SMS_CODE}
     * reset flows. It validates the user-provided 6-digit code against the hashed code
     * stored in the database, referenced by the JWT token.
     *
     * <p>If validation succeeds, generates a permission token that allows access to
     * the password reset page. The permission token has a limited lifespan (typically 15 minutes).
     *
     * @param request contains verification code and JWT token from cookie
     * @return verification result with permission token if successful, failure otherwise
     * @throws PasswordDomainException if validation fails or token is invalid
     */
    CodeVerificationResult verifyPasswordResetCode(VerifyCodeBLLRequest request);

    // =========================================================================
    // RESET PASSWORD - COMPLETE RESET
    // =========================================================================

    /**
     * Completes the password reset process using a valid token from email link.
     *
     * <p>This method is used for {@code EMAIL_LINK} reset flow. It validates the token,
     * checks its expiration, and updates the user's password if all validations pass.
     * The token is consumed during this process.
     *
     * <p>All active user sessions are invalidated for security.
     *
     * @param request contains the new password (already validated at PL level)
     * @param token the password reset token from email link
     * @throws PasswordDomainException if token is invalid, expired, or password fails validation
     */
    void resetPassword(PasswordResetRequest request, String token);

    /**
     * Completes password reset using a permission token from code verification.
     *
     * <p>This method is used for {@code EMAIL_CODE} and {@code SMS_CODE} reset flows,
     * after successful code verification via {@link #verifyPasswordResetCode}.
     *
     * <p>The permission token grants temporary access to reset the password without
     * needing the original email token. All active user sessions are invalidated for security.
     *
     * @param request contains permission token and new password
     * @throws PasswordDomainException if password fails validation
     * @throws InvalidPasswordResetTokenException if permission token is invalid or expired
     */
    void resetPasswordWithPermission(ResetPasswordWithPermissionBLLRequest request);

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    /**
     * Requests a new password reset token if the previous one expired.
     *
     * <p>This allows users to generate a new reset link without going through
     * the entire forgot password flow again.
     *
     * <p><strong>Security:</strong> Uses generic responses to prevent token validation attacks.
     *
     * @param token the expired token
     * @throws PasswordDomainException if token validation fails
     */
    void requestPasswordToken(String token);

    // =========================================================================
    // CHANGE PASSWORD (AUTHENTICATED)
    // =========================================================================

    /**
     * Changes the password for an authenticated user.
     *
     * <p>Validates the current password, applies password policy validation to the
     * new password, and updates the user's password.
     *
     * <p>All active sessions except the current one are invalidated for security.
     *
     * @param request contains current password and new password
     * @throws PasswordDomainException if current password is incorrect or new password fails validation
     */
    void changePassword(PasswordChangeRequest request);

    // =========================================================================
    // DEFINE PASSWORD (OAUTH USERS)
    // =========================================================================

    /**
     * Defines a password for OAuth-only users who don't have one yet.
     *
     * <p>This allows users who signed up via OAuth (Google, GitHub, Microsoft)
     * to also login with credentials.
     *
     * <p>Only allowed if user has no password defined (user.hasPassword() == false).
     *
     * @param newPassword the password to define (already validated at PL level)
     * @throws PasswordAlreadyDefinedException if user already has a password
     * @throws InvalidPasswordException if password fails policy validation
     */
    void definePassword(String newPassword);
}