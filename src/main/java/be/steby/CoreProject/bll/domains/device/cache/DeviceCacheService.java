package be.steby.CoreProject.bll.domains.device.cache;

import be.steby.CoreProject.bll.domains.device.cache.models.CachedDeviceInfo;
import be.steby.CoreProject.dl.entities.Device;

import java.util.Map;
import java.util.Optional;

/**
 * Cache abstraction for device security operations.
 *
 * This interface provides a clean abstraction over the caching mechanism,
 * allowing seamless switching between implementations (In-Memory, Redis, etc.)
 * while maintaining the same security and performance characteristics.
 *
 * Implementations must ensure:
 * - Thread safety for concurrent access
 * - Security validation (fingerprint matching)
 * - Memory management (size limits, eviction)
 * - Performance monitoring and statistics
 */
public interface DeviceCacheService {

    /**
     * Retrieves a device from cache if present and valid.
     *
     * @param deviceId The device ID to look up
     * @return Optional containing the cached device info, or empty if not found/expired
     */
    Optional<CachedDeviceInfo> get(Long deviceId);

    /**
     * Stores or updates a device in cache with intelligent update strategy.
     *
     * - If device exists with same fingerprint → update in-place
     * - If device exists with different fingerprint → recreate entry
     * - If device doesn't exist → create new entry
     *
     * @param deviceId The device ID (cache key)
     * @param device The device entity to cache
     * @param fingerprint The device fingerprint for security validation
     */
    void put(Long deviceId, Device device, String fingerprint);

    /**
     * Removes a specific device from cache.
     *
     * @param deviceId The device ID to remove
     * @return true if device was present and removed, false otherwise
     */
    boolean remove(Long deviceId);

    /**
     * Removes all expired entries from cache.
     *
     * @return Number of entries removed
     */
    int cleanExpired();

    /**
     * Gets comprehensive cache statistics for monitoring.
     *
     * @return Map containing statistics like size, memory usage, hit rates, etc.
     */
    Map<String, Object> getStats();

    /**
     * Gets current cache size (number of entries).
     *
     * @return Current number of cached devices
     */
    int size();

    /**
     * Checks if cache is near capacity and needs attention.
     *
     * @return true if cache fill percentage > configured threshold
     */
    boolean isNearCapacity();

    /**
     * Manually triggers cache maintenance operations.
     * Useful for admin operations or scheduled cleanup.
     */
    void performMaintenance();
}