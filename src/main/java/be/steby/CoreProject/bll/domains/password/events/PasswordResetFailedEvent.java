package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a password reset attempt fails.
 *
 * <p>This event is triggered when a user attempts to reset their password
 * but the provided token is invalid, expired, or already used.
 *
 * <p>Security note: High volume of failed reset attempts may indicate
 * brute-force token guessing. The device information helps identify
 * the source of suspicious activity.
 *
 * <p>Published by: {@code PasswordServiceImpl.resetPassword()}
 * <p>Consumed by:
 * <ul>
 *   <li>{@code PasswordActivityLogListener} — logs PASSWORD_RESET_FAILED action</li>
 * </ul>
 *
 * @param user          the user associated with the reset attempt; may be null if token is completely invalid
 * @param device        the device from which the attempt originated; may be null
 * @param failureReason human-readable description of why the reset failed; never null
 */
public record PasswordResetFailedEvent(
        User user,
        Device device,
        String failureReason
) {}