package be.steby.CoreProject.bll.domains.password.events.email;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event triggered when a password change confirmation email should be sent.
 * 
 * <p>This event is published after a successful password change to notify
 * the user via email for security purposes. This helps detect unauthorized
 * password changes.
 * 
 * <p>Published by: PasswordServiceImpl.savePassword()
 * <p>Consumed by: PasswordEmailNotificationListener
 */
public record PasswordChangeEmailNotificationEvent(
        /**
         * The user whose password was changed.
         */
        User user
) {
}