package be.steby.CoreProject.bll.domains.password.events.email;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event triggered when a password reset via email is requested.
 *
 * <p>This event specifically handles email-based password reset notifications.
 * It carries the information needed to send a password reset email with a link.
 *
 * <p>Published by: PasswordServiceImpl.handleEmailPasswordReset()
 * <p>Consumed by: PasswordEmailNotificationListener
 */
public record PasswordResetEmailRequestedEvent(
        /**
         * The user who requested the password reset.
         */
        User user,

        /**
         * The public ID of the password reset token.
         * Used to generate the reset link in the email.
         */
        String tokenPublicId
) {
}