package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user is forcibly logged out of all devices by a
 * system-triggered action, as opposed to a voluntary {@link UserLogoutEvent}.
 *
 * <p>This event intentionally carries no {@code Device} reference because a
 * forced logout always targets all active sessions simultaneously — there is no
 * single "source" device.</p>
 *
 * <p>Forced logout scenarios:</p>
 * <ul>
 *   <li>Account deactivation (self or admin) — handled by
 *       {@code AuthDeactivationListener}.</li>
 *   <li>Admin action: {@code FORCE_LOGOUT} — triggered explicitly by an
 *       administrator from the admin panel.</li>
 *   <li>Security breach response — all sessions invalidated after a detected
 *       compromise.</li>
 * </ul>
 *
 * <p>Listeners of this event should log an {@code AuthAction.FORCE_LOGOUT}
 * activity entry. Downstream effects (token revocation, device disconnection)
 * are the responsibility of the publisher, not the listeners.</p>
 *
 * <p>Published by: {@code AuthDeactivationListener}, admin force-logout flow</p>
 *
 * @param user   The user who was forcibly logged out. Always non-null.
 * @param reason Human-readable description of why the logout was forced,
 *               e.g. "Account deactivated", "Security breach", "Admin action".
 */
public record ForceLogoutEvent(
        User user,
        String reason
) {}