package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user requests a password reset.
 *
 * <p>This event is triggered when an unauthenticated user initiates the
 * password recovery flow. A reset token is generated and sent via email.
 *
 * <p>Security note: This event may indicate an account takeover attempt
 * if not initiated by the account owner. The device information helps
 * track the origin of the request.
 *
 * <p>Published by: {@code PasswordServiceImpl.requestPasswordReset()}
 * <p>Consumed by:
 * <ul>
 *   <li>{@code PasswordNotificationListener} — sends reset email with token</li>
 *   <li>{@code PasswordActivityLogListener} — logs PASSWORD_RESET_REQUESTED action</li>
 * </ul>
 *
 * @param user   the user who requested the reset; never null
 * @param token  the public ID of the generated reset token; never null
 * @param device the device from which the request originated; may be null
 */
public record RequestPasswordResetEvent(
        User user,
        String token,
        Device device
) {}