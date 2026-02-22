package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a password reset is successfully completed.
 *
 * <p>This event is triggered when a user successfully resets their password
 * using a valid recovery token (email link or SMS code). It marks the completion
 * of the password recovery flow.
 *
 * <p>This differs from {@link PasswordChangedEvent} which is triggered when
 * an authenticated user changes their password by providing the current one.
 *
 * <p>Published by: {@code PasswordServiceImpl.resetPassword()}
 * <p>Consumed by:
 * <ul>
 *   <li>{@code PasswordNotificationListener} — sends confirmation email</li>
 *   <li>{@code PasswordActivityLogListener} — logs PASSWORD_RESET_COMPLETED action</li>
 * </ul>
 *
 * @param user   the user whose password was reset; never null
 * @param device the device from which the reset was completed; may be null
 */
public record PasswordResetCompletedEvent(
        User user,
        Device device
) {}