package be.steby.CoreProject.bll.domains.user.cache;

import be.steby.CoreProject.bll.domains.user.cache.models.CachedUserInfo;
import be.steby.CoreProject.dl.entities.User;

import java.util.Map;
import java.util.Optional;

/**
 * Cache abstraction for user security operations.
 *
 * This interface provides a clean abstraction over the user caching mechanism,
 * allowing seamless switching between implementations (In-Memory, Redis, etc.)
 * while maintaining the same security and performance characteristics.
 *
 * Implementations must ensure:
 * - Thread safety for concurrent access
 * - Security validation (user status and permissions)
 * - Memory management (size limits, eviction)
 * - Performance monitoring and statistics
 * - Automatic invalidation on security-critical changes
 */
public interface UserCacheService {

    /**
     * Retrieves a user from cache if present, valid, and active.
     *
     * @param username The username to look up
     * @return Optional containing the cached user info, or empty if not found/expired/inactive
     */
    Optional<CachedUserInfo> get(String username);

    /**
     * Retrieves a user by ID from cache if present, valid, and active.
     *
     * @param userId The user ID to look up
     * @return Optional containing the cached user info, or empty if not found/expired/inactive
     */
    Optional<CachedUserInfo> getById(Long userId);

    /**
     * Stores or updates a user in cache with intelligent update strategy.
     *
     * - If user exists and is same version → update in-place
     * - If user exists but different version → recreate entry
     * - If user doesn't exist → create new entry
     *
     * @param user The user entity to cache
     */
    void put(User user);

    /**
     * Removes a specific user from cache by username.
     *
     * @param username The username to remove
     * @return true if user was present and removed, false otherwise
     */
    boolean remove(String username);

    /**
     * Removes a specific user from cache by user ID.
     *
     * @param userId The user ID to remove
     * @return true if user was present and removed, false otherwise
     */
    boolean removeById(Long userId);

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
     * @return Current number of cached users
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

    /**
     * Invalidates all users with a specific role.
     * Used when role permissions change globally.
     *
     * @param role The role to invalidate
     * @return Number of users invalidated
     */
    int invalidateByRole(String role);
}