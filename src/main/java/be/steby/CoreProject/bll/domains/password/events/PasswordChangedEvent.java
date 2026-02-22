package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user successfully changes their password.
 *
 * <p>This event is triggered after an authenticated user changes their password
 * by providing their current password. It differs from password reset, which
 * uses a token-based recovery flow.
 *
 * <p>Published by: {@code PasswordServiceImpl.changePassword()}
 * <p>Consumed by:
 * <ul>
 *   <li>{@code PasswordNotificationListener} — sends confirmation email</li>
 *   <li>{@code PasswordActivityLogListener} — logs PASSWORD_CHANGED action</li>
 * </ul>
 *
 * @param user   the user whose password was changed; never null
 * @param device the device from which the change was initiated; may be null for token-based flows
 */
public record PasswordChangedEvent(
        User user,
        Device device
) {}