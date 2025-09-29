package be.steby.CoreProject.pl.domains.password.models.responses;

import java.time.Instant;
import java.util.List;

/**
 * Unified response model for all password-related operations.
 * Provides a consistent API response structure with security considerations.
 *
 * <p>Security principles applied:
 * <ul>
 *   <li>Generic messages to prevent user enumeration</li>
 *   <li>No disclosure of whether emails/usernames exist</li>
 *   <li>Same response structure for success and safe errors</li>
 * </ul>
 */
public record PasswordOperationResponse(
        boolean success,
        String message,
        List<String> errors,
        Instant timestamp
) {
    // ==================== SUCCESS RESPONSES ====================

    /**
     * Generic success response with custom message.
     */
    public static PasswordOperationResponse success(String message) {
        return new PasswordOperationResponse(true, message, null, Instant.now());
    }

    /**
     * Response for successful password change (authenticated user).
     */
    public static PasswordOperationResponse passwordChanged() {
        return success("Your password has been changed successfully.");
    }

    /**
     * Response for successful password reset via token.
     */
    public static PasswordOperationResponse passwordReset() {
        return success("Your password has been reset successfully. You can now log in with your new password.");
    }

    /**
     * Generic response for password reset request.
     * ✅ SECURITY: Same message whether email exists or not (prevents user enumeration).
     */
    public static PasswordOperationResponse resetEmailSent() {
        return success(
                "If your email address is in our database, you will receive a password reset link shortly."
        );
    }

    /**
     * Response for requesting a new reset token.
     * ✅ SECURITY: Generic message to prevent token validation attacks.
     */
    public static PasswordOperationResponse newTokenSent() {
        return success(
                "If your request was valid, a new password reset link has been sent to your email."
        );
    }

    // ==================== ERROR RESPONSES ====================

    /**
     * Generic validation error response.
     * ✅ SAFE: Client-side validation errors are safe to expose.
     */
    public static PasswordOperationResponse validationError(String message) {
        return new PasswordOperationResponse(false, message, null, Instant.now());
    }

    /**
     * Validation error with detailed field errors.
     * ✅ SAFE: Validation rules don't expose sensitive information.
     */
    public static PasswordOperationResponse validationErrors(String message, List<String> errors) {
        return new PasswordOperationResponse(false, message, errors, Instant.now());
    }

    /**
     * Generic authentication failure response.
     * ✅ SECURITY: Doesn't specify which credential is wrong.
     */
    public static PasswordOperationResponse authenticationFailed() {
        return new PasswordOperationResponse(
                false,
                "The provided credentials are incorrect.",
                null,
                Instant.now()
        );
    }

    /**
     * Invalid or expired token error.
     * ✅ SAFE: Generic token error message.
     */
    public static PasswordOperationResponse invalidToken() {
        return new PasswordOperationResponse(
                false,
                "The password reset link is invalid or has expired. Please request a new one.",
                null,
                Instant.now()
        );
    }

    /**
     * Generic error response for unexpected situations.
     */
    public static PasswordOperationResponse error(String message) {
        return new PasswordOperationResponse(false, message, null, Instant.now());
    }
}