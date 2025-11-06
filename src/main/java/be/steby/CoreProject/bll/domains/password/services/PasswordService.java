package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.domains.password.exceptions.PasswordDomainException;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import be.steby.CoreProject.bll.domains.password.models.*;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for password management operations.
 *
 * <p>This service handles all password-related business logic including:
 * <ul>
 *   <li>Password reset requests (email and SMS delivery)</li>
 *   <li>Password reset completion with tokens</li>
 *   <li>Password changes for authenticated users</li>
 *   <li>Token regeneration for expired reset requests</li>
 * </ul>
 *
 * <p>Security considerations are implemented at the service level including
 * user enumeration prevention, rate limiting compliance, and secure token handling.
 */
public interface PasswordService {

    /**
     * Completes the password reset process using a valid token.
     *
     * <p>Validates the token, checks its expiration, and updates the user's password
     * if all validations pass. The token is consumed during this process.
     *
     * @param request contains the new password (already validated)
     * @param token the password reset token from email/SMS
     * @param httpRequest HTTP request for auditing purposes
     * @throws PasswordDomainException if token is invalid, expired, or password fails validation
     */
    void resetPassword(PasswordResetRequest request, String token, HttpServletRequest httpRequest);

    /**
     * Changes the password for an authenticated user.
     *
     * <p>Validates the current password, applies password policy validation to the new password,
     * and updates the user's password. All active sessions except the current one are invalidated.
     *
     * @param request contains current password and new password
     * @param httpRequest HTTP request for auditing purposes
     * @throws PasswordDomainException if current password is incorrect or new password fails validation
     */
    void changePassword(PasswordChangeRequest request, HttpServletRequest httpRequest);

    /**
     * Initiates a password reset request with support for email or SMS delivery.
     *
     * <p>This method handles both email and SMS delivery channels based on the notification type.
     * For security reasons, the response is always generic regardless of whether the email exists
     * or whether SMS requirements are met.
     *
     * <p>Email delivery: Creates a long-lived token and sends reset link via email.
     * <p>SMS delivery: Creates a short verification code and sends via SMS (requires verified phone).
     *
     * @param request contains email and notification type (EMAIL or SMS)
     * @param httpRequest HTTP request for auditing purposes
     * @return SMS password reset result (contains JWT token if SMS was successful, failure otherwise)
     * @throws PasswordDomainException if request data fails business validation
     */
    SmsPasswordResetResult requestPasswordReset(ForgotPasswordBLLRequest request, HttpServletRequest httpRequest);

    /**
     * Requests a new password reset token if the previous one expired.
     *
     * <p>This endpoint allows users to generate a new reset link without going through
     * the entire forgot password flow again. Uses generic responses for security.
     *
     * @param token the expired token
     * @param httpRequest HTTP request for auditing purposes
     * @throws PasswordDomainException if token validation fails
     */
    void requestPasswordToken(String token, HttpServletRequest httpRequest);

    /**
     * Verifies SMS password reset code and grants permission to reset password.
     *
     * <p>Validates the user-provided 6-digit code against the hashed code stored
     * in the JWT token. If validation succeeds, generates a permission token that
     * allows access to the password reset page.
     *
     * @param request contains verification code and JWT token from cookie
     * @return verification result with permission token if successful
     * @throws PasswordDomainException if validation fails or token is invalid
     */
    SmsVerificationResult verifySmsPasswordResetCode(VerifySmsPasswordResetBLLRequest request);


    /**
     * Resets password using a permission token (from SMS verification).
     *
     * <p>This method is used after successful SMS code verification. The permission token
     * grants temporary access to reset the password without needing the original email token.
     * All active user sessions are invalidated for security.
     *
     * @param request contains permission token and new password
     * @throws PasswordDomainException if token is invalid or password fails validation
     * @throws InvalidPasswordResetTokenException if permission token is invalid or expired
     */
    void resetPasswordWithPermission(ResetPasswordWithPermissionBLLRequest request);


}