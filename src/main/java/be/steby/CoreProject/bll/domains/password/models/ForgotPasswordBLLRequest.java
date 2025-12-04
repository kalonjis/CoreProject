package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Business layer DTO for password reset request operations.
 *
 * <p>This model represents a validated password reset request at the business logic level.
 * It contains clean, normalized data that has passed presentation layer validation
 * and is ready for business rule processing.
 *
 * <p>This separation allows:
 * <ul>
 *   <li>Business logic to work with clean, validated data</li>
 *   <li>Presentation layer to handle HTTP-specific concerns</li>
 *   <li>Easy testing of business logic without HTTP concerns</li>
 *   <li>Future API versioning without affecting business logic</li>
 * </ul>
 *
 * <p><strong>Note:</strong> This is a pure data transfer object. Business validation
 * is performed in the service layer, not in this record.
 *
 * @see PasswordResetType
 * @see be.steby.CoreProject.bll.domains.password.services.PasswordService#requestPasswordReset
 */
public record ForgotPasswordBLLRequest(

        /**
         * The user's email address (normalized to lowercase).
         *
         * <p>This serves as the primary identifier to locate the user account.
         * The email has been validated by the presentation layer and normalized
         * for consistent processing.
         */
        String email,

        /**
         * The password reset delivery method.
         *
         * <p>Determines how the reset credentials will be delivered to the user:
         * <ul>
         *   <li>{@link PasswordResetType#EMAIL_LINK}: Email with clickable reset link (token in URL)</li>
         *   <li>{@link PasswordResetType#EMAIL_CODE}: Email with 6-digit verification code</li>
         *   <li>{@link PasswordResetType#SMS_CODE}: SMS with 6-digit verification code</li>
         * </ul>
         *
         * <p>Code-based methods ({@code EMAIL_CODE}, {@code SMS_CODE}) require an additional
         * verification step and use HTTP-only cookies for permission management.
         *
         * <p>SMS delivery requires the user to have a verified phone number.
         * If requirements are not met, the system may silently take no action
         * for security reasons (preventing user enumeration).
         */
        PasswordResetType resetType

) {
    /**
     * Checks if this request uses a code-based verification flow.
     *
     * <p>Convenience method that delegates to {@link PasswordResetType#requiresCodeVerification()}.
     *
     * @return {@code true} if the reset type requires code verification,
     *         {@code false} for link-based reset
     */
    public boolean requiresCodeVerification() {
        return resetType != null && resetType.requiresCodeVerification();
    }
}