package be.steby.CoreProject.bll.domains.user.cache.impl;

import be.steby.CoreProject.bll.domains.user.cache.UserCacheService;
import be.steby.CoreProject.bll.domains.user.cache.models.CachedUserInfo;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-Memory implementation of UserCacheService using ConcurrentHashMap.
 *
 * This implementation provides:
 * - Thread-safe concurrent access
 * - LRU eviction strategy
 * - Smart cache updates (in-place when possible)
 * - Memory usage monitoring
 * - Automatic cleanup of expired entries
 * - Dual-key access (username and userId)
 * - Role-based invalidation for permission changes
 *
 * Activated when cache.user.type=memory (default)
 */
@Service
@ConditionalOnProperty(name = "cache.user.type", havingValue = "memory", matchIfMissing = true)
@Slf4j
public class InMemoryUserCacheService implements UserCacheService {

    // Primary cache: username -> CachedUserInfo
    private final Map<String, CachedUserInfo> cacheByUsername = new ConcurrentHashMap<>();

    // Secondary index: userId -> username (for ID-based lookups)
    private final Map<Long, String> userIdToUsernameIndex = new ConcurrentHashMap<>();

    @Value("${cache.user.expiration:600000}") // 10 minutes default (longer than device)
    private long cacheExpirationMs;

    @Value("${cache.user.max-size:5000}") // 5k users max
    private int maxCacheSize;

    @Value("${cache.user.eviction-percentage:15}") // Evict 15% when full
    private int evictionPercentage;

    @Value("${cache.monitoring.alert-threshold:80}") // Alert at 80% full
    private int alertThreshold;

    @Value("${cache.user.monitoring.log-operations:false}") // Detailed logging
    private boolean logAllOperations;

    // Statistics tracking
    private volatile long hitCount = 0;
    private volatile long missCount = 0;
    private volatile long evictionCount = 0;

    @Override
    public Optional<CachedUserInfo> get(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        CachedUserInfo cached = cacheByUsername.get(username);

        if (cached != null) {
            if (cached.isExpired()) {
                removeInternal(username, cached.userId());
                missCount++;
                return Optional.empty();
            }

            // Security validation - user must be enabled for authentication
            if (!cached.isValidForAuthentication()) {
                if (logAllOperations) {
                    log.debug("User {} cache hit but invalid for authentication - expired: {}, enabled: {}",
                            username, cached.isExpired(), cached.isEnabled());
                }
                missCount++;
                return Optional.empty();
            }

            hitCount++;

            if (logAllOperations) {
                log.debug("User {} retrieved from cache - roles: {}", username, cached.getAllRoles());
            }

            return Optional.of(cached);
        }

        missCount++;
        return Optional.empty();
    }

    @Override
    public Optional<CachedUserInfo> getById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String username = userIdToUsernameIndex.get(userId);
        if (username == null) {
            missCount++;
            return Optional.empty();
        }

