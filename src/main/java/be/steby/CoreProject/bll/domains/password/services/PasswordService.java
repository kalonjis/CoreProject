package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.domains.password.models.*;

import be.steby.CoreProject.bll.domains.password.exceptions.*;
import be.steby.CoreProject.bll.common.exceptions.TokenValidityException;

/**
 * Core password operations: verification, reset completion, change, and definition.
 *
 * <p>Reset initiation is intentionally excluded from this service and delegated
 * to channel-specific services:
 * <ul>
 *   <li>{@link EmailLinkPasswordResetService} — reset via email link</li>
 *   <li>{@link EmailCodePasswordResetService} — reset via email verification code</li>
 *   <li>{@link SmsPasswordResetService} — reset via SMS verification code</li>
 * </ul>
 */
public interface PasswordService {

    // =========================================================================
    // CODE VERIFICATION (EMAIL_CODE + SMS_CODE)
    // =========================================================================

    /**
     * Verifies a 6-digit code and grants permission to reset the password.
     *
     * <p>Used for both {@code EMAIL_CODE} and {@code SMS_CODE} flows. Validates the
     * user-provided code against the hashed value stored in the database, referenced
     * by the JWT token from the verification cookie.
     *
     * <p>On success, returns a short-lived permission token (typically 15 minutes)
     * that allows the user to access the password reset form.
     *
     * @param request contains the 6-digit code and the JWT reference token from cookie
     * @return success with permission token, or failure if code is invalid or expired
     */
    CodeVerificationResult verifyPasswordResetCode(VerifyCodeBLLRequest request);

    // =========================================================================
    // RESET PASSWORD - COMPLETE RESET
    // =========================================================================

    /**
     * Completes the password reset using a token from the email link (EMAIL_LINK flow).
     *
     * <p>Validates and consumes the token, updates the password, then revokes all
     * active sessions and disconnects all devices for security.
     *
     * @param request contains the new password
     * @param token   the reset token from the email link URL parameter
     * @throws InvalidPasswordResetTokenException if the token is invalid or expired
     * @throws InvalidPasswordException           if the new password fails policy validation
     */
    void resetPassword(PasswordResetRequest request, String token);

    /**
     * Completes the password reset using a permission token (EMAIL_CODE + SMS_CODE flows).
     *
     * <p>Called after successful code verification. The permission token — issued by
     * {@link #verifyPasswordResetCode} — grants temporary access to reset the password
     * without requiring the original email token.
     *
     * <p>All active sessions are revoked and all devices are disconnected on success.
     *
     * @param request contains the permission token and the new password
     * @throws InvalidPasswordResetTokenException if the permission token is invalid or expired
     * @throws InvalidPasswordException           if the new password fails policy validation
     */
    void resetPasswordWithPermission(ResetPasswordWithPermissionBLLRequest request);

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    /**
     * Issues a new EMAIL_LINK reset token when the previous one has expired.
     *
     * <p>The expired token is revoked and a fresh one is created and sent by email.
     * If the token is still valid, a {@code TokenValidityException} is thrown with
     * a link to the existing reset page.
     *
     * @param token the expired (or potentially still valid) reset token
     * @throws TokenValidityException if the token is still valid
     */
    void requestPasswordToken(String token);

    // =========================================================================
    // AUTHENTICATED OPERATIONS
    // =========================================================================

    /**
     * Changes the password for the currently authenticated user.
     *
     * <p>Requires the current password for verification. On success, all sessions
     * except the current device are revoked. If the current device cannot be
     * identified, all sessions are revoked.
     *
     * @param request contains the current password and the new password
     * @throws InvalidPasswordException    if the current password is incorrect or the new one fails policy
     * @throws NoPasswordDefinedException  if the user has no password yet (OAuth user — use definePassword instead)
     */
    void changePassword(PasswordChangeRequest request);

    /**
     * Defines a first password for OAuth users who signed up without credentials.
     *
     * <p>Only allowed when the user has no password defined ({@code user.hasPassword() == false}).
     * Does not require a current password. Use {@link #changePassword} if the user already has one.
     *
     * @param newPassword the password to set
     * @throws PasswordAlreadyDefinedException if the user already has a password
     * @throws InvalidPasswordException        if the password fails policy validation
     */
    void definePassword(String newPassword);
}