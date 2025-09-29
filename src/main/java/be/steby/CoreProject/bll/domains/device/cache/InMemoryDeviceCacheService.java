package be.steby.CoreProject.bll.domains.device.cache;

import be.steby.CoreProject.bll.domains.device.cache.models.CachedDeviceInfo;
import be.steby.CoreProject.dl.entities.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-Memory implementation of DeviceCacheService using ConcurrentHashMap.
 *
 * This implementation provides:
 * - Thread-safe concurrent access
 * - LRU eviction strategy
 * - Smart cache updates (in-place when possible)
 * - Memory usage monitoring
 * - Automatic cleanup of expired entries
 *
 * Activated when cache.device.type=memory (default)
 */
@Service
@ConditionalOnProperty(name = "cache.device.type", havingValue = "memory", matchIfMissing = true)
@Slf4j
public class InMemoryDeviceCacheService implements DeviceCacheService {

    private final Map<Long, CachedDeviceInfo> cache = new ConcurrentHashMap<>();

    @Value("${cache.device.expiration:300000}") // 5 minutes default
    private long cacheExpirationMs;

    @Value("${cache.device.max-size:10000}") // 10k devices max
    private int maxCacheSize;

    @Value("${cache.device.eviction-percentage:10}") // Evict 10% when full
    private int evictionPercentage;

    @Value("${cache.monitoring.alert-threshold:80}") // Alert at 80% full
    private int alertThreshold;

    @Override
    public Optional<CachedDeviceInfo> get(Long deviceId) {
        CachedDeviceInfo cached = cache.get(deviceId);

        if (cached != null) {
            if (cached.isExpired()) {
                cache.remove(deviceId);
                return Optional.empty();
            }

            cached.markAccessed(); // LRU tracking
            return Optional.of(cached);
        }

        return Optional.empty();
    }

    @Override
    public void put(Long deviceId, Device device, String fingerprint) {
        CachedDeviceInfo existing = cache.get(deviceId);

        if (existing != null && existing.matchesFingerprint(fingerprint)) {
            // ✅ Same fingerprint - update in-place (memory efficient)
            existing.updateDevice(device);
            log.debug("Device {} cache entry updated in-place", deviceId);
        } else {
            // ❌ Different fingerprint or no entry - create new
            putWithSizeCheck(deviceId, new CachedDeviceInfo(device, fingerprint, cacheExpirationMs));
            String action = existing != null ? "recreated (fingerprint changed)" : "created";
            log.debug("Device {} cache entry {}", deviceId, action);
        }
    }

    @Override
    public boolean remove(Long deviceId) {
        return cache.remove(deviceId) != null;
    }

    @Override
    public int cleanExpired() {
        int sizeBefore = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int sizeAfter = cache.size();
        int removed = sizeBefore - sizeAfter;

        if (removed > 0) {
            log.debug("Cleaned {} expired entries from device cache. Size: {} -> {}",
                    removed, sizeBefore, sizeAfter);
        }

        return removed;
    }

    @Override
    public Map<String, Object> getStats() {
        int totalEntries = cache.size();
        long expiredEntries = cache.values().stream()
                .mapToLong(entry -> entry.isExpired() ? 1 : 0)
                .sum();
        long validEntries = totalEntries - expiredEntries;

        // Memory calculations
        long estimatedMemoryBytes = totalEntries * 1100L; // ~1.1KB per entry
        double fillPercentage = (double) totalEntries / maxCacheSize * 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("implementation", "InMemory");
        stats.put("totalEntries", totalEntries);
        stats.put("expiredEntries", expiredEntries);
        stats.put("validEntries", validEntries);
        stats.put("maxSize", maxCacheSize);
        stats.put("fillPercentage", Math.round(fillPercentage * 100.0) / 100.0);
        stats.put("estimatedMemoryKB", estimatedMemoryBytes / 1024);
        stats.put("estimatedMemoryMB", Math.round((estimatedMemoryBytes / 1024.0 / 1024.0) * 100.0) / 100.0);
        stats.put("cacheExpirationMs", cacheExpirationMs);
        stats.put("needsCleanup", expiredEntries > 0);
        stats.put("nearCapacity", fillPercentage > alertThreshold);
        stats.put("evictionPercentage", evictionPercentage);

        return stats;
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isNearCapacity() {
        double fillPercentage = (double) cache.size() / maxCacheSize * 100;
        return fillPercentage > alertThreshold;
    }

    @Override
    public void performMaintenance() {
        int expiredRemoved = cleanExpired();

        // Check if still near capacity after cleanup
        if (isNearCapacity()) {
            int entriesToEvict = Math.max(1, (maxCacheSize * evictionPercentage) / 100);
            int evictedCount = evictOldestEntries(entriesToEvict);
            log.info("Cache maintenance: {} expired removed, {} entries evicted",
                    expiredRemoved, evictedCount);
        } else {
            log.debug("Cache maintenance: {} expired entries removed", expiredRemoved);
        }
    }

    /**
     * Adds entry to cache with size limit enforcement
     */
    private void putWithSizeCheck(Long deviceId, CachedDeviceInfo deviceInfo) {
        // Check size limit before adding
        if (cache.size() >= maxCacheSize) {
            log.warn("Cache size limit reached ({}), performing maintenance", maxCacheSize);
            performMaintenance();

            // If still full after maintenance, force eviction
            if (cache.size() >= maxCacheSize) {
                int entriesToEvict = Math.max(1, maxCacheSize / 10); // Evict at least 10%
                evictOldestEntries(entriesToEvict);
            }
        }

        cache.put(deviceId, deviceInfo);
    }

    /**
     * LRU eviction - removes least recently used entries
     */
    private int evictOldestEntries(int countToEvict) {
        List<Map.Entry<Long, CachedDeviceInfo>> sortedByLastAccess = cache.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(
                        (a, b) -> Long.compare(a.getLastAccessTime(), b.getLastAccessTime())
                ))
                .limit(countToEvict)
                .collect(Collectors.toList());

        for (Map.Entry<Long, CachedDeviceInfo> entry : sortedByLastAccess) {
            cache.remove(entry.getKey());
        }

        log.info("Evicted {} oldest cache entries to free space (LRU)", sortedByLastAccess.size());
        return sortedByLastAccess.size();
    }

    /**
     * Scheduled cleanup every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void scheduledCleanup() {
        cleanExpired();
    }

    /**
     * Scheduled statistics logging every 30 minutes
     */
    @Scheduled(fixedRate = 1800000)
    public void logCacheStats() {
        Map<String, Object> stats = getStats();

        log.info("Device Cache Stats: {} entries ({} valid, {} expired), {}% full, {} MB RAM",
                stats.get("totalEntries"),
                stats.get("validEntries"),
                stats.get("expiredEntries"),
                stats.get("fillPercentage"),
                stats.get("estimatedMemoryMB")
        );

        // Alert if cache getting full
        if ((Boolean) stats.get("nearCapacity")) {
            log.warn("⚠️  Device cache is {}% full - consider cleanup or increasing max-size",
                    stats.get("fillPercentage"));
        }
    }
}