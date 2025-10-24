package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.user.services.UserAuthenticationService;
import be.steby.CoreProject.bll.domains.user.cache.listeners.UserCacheEventListener;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**TODO decide what to do with this : keep or remove from project
 * Admin controller for user cache management and monitoring.
 * Provides endpoints for monitoring cache performance, health checks,
 * manual cache operations, and security incident response.
 *
 * All endpoints require ADMIN privileges for security.
 */
@RestController
@RequestMapping("/api/admin/cache/user")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN')")
public class UserCacheAdminController {

    private final UserAuthenticationService userAuthenticationService;
    private final UserCacheEventListener userCacheEventListener;
    private final UserService userService;

    // ===============================
    // CACHE MONITORING ENDPOINTS
    // ===============================

    /**
     * Get comprehensive user cache statistics.
     * GET /api/admin/cache/user/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserCacheStats() {
        log.info("Admin requesting user cache statistics");

        Map<String, Object> stats = userAuthenticationService.getCacheStats();

        // Add additional context
        Map<String, Object> response = Map.of(
                "cacheStats", stats,
                "cacheHealthy", userAuthenticationService.isCacheNearCapacity(),
                "recommendedAction", getRecommendedAction(stats),
                "timestamp", java.time.Instant.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get cache health check.
     * GET /api/admin/cache/user/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getUserCacheHealth() {
        Map<String, Object> stats = userAuthenticationService.getCacheStats();
        boolean isHealthy = userAuthenticationService.isCacheNearCapacity();
        boolean isNearCapacity = userAuthenticationService.isCacheNearCapacity();

        String status = isHealthy ? "HEALTHY" : "NEEDS_ATTENTION";
        String message = isHealthy ? "Cache operating normally" : "Cache requires maintenance";

        Map<String, Object> health = Map.of(
                "status", status,
                "message", message,
                "isHealthy", isHealthy,
                "isNearCapacity", isNearCapacity,
                "currentSize", stats.get("size"),
                "maxSize", stats.get("maxSize"),
                "fillPercentage", stats.get("fillPercentage"),
                "hitRate", stats.get("hitRate")
        );

        return ResponseEntity.ok(health);
    }

    // ===============================
    // CACHE MAINTENANCE ENDPOINTS
    // ===============================

    /**
     * Force cleanup of expired cache entries.
     * POST /api/admin/cache/user/cleanup
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupUserCache() {
        log.info("Admin triggering user cache cleanup");

        Map<String, Object> statsBefore = userAuthenticationService.getCacheStats();
        int entriesRemoved = userAuthenticationService.cleanExpiredCache();
        Map<String, Object> statsAfter = userAuthenticationService.getCacheStats();

        Map<String, Object> response = Map.of(
                "message", "User cache cleanup completed",
                "entriesRemoved", entriesRemoved,
                "statsBefore", Map.of(
                        "size", statsBefore.get("size"),
                        "expiredEntries", statsBefore.get("expiredEntries")
                ),
                "statsAfter", Map.of(
                        "size", statsAfter.get("size"),
                        "expiredEntries", statsAfter.get("expiredEntries")
                )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger comprehensive cache maintenance.
     * POST /api/admin/cache/user/maintenance
     */
    @PostMapping("/maintenance")
    public ResponseEntity<Map<String, Object>> performCacheMaintenance() {
        log.info("Admin triggering comprehensive user cache maintenance");

        Map<String, Object> statsBefore = userAuthenticationService.getCacheStats();
        userAuthenticationService.performCacheMaintenance();
        Map<String, Object> statsAfter = userAuthenticationService.getCacheStats();

        Map<String, Object> response = Map.of(
                "message", "User cache maintenance completed",
                "statsBefore", statsBefore,
                "statsAfter", statsAfter,
                "sizeDifference", (Integer) statsAfter.get("size") - (Integer) statsBefore.get("size")
        );

        return ResponseEntity.ok(response);
    }

    // ===============================
    // MANUAL CACHE OPERATIONS
    // ===============================

    /**
     * Invalidate specific user from cache by username.
     * DELETE /api/admin/cache/user/username/{username}
     */
    @DeleteMapping("/username/{username}")
    public ResponseEntity<Map<String, String>> invalidateUserByUsername(
            @PathVariable @NotBlank String username) {

        log.info("Admin invalidating user cache for username: {}", username);

        // Trigger manual invalidation via event listener for proper logging
        userCacheEventListener.handleManualCacheInvalidation(
                "Admin manual invalidation", null, username);

        return ResponseEntity.ok(Map.of(
                "message", "User cache invalidation triggered",
                "username", username,
                "action", "removed"
        ));
    }

