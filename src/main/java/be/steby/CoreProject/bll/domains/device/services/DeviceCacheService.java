package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.domains.device.utils.CachedDevice;
import be.steby.CoreProject.dl.entities.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for caching Device entities to reduce database queries.
 * Provides automatic cache expiration and event-based cache invalidation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceCacheService {

    private final DeviceService deviceService;
    private final Map<Long, CachedDevice> deviceCache = new ConcurrentHashMap<>();

    @Value("${device.cache.expiration:300000}") // 5 minutes default
    private long cacheExpirationMs;

    /**
     * Get device from cache or database if not cached/expired
     * @param deviceId The device ID to retrieve
     * @return The device entity
     */
    public Device getDevice(Long deviceId) {
        if (deviceId == null) {
            return null;
        }

        CachedDevice cached = deviceCache.get(deviceId);

        // Check if cache hit and not expired
        if (cached != null && !cached.isExpired(cacheExpirationMs)) {
            log.debug("Cache hit for device ID: {}", deviceId);
            return cached.getDevice();
        }

        // Cache miss or expired - fetch from database
        log.debug("Cache miss for device ID: {}, fetching from database", deviceId);
        Device device = deviceService.getDeviceById(deviceId);

        if (device != null) {
            deviceCache.put(deviceId, new CachedDevice(device));
        }

        return device;
    }

    /**
     * Invalidate cache for a specific device
     * @param deviceId The device ID to invalidate
     */
    public void invalidateDevice(Long deviceId) {
        if (deviceId != null) {
            deviceCache.remove(deviceId);
            log.debug("Invalidated cache for device ID: {}", deviceId);
        }
    }

    /**
     * Invalidate cache for a device entity
     * @param device The device to invalidate
     */
    public void invalidateDevice(Device device) {
        if (device != null) {
            invalidateDevice(device.getId());
        }
    }

    /**
     * Update cache with fresh device data
     * @param device The device to cache
     */
    public void updateCache(Device device) {
        if (device != null && device.getId() != null) {
            deviceCache.put(device.getId(), new CachedDevice(device));
            log.debug("Updated cache for device ID: {}", device.getId());
        }
    }

    /**
     * Clear all cached devices
     */
    public void clearAll() {
        int size = deviceCache.size();
        deviceCache.clear();
        log.info("Cleared all device cache entries: {} items removed", size);
    }

    /**
     * Get current cache size
     * @return Number of cached devices
     */
    public int getCacheSize() {
        return deviceCache.size();
    }

    /**
     * Clean expired entries from cache
     * Called periodically to prevent memory leaks
     */
    public void cleanExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        AtomicInteger removedCount = new AtomicInteger();

        deviceCache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(cacheExpirationMs);
            if (expired) {
                removedCount.getAndIncrement();
            }
            return expired;
        });

        if (removedCount.get() > 0) {
            log.debug("Cleaned {} expired cache entries", removedCount);
        }
    }

    /**
     * Event listener to automatically invalidate cache when device is updated
     * This ensures cache consistency when devices are modified
     */
    @EventListener
    public void handleDeviceUpdated(Device device) {
        updateCache(device);
    }
}