package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.device.cache.DeviceCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**TODO decide what to do with this : keep or remove from project
 * Admin controller for device cache management and monitoring.
 * Provides endpoints for monitoring cache performance, health checks,
 * and administrative operations.
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN')")
public class CacheAdminController {

    private final DeviceCacheService deviceCacheService;

    /**
     * Get detailed cache statistics
     * GET /api/admin/cache/device/stats
     */
    @GetMapping("/device/stats")
    public ResponseEntity<Map<String, Object>> getDeviceCacheStats() {
        Map<String, Object> stats = deviceCacheService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Force cleanup of expired entries
     * POST /api/admin/cache/device/cleanup
     */
    @PostMapping("/device/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupDeviceCache() {
        Map<String, Object> statsBefore = deviceCacheService.getStats();

        int entriesRemoved = deviceCacheService.cleanExpired();

        Map<String, Object> statsAfter = deviceCacheService.getStats();

        return ResponseEntity.ok(Map.of(
                "message", "Cache cleanup completed",
                "entriesRemoved", entriesRemoved,
                "statsBefore", statsBefore,
                "statsAfter", statsAfter
        ));
    }

    /**
     * Invalidate specific device from cache
     * DELETE /api/admin/cache/device/{deviceId}
     */
    @DeleteMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, String>> invalidateDevice(@PathVariable Long deviceId) {
        boolean removed = deviceCacheService.remove(deviceId);

        String message = removed ? "Device cache invalidated" : "Device not found in cache";

        log.info("Device {} cache invalidation attempt by admin: {}", deviceId, removed ? "success" : "not found");

        return ResponseEntity.ok(Map.of(
                "message", message,
                "deviceId", deviceId.toString(),
                "removed", String.valueOf(removed)
        ));
    }

    /**
     * Health check endpoint for cache
     * GET /api/admin/cache/device/health
     */
    @GetMapping("/device/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> stats = deviceCacheService.getStats();

        double fillPercentage = (Double) stats.get("fillPercentage");
        double memoryMB = (Double) stats.get("estimatedMemoryMB");

        String status = determineHealthStatus(fillPercentage, memoryMB);
        String recommendation = getHealthRecommendation(fillPercentage, memoryMB);

        return ResponseEntity.ok(Map.of(
                "status", status,
                "fillPercentage", fillPercentage,
                "memoryMB", memoryMB,
                "recommendation", recommendation,
                "stats", stats
        ));
    }

    /**
     * Trigger cache maintenance (cleanup + eviction if needed)
     * POST /api/admin/cache/device/maintenance
     */
    @PostMapping("/device/maintenance")
    public ResponseEntity<Map<String, Object>> performMaintenance() {
        Map<String, Object> statsBefore = deviceCacheService.getStats();

        deviceCacheService.performMaintenance();

        Map<String, Object> statsAfter = deviceCacheService.getStats();

        int entriesRemoved = (Integer) statsBefore.get("totalEntries") - (Integer) statsAfter.get("totalEntries");

        log.info("Cache maintenance performed by admin: {} entries removed", entriesRemoved);

        return ResponseEntity.ok(Map.of(
                "message", "Cache maintenance completed",
                "entriesRemoved", entriesRemoved,
                "statsBefore", statsBefore,
                "statsAfter", statsAfter
        ));
    }

    /**
     * Get cache size information
     * GET /api/admin/cache/device/size
     */
    @GetMapping("/device/size")
    public ResponseEntity<Map<String, Object>> getCacheSize() {
        int currentSize = deviceCacheService.size();
        Map<String, Object> stats = deviceCacheService.getStats();

        return ResponseEntity.ok(Map.of(
                "currentSize", currentSize,
                "maxSize", stats.get("maxSize"),
                "fillPercentage", stats.get("fillPercentage"),
                "nearCapacity", deviceCacheService.isNearCapacity()
        ));
    }

    // Helper methods

    private String determineHealthStatus(double fillPercentage, double memoryMB) {
        if (fillPercentage > 90 || memoryMB > 100) {
            return "CRITICAL";
        } else if (fillPercentage > 80 || memoryMB > 50) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    private String getHealthRecommendation(double fillPercentage, double memoryMB) {
        if (fillPercentage > 90) {
            return "Immediate action required: Increase max-size or reduce expiration time";
        } else if (fillPercentage > 80) {
            return "Monitor closely: Consider increasing max-size or reducing expiration time";
        } else if (memoryMB > 50) {
            return "High memory usage: Monitor memory consumption";
        } else {
            return "Cache is healthy";
        }
    }
}