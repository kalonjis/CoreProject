package be.steby.CoreProject.pl.domains.password.models.responses;

import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Response for password-related operations.
 *
 * <p>This response model uses factory methods to ensure consistent messaging
 * across all password operations while maintaining security best practices.
 *
 * <p>All messages are intentionally generic to prevent user enumeration attacks.
 * The same response is returned regardless of whether the user exists or whether
 * delivery requirements are met.
 *
 * @see PasswordResetType
 */
public record PasswordOperationResponse(
        String message
) {

    // =========================================================================
    // PASSWORD CHANGE / RESET COMPLETION
    // =========================================================================

    /**
     * Response for successful password change operation (authenticated user).
     *
     * @return response indicating password was changed successfully
     */
    public static PasswordOperationResponse passwordChanged() {
        return new PasswordOperationResponse(
                "Your password has been changed successfully."
        );
    }

    /**
     * Response for successful password reset completion.
     *
     * @return response indicating password was reset successfully
     */
    public static PasswordOperationResponse passwordReset() {
        return new PasswordOperationResponse(
                "Your password has been reset successfully. You can now log in with your new password."
        );
    }

    // =========================================================================
    // FORGOT PASSWORD - RESET TYPE SPECIFIC RESPONSES
    // =========================================================================

    /**
     * Returns the appropriate response for a given password reset type.
     *
     * <p>This is a convenience method to simplify controller logic by mapping
     * the reset type to the correct generic response message.
     *
     * @param resetType the password reset delivery method
     * @return generic response appropriate for the reset type
     */
    public static PasswordOperationResponse forResetType(PasswordResetType resetType) {
        return switch (resetType) {
            case EMAIL_LINK -> resetEmailLinkSent();
            case EMAIL_CODE -> resetEmailCodeSent();
            case SMS_CODE -> resetSmsCodeSent();
        };
    }

    /**
     * Response for password reset via email link ({@link PasswordResetType#EMAIL_LINK}).
     *
     * <p>Uses generic messaging to prevent user enumeration attacks.
     * Returns the same message regardless of whether the email exists.
     *
     * @return generic response for email link reset requests
     */
    public static PasswordOperationResponse resetEmailLinkSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database, you will receive a password reset link shortly."
        );
    }

    /**
     * Response for password reset via email code ({@link PasswordResetType#EMAIL_CODE}).
     *
     * <p>Uses generic messaging to prevent user enumeration attacks.
     * Returns the same message regardless of whether the email exists.
     *
     * @return generic response for email code reset requests
     */
    public static PasswordOperationResponse resetEmailCodeSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database, you will receive a verification code shortly."
        );
    }

    /**
     * Response for password reset via SMS code ({@link PasswordResetType#SMS_CODE}).
     *
     * <p>Uses generic messaging to prevent user enumeration attacks.
     * Returns the same message regardless of whether the email exists or
     * whether the user has a verified phone number.
     *
     * @return generic response for SMS code reset requests
     */
    public static PasswordOperationResponse resetSmsCodeSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database and you have a verified phone number, " +
                        "you will receive a verification code via SMS shortly."
        );
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    /**
     * Response for new password reset token request (resend expired token).
     *
     * <p>Uses generic messaging to prevent token validation attacks.
     *
     * @return generic response for token resend requests
     */
    public static PasswordOperationResponse newTokenSent() {
        return new PasswordOperationResponse(
                "If your request was valid, a new password reset link has been sent to your email."
        );
    }

    // =========================================================================
    // CODE VERIFICATION
    // =========================================================================

    /**
     * Response for successful verification code validation.
     *
     * <p>Used for both {@link PasswordResetType#EMAIL_CODE} and {@link PasswordResetType#SMS_CODE}
     * flows after the user enters the correct 6-digit code.
     *
     * @return response indicating code was verified successfully
     */
    public static PasswordOperationResponse codeVerified() {
        return new PasswordOperationResponse(
                "Code verified successfully. You can now reset your password."
        );
    }

    /**
     * Response for failed verification code validation.
     *
     * <p>Used when the provided code is invalid, expired, or when
     * too many attempts have been made.
     *
     * @return response indicating code verification failed
     */
    public static PasswordOperationResponse codeVerificationFailed() {
        return new PasswordOperationResponse(
                "Invalid or expired verification code."
        );
    }

    /**
     * Response for invalid or expired permission token.
     *
     * <p>Used when attempting to reset password with an invalid
     * permission cookie after code verification.
     *
     * @return response indicating permission token is invalid
     */
    public static PasswordOperationResponse invalidPermissionToken() {
        return new PasswordOperationResponse(
                "Invalid or expired permission token. Please restart the password reset process."
        );
    }

    // =========================================================================
    // DEPRECATED - Keep for backward compatibility during transition
    // =========================================================================

    /**
     * @deprecated Use {@link #resetEmailLinkSent()} instead.
     */
    @Deprecated(forRemoval = true)
    public static PasswordOperationResponse resetEmailSent() {
        return resetEmailLinkSent();
    }

    /**
     * @deprecated Use {@link #resetSmsCodeSent()} instead.
     */
    @Deprecated(forRemoval = true)
    public static PasswordOperationResponse resetSmsSent() {
        return resetSmsCodeSent();
    }

    /**
     * @deprecated Use {@link #codeVerified()} instead.
     */
    @Deprecated(forRemoval = true)
    public static PasswordOperationResponse smsCodeVerified() {
        return codeVerified();
    }

    /**
     * @deprecated Use {@link #codeVerificationFailed()} instead.
     */
    @Deprecated(forRemoval = true)
    public static PasswordOperationResponse smsCodeVerificationFailed() {
        return codeVerificationFailed();
    }
}