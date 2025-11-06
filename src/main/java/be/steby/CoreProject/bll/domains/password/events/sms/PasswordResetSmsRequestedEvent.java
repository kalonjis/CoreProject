package be.steby.CoreProject.bll.domains.password.events.sms;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event triggered when a password reset via SMS is requested.
 *
 * <p>This event specifically handles SMS-based password reset notifications.
 * It carries the information needed to send a 6-digit verification code
 * via SMS to the user's verified phone number.</p>
 *
 * <h4>Migration Notes:</h4>
 * <p>Updated to support the new DB token approach:</p>
 * <ul>
 *   <li>Still carries the plain verification code for SMS sending</li>
 *   <li>The hashed version is stored in the database token</li>
 *   <li>Event is published after successful token creation</li>
 * </ul>
 *
 * <h4>Security:</h4>
 * <p>The verification code in this event is used only for SMS delivery
 * and should be handled securely by the SMS service. The actual validation
 * is performed against the hashed version stored in the database.</p>
 *
 * <p><strong>Published by:</strong> PasswordServiceImpl.handleSmsPasswordReset()</p>
 * <p><strong>Consumed by:</strong> PasswordSmsNotificationListener</p>
 *
 * @see be.steby.CoreProject.bll.domains.password.services.PasswordServiceImpl#handleSmsPasswordReset(User)
 * @see be.steby.CoreProject.bll.domains.password.listeners.PasswordSmsNotificationListener#handlePasswordResetSmsRequested(PasswordResetSmsRequestedEvent)
 */
public record PasswordResetSmsRequestedEvent(
        /**
         * The user who requested the password reset.
         */
        User user,

        /**
         * The generated 6-digit verification code to send via SMS.
         *
         * <p><strong>Security Note:</strong> This is the plain verification code
         * needed for SMS delivery. The hashed version is stored in the database
         * token for validation purposes.</p>
         *
         * <p><strong>Usage:</strong> This code should be sent via SMS and then
         * discarded. All validation is performed against the hashed version
         * in the database.</p>
         */
        String verificationCode
) {

    /**
     * Creates a new password reset SMS requested event.
     *
     * @param user the user requesting password reset (must not be null)
     * @param verificationCode the plain 6-digit code for SMS (must not be null or empty)
     * @throws IllegalArgumentException if user or verification code is null/empty
     */
//    public PasswordResetSmsRequestedEvent {
//        if (user == null) {
//            throw new IllegalArgumentException("User cannot be null");
//        }
//        if (verificationCode == null || verificationCode.trim().isEmpty()) {
//            throw new IllegalArgumentException("Verification code cannot be null or empty");
//        }
//        if (!verificationCode.matches("\\d{6}")) {
//            throw new IllegalArgumentException("Verification code must be exactly 6 digits");
//        }
//    }
}