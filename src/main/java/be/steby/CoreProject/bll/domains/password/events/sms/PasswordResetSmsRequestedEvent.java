package be.steby.CoreProject.bll.domains.password.events.sms;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event triggered when a password reset via SMS is requested.
 * 
 * <p>This event specifically handles SMS-based password reset notifications.
 * It carries the information needed to generate and send a 6-digit verification
 * code via SMS to the user's verified phone number.
 * 
 * <p>Published by: PasswordServiceImpl.handleSmsPasswordReset()
 * <p>Consumed by: PasswordSmsNotificationListener
 */
public record PasswordResetSmsRequestedEvent(
        /**
         * The user who requested the password reset.
         */
        User user,

        /**
         * The generated 6-digit verification code to send via SMS.
         * This code is already generated and stored in JWT token.
         */
        String verificationCode
) {
}