package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.domains.device.events.DeviceCreatedOrUpdatedEvent;
import be.steby.CoreProject.bll.domains.device.events.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.domains.device.models.DeviceSecurityResult;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dl.entities.Device;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security-focused service for device validation and caching.
 * This service is responsible for:
 * - Secure device retrieval with cache optimization
 * - Device fingerprint validation
 * - Cache management and invalidation
 * - Security logging for device-related operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSecurityService {

    private final DeviceRepository deviceRepository;
    private final Map<Long, CachedDeviceInfo> secureDeviceCache = new ConcurrentHashMap<>();

    @Value("${device.cache.expiration:300000}") // 5 minutes default
    private long cacheExpirationMs;

    /**
     * Main method for secure device retrieval and validation.
     * Used exclusively by JwtFilter for authentication purposes.
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

        // 2. Cache lookup with security validation
        CachedDeviceInfo cached = secureDeviceCache.get(deviceId);
        if (cached != null && !cached.isExpired() && cached.matchesFingerprint(fingerprint)) {
            log.debug("Device {} retrieved from cache", deviceId);
            return DeviceSecurityResult.success(cached.getDevice());
        }

        // 3. Database lookup with cross-validation
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.warn("Device not found in database: {}", deviceId);
            return DeviceSecurityResult.failure("Device not found");
        }

        Device device = deviceOpt.get();

        // 4. Critical security validation - fingerprint must match
        if (!device.getFingerprint().equals(fingerprint)) {
            log.error("SECURITY ALERT: Fingerprint mismatch for device {} - Expected: {}, Received: {}",
                    deviceId, device.getFingerprint(), fingerprint);
            return DeviceSecurityResult.failure("Device fingerprint mismatch");
        }

        // 5. Update cache with validated device
        secureDeviceCache.put(deviceId, new CachedDeviceInfo(device, fingerprint));
        log.debug("Device {} cached after database lookup", deviceId);

        return DeviceSecurityResult.success(device);
    }

    /**
     * Updates cache when a device is created or modified.
     * Ensures cache consistency across the application.
     */
    @EventListener
    public void handleDeviceCreatedOrUpdated(DeviceCreatedOrUpdatedEvent event) {
        Device device = event.getDevice();

        if (device.getId() != null && device.getFingerprint() != null) {
            secureDeviceCache.put(device.getId(),
                    new CachedDeviceInfo(device, device.getFingerprint()));

            log.debug("Device cache updated for device {} (action: {})",
                    device.getId(), event.getAction());
        }
    }

    /**
     * Invalidates cache when device security properties change.
     */
    @EventListener
    public void handleDeviceSecurityChange(DeviceTrustLevelChangedEvent event) {
        secureDeviceCache.remove(event.deviceId());
        log.info("Device cache invalidated due to trust level change: {}", event.deviceId());
    }

    /**
     * Removes expired entries from cache automatically.
     * Runs every 10 minutes to maintain cache efficiency.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanExpiredCache() {
        int sizeBefore = secureDeviceCache.size();
        secureDeviceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int sizeAfter = secureDeviceCache.size();

        if (sizeBefore != sizeAfter) {
            log.debug("Cleaned {} expired entries from device cache. Size: {} -> {}",
                    sizeBefore - sizeAfter, sizeBefore, sizeAfter);
        }
    }

    /**
     * Manually invalidates a specific device from cache.
     * Used for security operations like blacklisting.
     */
    public void invalidateDeviceCache(Long deviceId) {
        if (secureDeviceCache.remove(deviceId) != null) {
            log.info("Device {} manually removed from cache", deviceId);
        }
    }

    /**
     * Gets current cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStats() {
        int totalEntries = secureDeviceCache.size();
        long expiredEntries = secureDeviceCache.values().stream()
                .mapToLong(entry -> entry.isExpired() ? 1 : 0)
                .sum();

        return Map.of(
                "totalEntries", totalEntries,
                "expiredEntries", expiredEntries,
                "validEntries", totalEntries - expiredEntries,
                "cacheExpirationMs", cacheExpirationMs
        );
    }

    /**
     * Internal class for caching device information with security validation.
     */
    @Data
    private static class CachedDeviceInfo {
        private final Device device;
        private final String fingerprint;
        private final long timestamp;

        public CachedDeviceInfo(Device device, String fingerprint) {
            this.device = device;
            this.fingerprint = fingerprint;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 300000; // 5 minutes hardcoded for security
        }

        public boolean matchesFingerprint(String fingerprint) {
            return this.fingerprint != null && this.fingerprint.equals(fingerprint);
        }
    }
}