        return get(username); // Reuse username-based logic
    }

    @Override
    public void put(User user) {
        if (user == null || user.getUsername() == null) {
            log.warn("Attempted to cache null user or user with null username");
            return;
        }

        String username = user.getUsername();
        Long userId = user.getId();

        // ✅ Simple: always create new entry (no in-place updates with record)
        putWithSizeCheck(username, userId, new CachedUserInfo(user, cacheExpirationMs));

        if (logAllOperations) {
            log.debug("User {} cache entry created/updated - roles: {}", username, user.getUserRoles());
        }
    }

    @Override
    public boolean remove(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        CachedUserInfo removed = cacheByUsername.remove(username);
        if (removed != null) {
            userIdToUsernameIndex.remove(removed.userId());
            if (logAllOperations) {
                log.debug("User {} manually removed from cache", username);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removeById(Long userId) {
        if (userId == null) {
            return false;
        }

        String username = userIdToUsernameIndex.get(userId);
        if (username != null) {
            return remove(username);
        }
        return false;
    }

    @Override
    public int cleanExpired() {
        int removedCount = 0;
        Iterator<Map.Entry<String, CachedUserInfo>> iterator = cacheByUsername.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, CachedUserInfo> entry = iterator.next();
            CachedUserInfo cached = entry.getValue();

            if (cached.isExpired()) {
                iterator.remove();
                userIdToUsernameIndex.remove(cached.userId());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.debug("Cleaned {} expired user cache entries", removedCount);
        }

        return removedCount;
    }

    @Override
    public int invalidateByRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return 0;
        }

        UserRole targetRole;
        try {
            targetRole = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role specified for cache invalidation: {}", role);
            return 0;
        }

        List<String> toRemove = cacheByUsername.values().stream()
                .filter(cached -> cached.getAllRoles().contains(targetRole))
                .map(CachedUserInfo::username)
                .collect(Collectors.toList());

        int removedCount = 0;
        for (String username : toRemove) {
            if (remove(username)) {
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Invalidated {} users with role {} from cache", removedCount, role);
        }

        return removedCount;
    }

    @Override
    public Map<String, Object> getStats() {
        long totalRequests = hitCount + missCount;
        double hitRate = totalRequests > 0 ? (double) hitCount / totalRequests * 100 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "InMemory");
        stats.put("size", cacheByUsername.size());
        stats.put("maxSize", maxCacheSize);
        stats.put("fillPercentage", Math.round((double) cacheByUsername.size() / maxCacheSize * 100));
        stats.put("hitCount", hitCount);
        stats.put("missCount", missCount);
        stats.put("hitRate", String.format("%.2f%%", hitRate));
        stats.put("evictionCount", evictionCount);
        stats.put("expiredEntries", countExpiredEntries());
        stats.put("isNearCapacity", isNearCapacity());
        stats.put("memoryEstimateMB", estimateMemoryUsageMB());

        return stats;
    }

    @Override
    public int size() {
        return cacheByUsername.size();
    }

    @Override
    public boolean isNearCapacity() {
        return (cacheByUsername.size() * 100 / maxCacheSize) >= alertThreshold;
    }

    @Override
    public void performMaintenance() {
        log.debug("Starting user cache maintenance");

        int expiredRemoved = cleanExpired();

        // Memory pressure management
        if (isNearCapacity()) {
            int evicted = performSimpleEviction();
            log.info("Cache maintenance completed - expired: {}, evicted: {}", expiredRemoved, evicted);
        } else {
            log.debug("Cache maintenance completed - expired: {}", expiredRemoved);
        }
    }

    /**
     * Scheduled cleanup every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void scheduledCleanup() {
        performMaintenance();
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    private void putWithSizeCheck(String username, Long userId, CachedUserInfo cachedUserInfo) {
        // Check if we need to make space
        if (cacheByUsername.size() >= maxCacheSize) {
            performSimpleEviction();
        }

        // Store in both maps
        cacheByUsername.put(username, cachedUserInfo);
        userIdToUsernameIndex.put(userId, username);
    }

    private void removeInternal(String username, Long userId) {
        cacheByUsername.remove(username);
        userIdToUsernameIndex.remove(userId);
    }

    private int performSimpleEviction() {
        // Simple: remove oldest entries when cache is full
        int evictCount = Math.max(1, (cacheByUsername.size() * evictionPercentage) / 100);

        List<Map.Entry<String, CachedUserInfo>> entries = new ArrayList<>(cacheByUsername.entrySet());

        // Sort by cached time (oldest first) - simple approach
        entries.sort((e1, e2) ->
                e1.getValue().cachedAt().compareTo(e2.getValue().cachedAt()));

        int evicted = 0;
        for (int i = 0; i < Math.min(evictCount, entries.size()); i++) {
            Map.Entry<String, CachedUserInfo> entry = entries.get(i);
            String username = entry.getKey();
            Long userId = entry.getValue().userId();

            removeInternal(username, userId);
            evicted++;
        }

        evictionCount += evicted;
        log.debug("Simple evicted {} user cache entries", evicted);
        return evicted;
    }

    private long countExpiredEntries() {
        return cacheByUsername.values().stream()
                .mapToLong(cached -> cached.isExpired() ? 1 : 0)
                .sum();
    }

    private double estimateMemoryUsageMB() {
        // Rough estimation: each entry ~1KB (user object + metadata)
        return (cacheByUsername.size() * 1024.0) / (1024 * 1024);
    }
}