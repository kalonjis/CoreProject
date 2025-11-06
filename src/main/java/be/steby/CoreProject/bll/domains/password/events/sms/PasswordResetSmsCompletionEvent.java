package be.steby.CoreProject.bll.domains.password.events.sms;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event triggered when a password reset completion SMS notification should be sent.
 * 
 * <p>This event is published after a successful password reset via SMS code
 * to confirm to the user that their password has been successfully changed.
 * 
 * <p>Published by: PasswordServiceImpl.resetPassword() (when SMS flow)
 * <p>Consumed by: PasswordSmsNotificationListener
 */
public record PasswordResetSmsCompletionEvent(
        /**
         * The user whose password was reset.
         */
        User user
) {
}