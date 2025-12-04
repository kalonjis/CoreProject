package be.steby.CoreProject.bll.domains.password.events.email;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Event triggered when a password reset via email code is requested.
 *
 * <p>This event handles the {@link PasswordResetType#EMAIL_CODE} flow where
 * a 6-digit verification code is sent via email instead of a clickable link.
 *
 * <p><strong>Flow:</strong>
 * <ol>
 *   <li>User requests password reset with EMAIL_CODE type</li>
 *   <li>Service generates 6-digit code and stores hashed version in DB</li>
 *   <li>This event is published with the plain code</li>
 *   <li>Listener sends email containing the verification code</li>
 *   <li>User enters code on verification page</li>
 * </ol>
 *
 * <p><strong>Security:</strong>
 * <ul>
 *   <li>The plain code is only used for email delivery</li>
 *   <li>The hashed version is stored in the database for validation</li>
 *   <li>Code expires after a short period (typically 10 minutes)</li>
 * </ul>
 *
 * <p><strong>Published by:</strong> PasswordServiceImpl.handleEmailCodePasswordReset()
 * <p><strong>Consumed by:</strong> PasswordEmailNotificationListener
 *
 * @see PasswordResetType#EMAIL_CODE
 * @see PasswordResetEmailRequestedEvent
 */
public record PasswordResetCodeEmailRequestedEvent(

        /**
         * The user who requested the password reset.
         */
        User user,

        /**
         * The generated 6-digit verification code to send via email.
         *
         * <p><strong>Security Note:</strong> This is the plain verification code
         * needed for email delivery. The hashed version is stored in the database
         * token for validation purposes.
         *
         * <p><strong>Format:</strong> Exactly 6 numeric digits (e.g., "123456").
         */
        String verificationCode

) {
    /**
     * Validates the event data on construction.
     *
     * @throws IllegalArgumentException if user or verification code is invalid
     */
    public PasswordResetCodeEmailRequestedEvent {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (verificationCode == null || verificationCode.isBlank()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty");
        }
        if (!verificationCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("Verification code must be exactly 6 digits");
        }
    }
}