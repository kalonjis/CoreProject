package be.steby.CoreProject.pl.domains.password.models.responses;

/**
 * Response for password-related operations.
 *
 * <p>All messages are intentionally generic to prevent user enumeration attacks.
 * The same response is returned regardless of whether the user exists or whether
 * delivery requirements are met.
 */
public record PasswordOperationResponse(
        String message,
        String hint
) {

    public PasswordOperationResponse(String message) {
        this(message, null);
    }

    // =========================================================================
    // FORGOT PASSWORD
    // =========================================================================

    public static PasswordOperationResponse resetEmailLinkSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database, you will receive a password reset link shortly."
        );
    }

    public static PasswordOperationResponse resetEmailCodeSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database, you will receive a verification code shortly."
        );
    }

    /**
     * @param phoneHint masked phone number for display (e.g. "+32 *** *** 47"), may be null
     */
    public static PasswordOperationResponse resetSmsCodeSent(String phoneHint) {
        return new PasswordOperationResponse(
                "If your account exists and has a verified phone number, " +
                        "you will receive a verification code via SMS shortly.",
                phoneHint
        );
    }

    // =========================================================================
    // CODE VERIFICATION
    // =========================================================================

    public static PasswordOperationResponse codeVerified() {
        return new PasswordOperationResponse(
                "Code verified successfully. You can now reset your password."
        );
    }

    public static PasswordOperationResponse codeVerificationFailed() {
        return new PasswordOperationResponse(
                "Invalid or expired verification code."
        );
    }

    public static PasswordOperationResponse invalidPermissionToken() {
        return new PasswordOperationResponse(
                "Invalid or expired permission token. Please restart the password reset process."
        );
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    public static PasswordOperationResponse newTokenSent() {
        return new PasswordOperationResponse(
                "If your request was valid, a new password reset link has been sent to your email."
        );
    }

    // =========================================================================
    // PASSWORD CHANGE / RESET / DEFINE
    // =========================================================================

    public static PasswordOperationResponse passwordReset() {
        return new PasswordOperationResponse(
                "Your password has been reset successfully. You can now log in with your new password."
        );
    }

    public static PasswordOperationResponse passwordChanged() {
        return new PasswordOperationResponse(
                "Your password has been changed successfully."
        );
    }

    public static PasswordOperationResponse passwordDefined() {
        return new PasswordOperationResponse(
                "Your password has been set successfully. You can now login with your email and password."
        );
    }
}