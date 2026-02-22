package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a password change attempt fails.
 *
 * <p>This event is triggered when an authenticated user attempts to change
 * their password but provides an incorrect current password.
 *
 * <p>Security note: Multiple failed attempts may indicate brute-force attack
 * on an authenticated session. This event enables detection of such patterns
 * and potential account lockout.
 *
 * <p>Published by: {@code PasswordServiceImpl.changePassword()}
 * <p>Consumed by:
 * <ul>
 *   <li>{@code PasswordActivityLogListener} — logs PASSWORD_CHANGE_FAILED action</li>
 * </ul>
 *
 * @param user          the user who attempted the change; never null
 * @param device        the device from which the attempt originated; may be null
 * @param failureReason human-readable description of why the change failed; never null
 */
public record PasswordChangeFailedEvent(
        User user,
        Device device,
        String failureReason
) {}