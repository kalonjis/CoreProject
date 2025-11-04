package be.steby.CoreProject.bll.domains.password.events.sms;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event triggered when a password change SMS alert should be sent.
 * 
 * <p>This event is published after a successful password change to notify
 * the user via SMS for additional security. This provides multi-channel
 * notification for important security events.
 * 
 * <p>Published by: PasswordServiceImpl.savePassword()
 * <p>Consumed by: PasswordSmsNotificationListener
 */
public record PasswordChangeSmsAlertEvent(
        /**
         * The user whose password was changed.
         */
        User user
) {
}