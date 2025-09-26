package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.domains.device.cache.DeviceCacheService;
import be.steby.CoreProject.bll.domains.device.cache.models.CachedDeviceInfo;
import be.steby.CoreProject.bll.domains.device.events.DevicePersistedEvent;
import be.steby.CoreProject.bll.domains.device.events.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.domains.device.models.DeviceSecurityResult;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dl.entities.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Security-focused service for device validation.
 *
 * This service focuses exclusively on BUSINESS LOGIC:
 * - Device security validation
 * - Fingerprint matching and verification
 * - Security logging and audit
 * - Integration with domain events
 *
 * CACHE OPERATIONS are delegated to DeviceCacheService following DDD principles.
 * This separation allows for easy cache implementation switching (Memory → Redis)
 * without affecting the core security logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceAuthenticationService {

    // Repository for database operations
    private final DeviceRepository deviceRepository;

    // ✅ DDD: Cache operations delegated to dedicated service
    private final DeviceCacheService deviceCacheService;

    /**
     * Main method for secure device retrieval and validation.
     * Used exclusively by JwtFilter for authentication purposes.
     *
     * SECURITY FLOW:
     * 1. Input validation
     * 2. Cache lookup with fingerprint validation
     * 3. Database lookup with cross-validation
     * 4. Fingerprint security check
     * 5. Cache update
     *
     * @param deviceId Device ID from JWT claims
     * @param fingerprint Device fingerprint from JWT claims
     * @return DeviceSecurityResult containing validation outcome
     */
    public DeviceSecurityResult getSecureDeviceFromCacheOrDatabase(Long deviceId, String fingerprint) {
        // 1. Input validation for security
        if (deviceId == null || fingerprint == null || fingerprint.trim().isEmpty()) {
            log.warn("Invalid device credentials provided - deviceId: {}, fingerprint: {}",
                    deviceId, fingerprint != null ? "present" : "null");
            return DeviceSecurityResult.failure("Invalid device credentials");
        }

        // 2. ✅ DDD: Cache lookup delegated to cache service
        Optional<CachedDeviceInfo> cached = deviceCacheService.get(deviceId);
        if (cached.isPresent() && cached.get().matchesFingerprint(fingerprint)) {
            log.debug("Device {} retrieved from cache", deviceId);
            return DeviceSecurityResult.success(cached.get().getDevice());
        }

        // 3. Database lookup with cross-validation
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.warn("Device not found in database: {}", deviceId);
            return DeviceSecurityResult.failure("Device not found");
        }

        Device device = deviceOpt.get();

        // 4. ⚠️ CRITICAL SECURITY VALIDATION - fingerprint must match
        if (!device.getFingerprint().equals(fingerprint)) {
            log.error("🚨 SECURITY ALERT: Fingerprint mismatch for device {} - Expected: {}, Received: {}",
                    deviceId, device.getFingerprint(), fingerprint);
            return DeviceSecurityResult.failure("Device fingerprint mismatch");
        }

        // 5. ✅ DDD: Cache update delegated to cache service
        deviceCacheService.put(deviceId, device, fingerprint);
        log.debug("Device {} cached after database lookup", deviceId);

        return DeviceSecurityResult.success(device);
    }

    /**
     * ✅ DDD: Event listener for device persistence events
     * Updates cache when a device is created or modified.
     * Cache logic is delegated to the cache service.
     */
    @EventListener
    public void handleDeviceCreatedOrUpdated(DevicePersistedEvent event) {
        Device device = event.device();

        if (device.getId() != null && device.getFingerprint() != null) {
            // ✅ DDD: Cache update delegated
            deviceCacheService.put(device.getId(), device, device.getFingerprint());
            log.debug("Device cache updated for device {}", device.getId());
        }
    }

    /**
     * ✅ DDD: Event listener for device security changes
     * Invalidates cache when device security properties change.
     */
    @EventListener
    public void handleDeviceSecurityChange(DeviceTrustLevelChangedEvent event) {
        // ✅ DDD: Cache invalidation delegated
        boolean removed = deviceCacheService.remove(event.deviceId());
        if (removed) {
            log.info("Device cache invalidated due to trust level change: {}", event.deviceId());
        }
    }

    /**
     * ✅ DDD: Manually invalidates a specific device from cache.
     * Used for security operations like blacklisting.
     */
    public void invalidateDeviceCache(Long deviceId) {
        boolean removed = deviceCacheService.remove(deviceId);
        if (removed) {
            log.info("Device {} manually removed from cache", deviceId);
        }
    }

    /**
     * ✅ DDD: Gets cache statistics for monitoring.
     * Delegates to cache service for implementation details.
     */
    public Map<String, Object> getCacheStats() {
        return deviceCacheService.getStats();
    }

    /**
     * ✅ DDD: Forces cache cleanup.
     * Delegates to cache service for implementation.
     */
    public int cleanExpiredCache() {
        return deviceCacheService.cleanExpired();
    }

    /**
     * ✅ DDD: Checks if cache needs attention.
     * Business logic can react to cache capacity issues.
     */
    public boolean isCacheNearCapacity() {
        return deviceCacheService.isNearCapacity();
    }

    /**
     * ✅ DDD: Triggers cache maintenance.
     * Can be called by admin endpoints or scheduled tasks.
     */
    public void performCacheMaintenance() {
        deviceCacheService.performMaintenance();
        log.info("Cache maintenance completed via DeviceSecurityService");
    }
}