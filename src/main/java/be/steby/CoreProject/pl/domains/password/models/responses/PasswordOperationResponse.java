package be.steby.CoreProject.pl.domains.password.models.responses;

/**
 * Response for password-related operations.
 *
 * <p>This response model uses factory methods to ensure consistent messaging
 * across all password operations while maintaining security best practices.
 */
public record PasswordOperationResponse(
        String message
) {

    /**
     * Response for successful password change operation.
     *
     * @return response indicating password was changed successfully
     */
    public static PasswordOperationResponse passwordChanged() {
        return new PasswordOperationResponse("Your password has been changed successfully.");
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

    /**
     * Response for password reset email request.
     *
     * <p>Uses generic messaging to prevent user enumeration attacks.
     * Returns the same message regardless of whether the email exists.
     *
     * @return generic response for email reset requests
     */
    public static PasswordOperationResponse resetEmailSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database, you will receive a password reset link shortly."
        );
    }

    /**
     * Response for password reset SMS request.
     *
     * <p>Uses generic messaging to prevent user enumeration attacks.
     * Returns the same message regardless of whether the email exists or
     * whether the user has a verified phone number.
     *
     * @return generic response for SMS reset requests
     */
    public static PasswordOperationResponse resetSmsSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database and you have a verified phone number, " +
                        "you will receive a password reset code via SMS shortly."
        );
    }

    /**
     * Response for new password reset token request.
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

    public static PasswordOperationResponse smsCodeVerified() {
        return new PasswordOperationResponse("SMS code verified successfully. You can now reset your password.");
    }

    public static PasswordOperationResponse smsCodeVerificationFailed() {
        return new PasswordOperationResponse("Invalid or expired SMS verification code.");
    }

    public static PasswordOperationResponse invalidPermissionToken() {
        return new PasswordOperationResponse("Invalid or expired permission token. Please restart the password reset process.");
    }
}