    /**
     * Invalidate specific user from cache by user ID.
     * DELETE /api/admin/cache/user/id/{userId}
     */
    @DeleteMapping("/id/{userId}")
    public ResponseEntity<Map<String, Object>> invalidateUserById(
            @PathVariable @NotNull Long userId) {

        log.info("Admin invalidating user cache for userId: {}", userId);

        // Trigger manual invalidation via event listener for proper logging
        userCacheEventListener.handleManualCacheInvalidation(
                "Admin manual invalidation", userId, null);

        return ResponseEntity.ok(Map.of(
                "message", "User cache invalidation triggered",
                "userId", userId,
                "action", "removed"
        ));
    }

    /**
     * Bulk invalidate users by role.
     * DELETE /api/admin/cache/user/role/{role}
     */
    @DeleteMapping("/role/{role}")
    public ResponseEntity<Map<String, Object>> invalidateUsersByRole(
            @PathVariable @NotBlank String role) {

        log.info("Admin invalidating user cache for role: {}", role);

        // Validate role exists
        try {
            UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid role specified",
                    "role", role,
                    "validRoles", java.util.Arrays.toString(UserRole.values())
            ));
        }

        int invalidatedCount = userAuthenticationService.invalidateUsersByRole(role);

        return ResponseEntity.ok(Map.of(
                "message", "Bulk user cache invalidation completed",
                "role", role,
                "invalidatedCount", invalidatedCount
        ));
    }

    // ===============================
    // SECURITY INCIDENT RESPONSE
    // ===============================

    /**
     * Emergency cache response for security incidents.
     * POST /api/admin/cache/user/security-incident
     */
    @PostMapping("/security-incident")
    public ResponseEntity<Map<String, Object>> handleSecurityIncident(
            @RequestBody SecurityIncidentRequest request) {

        log.error("Security incident cache response initiated - incident: {}", request.incidentId());

        // Generate unique incident ID if not provided
        String incidentId = request.incidentId() != null ?
                request.incidentId() : "INC-" + System.currentTimeMillis();

        // Trigger emergency cache maintenance
        userCacheEventListener.handleSecurityIncidentCacheClear(incidentId, request.reason());

        Map<String, Object> response = Map.of(
                "message", "Security incident cache response completed",
                "incidentId", incidentId,
                "reason", request.reason(),
                "action", "emergency_maintenance",
                "timestamp", java.time.Instant.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get cache configuration details.
     * GET /api/admin/cache/user/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getCacheConfiguration() {
        Map<String, Object> stats = userAuthenticationService.getCacheStats();

        Map<String, Object> config = Map.of(
                "type", stats.get("type"),
                "maxSize", stats.get("maxSize"),
                "currentSize", stats.get("size"),
                "fillPercentage", stats.get("fillPercentage"),
                "hitRate", stats.get("hitRate"),
                "isNearCapacity", userAuthenticationService.isCacheNearCapacity()
        );

        return ResponseEntity.ok(config);
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    private String getRecommendedAction(Map<String, Object> stats) {
        Object fillPercentageObj = stats.get("fillPercentage");
        String hitRate = (String) stats.get("hitRate");

        // Handle both Integer and Long for fillPercentage
        int fillPercentage;
        if (fillPercentageObj instanceof Integer) {
            fillPercentage = (Integer) fillPercentageObj;
        } else if (fillPercentageObj instanceof Long) {
            fillPercentage = ((Long) fillPercentageObj).intValue();
        } else {
            fillPercentage = 0; // Default fallback
        }

        if (fillPercentage > 90) {
            return "URGENT: Cache nearly full - perform maintenance immediately";
        } else if (fillPercentage > 80) {
            return "WARNING: Cache getting full - schedule maintenance soon";
        } else if (hitRate != null && hitRate.replace("%", "").equals("0.00")) {
            return "INFO: Low hit rate - consider cache warming strategies";
        } else {
            return "OK: Cache operating normally";
        }
    }

    // ===============================
    // REQUEST/RESPONSE MODELS
    // ===============================

    /**
     * Request model for security incident cache operations.
     */
    public record SecurityIncidentRequest(
            String incidentId,
            @NotBlank String reason
    ) {}
}