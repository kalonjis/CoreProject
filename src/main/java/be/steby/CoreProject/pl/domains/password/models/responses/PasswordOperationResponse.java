package be.steby.CoreProject.pl.domains.password.models.responses;

/**
 * Response for password-related operations.
 */
public record PasswordOperationResponse(
        String message
) {
    public static PasswordOperationResponse passwordChanged() {
        return new PasswordOperationResponse("Your password has been changed successfully.");
    }

    public static PasswordOperationResponse passwordReset() {
        return new PasswordOperationResponse(
                "Your password has been reset successfully. You can now log in with your new password."
        );
    }

    public static PasswordOperationResponse resetEmailSent() {
        return new PasswordOperationResponse(
                "If your email address is in our database, you will receive a password reset link shortly."
        );
    }

    public static PasswordOperationResponse newTokenSent() {
        return new PasswordOperationResponse(
                "If your request was valid, a new password reset link has been sent to your email."
        );
    }
}