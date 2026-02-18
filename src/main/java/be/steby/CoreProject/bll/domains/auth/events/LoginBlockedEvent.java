package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a login attempt is blocked by a security policy before
 * authentication can even be evaluated.
 *
 * <p>This is distinct from {@link UserLoginFailedEvent} (bad credentials for a
 * known user) and {@link AccountLockedEvent} (lock applied after N failures).
 * A blocked login means the request was rejected upfront — the credentials were
 * never checked — because a rate-limiting or blocking rule was triggered.</p>
 *
 * <p>Blocking scenarios covered by {@code SecurityAction}:</p>
 * <ul>
 *   <li>{@code IP_BLOCKED} — the source IP has been blacklisted.</li>
 *   <li>{@code USERNAME_BLOCKED} — the username is under a temporary ban.</li>
 *   <li>{@code COMBINED_BLOCKED} — the username + IP combination is blocked.</li>
 * </ul>
 *
 * <p>Published by: {@code LoginAttemptService} (when a block rule is matched
 * before delegating to the authentication provider)</p>
 *
 * @param user        The user targeted by the blocked attempt. May be null when
 *                    the block is IP-based and the username does not resolve to
 *                    any existing account (e.g. username enumeration attempt).
 * @param device      The device from which the blocked attempt originated.
 *                    May be null if device resolution failed before the block
 *                    check.
 * @param blockReason Human-readable description of why the login was blocked,
 *                    e.g. "IP address blocked", "Username temporarily banned".
 */
public record LoginBlockedEvent(
        User user,
        Device device,
        String blockReason
) {}