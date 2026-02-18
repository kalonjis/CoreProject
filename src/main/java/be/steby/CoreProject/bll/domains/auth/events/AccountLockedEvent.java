package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Event published when a user account is temporarily locked due to too many
 * failed authentication attempts.
 *
 * <p>This event is raised by {@code LoginAttemptService} once the failed attempt
 * threshold is crossed. It is distinct from {@link UserLoginFailedEvent}, which
 * is raised on every individual failure — this one fires only at the moment the
 * lock is applied.</p>
 *
 * <p>Use cases for listeners:</p>
 * <ul>
 *   <li>Persisting an {@code ACCOUNT_LOCKED} activity log entry.</li>
 *   <li>Sending a security alert email to the account owner.</li>
 *   <li>Triggering additional security monitoring.</li>
 * </ul>
 *
 * <p>Published by: {@code LoginAttemptService} (when lock threshold is reached)</p>
 *
 * @param user       The user whose account has been locked. Always non-null.
 * @param device     The device that triggered the final failed attempt leading
 *                   to the lock. May be null if device resolution failed.
 * @param unlockTime The instant at which the account will be automatically
 *                   unlocked. Null if the lock duration is indefinite and
 *                   requires manual intervention.
 */
public record AccountLockedEvent(
        User user,
        Device device,
        Instant unlockTime
) {}