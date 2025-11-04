package be.steby.CoreProject.bll.domains.password.events.email;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event triggered when a password reset token refresh via email is requested.
 * 
 * <p>This event is published when a user requests a new password reset token
 * because their previous token expired. The old token is revoked and a new
 * email with a fresh token is sent.
 * 
 * <p>Published by: PasswordServiceImpl.requestPasswordToken()
 * <p>Consumed by: PasswordEmailNotificationListener
 */
public record PasswordResetTokenRefreshRequestedEvent(
        /**
         * The user who requested the token refresh.
         */
        User user,
        
        /**
         * The public ID of the new password reset token.
         * Used to generate the new reset link in the email.
         */
        String newTokenPublicId
) {
}