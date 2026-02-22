package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user requests a new password reset token.
 *
 * <p>This event is triggered when a user attempts to use an expired reset token
 * and requests a fresh one. The old token is revoked and a new email is sent.
 *
 * <p>Security note: Multiple token refresh requests in a short period may indicate
 * delivery issues or a potential attack. The device information helps correlate
 * requests and detect suspicious patterns.
 *
 * <p>Published by: {@code PasswordServiceImpl.requestPasswordToken()}
 * <p>Consumed by:
 * <ul>
 *   <li>{@code PasswordNotificationListener} — sends new reset email</li>
 *   <li>{@code PasswordActivityLogListener} — logs PASSWORD_RESET_TOKEN_REFRESHED action</li>
 * </ul>
 *
 * @param user     the user who requested the token refresh; never null
 * @param newToken the public ID of the newly generated reset token; never null
 * @param device   the device from which the request originated; may be null
 */
public record RequestPasswordTokenEvent(
        User user,
        String newToken,
        Device device
) {}