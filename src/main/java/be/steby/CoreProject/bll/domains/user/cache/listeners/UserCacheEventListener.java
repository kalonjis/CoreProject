package be.steby.CoreProject.bll.domains.user.cache.listeners;

import be.steby.CoreProject.bll.domains.auth.events.ForceLogoutEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.domains.user.cache.UserCacheService;
import be.steby.CoreProject.bll.domains.user.events.UserPersistedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event listener for user cache management.
 *
 * Following the same pattern as DeviceCacheService for consistency.
 * Uses UserPersistedEvent with full user data for efficient cache updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(UserCacheService.class)
public class UserCacheEventListener {

    private final UserCacheService userCacheService;

    /**
     * Handles user persistence - updates cache with fresh data.
     * Follows the same pattern as DevicePersistedEvent.
     */
    @EventListener
    @Order(1)
    public void handleUserPersisted(UserPersistedEvent event) {
        userCacheService.put(event.user());
        log.debug("User cache updated due to persistence - userId: {}, username: {}",
                event.user().getId(), event.user().getUsername());
    }

    // =========================================================================
    // Logout
    // =========================================================================

    /**
     * Clears user cache on voluntary logout.
     * The user's token is already revoked at this point; removing the cache
     * entry is a clean-up measure to avoid keeping stale data in memory.
     *
     * @param event published by AuthServiceImpl.logout()
     */
    @EventListener
    @Order(1)
    public void handleUserLogout(UserLogoutEvent event) {
        String username = event.user().getUsername();
        boolean removed = userCacheService.remove(username);
        log.debug("User cache cleared on logout - username: {}, removed: {}", username, removed);
    }

    /**
     * Clears user cache on forced logout (account deactivation, admin action, security breach).
     * Complements token revocation done by the publisher of this event.
     *
     * @param event published by AuthDeactivationListener or admin force-logout flow
     */
    @EventListener
    @Order(1)
    public void handleForceLogout(ForceLogoutEvent event) {
        String username = event.user().getUsername();
        boolean removed = userCacheService.remove(username);
        log.info("User cache cleared on force logout - username: {}, reason: {}, removed: {}",
                username, event.reason(), removed);
    }

    // =========================================================================
    // Admin / Manual operations
    // =========================================================================

    /**
     * Manual cache invalidation trigger for admin operations.
     */
    public void handleManualCacheInvalidation(String reason, Long userId, String username) {
        boolean removed = false;

        if (userId != null) {
            removed = userCacheService.removeById(userId);
        } else if (username != null) {
            removed = userCacheService.remove(username);
        }

        log.info("Manual user cache invalidation - reason: {}, userId: {}, username: {}, removed: {}",
                reason, userId, username, removed);
    }

    /**
     * Emergency cache clear for security incidents.
     */
    public void handleSecurityIncidentCacheClear(String incidentId, String reason) {
        Map<String, Object> statsBefore = userCacheService.getStats();

        userCacheService.performMaintenance();

        Map<String, Object> statsAfter = userCacheService.getStats();

        log.error("Security incident cache maintenance - incident: {}, reason: {}, before: {}, after: {}",
                incidentId, reason, statsBefore.get("size"), statsAfter.get("size"));
    }

    /**
     * Bulk invalidation by role for permission changes.
     */
    public void handleGlobalRolePermissionChange(String roleName) {
        int invalidatedCount = userCacheService.invalidateByRole(roleName);

        log.warn("Bulk user cache invalidation due to role permission change - role: {}, count: {}",
                roleName, invalidatedCount);
    }
}