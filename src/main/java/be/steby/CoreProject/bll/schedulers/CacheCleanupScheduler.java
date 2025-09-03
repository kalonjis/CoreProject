package be.steby.CoreProject.bll.common.schedulers;

import be.steby.CoreProject.bll.domains.device.services.DeviceCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for cache maintenance and cleanup operations.
 * Ensures optimal performance by removing expired cache entries periodically.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheCleanupScheduler {

    private final DeviceCacheService deviceCacheService;

    /**
     * Clean expired cache entries every 30 minutes
     * This prevents memory leaks and ensures cache freshness
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void cleanExpiredCacheEntries() {
        try {
            int sizeBefore = deviceCacheService.getCacheSize();
            deviceCacheService.cleanExpiredEntries();
            int sizeAfter = deviceCacheService.getCacheSize();

            if (sizeBefore != sizeAfter) {
                log.info("Cache cleanup completed: {} entries removed, {} entries remaining",
                        sizeBefore - sizeAfter, sizeAfter);
            }
        } catch (Exception e) {
            log.error("Error during cache cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Log cache statistics every hour for monitoring
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void logCacheStatistics() {
        try {
            int cacheSize = deviceCacheService.getCacheSize();
            log.info("Device cache statistics - Current size: {} entries", cacheSize);
        } catch (Exception e) {
            log.error("Error logging cache statistics: {}", e.getMessage(), e);
        }
    }
}