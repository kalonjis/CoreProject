package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.bll.domains.user.cache.UserCacheService;
import be.steby.CoreProject.bll.domains.user.cache.models.CachedUserInfo;
import be.steby.CoreProject.bll.domains.user.exceptions.UsernameNotFoundAuthenticationException;
import be.steby.CoreProject.bll.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Security-focused service for user authentication with intelligent caching.
 *
 * This service focuses exclusively on AUTHENTICATION BUSINESS LOGIC:
 * - Secure user retrieval with cache validation
 * - Authentication context management
 * - Security logging and audit
 * - Integration with Spring Security context
 *
 * CACHE OPERATIONS are delegated to UserCacheService following DDD principles.
 * This separation allows for easy cache implementation switching (Memory → Redis)
 * without affecting the core authentication logic.
 *
 * IMPORTANT: This service replaces the caching logic in UserServiceImpl.getAuthenticatedUser()
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationService {

    // Repository for database operations
    private final UserRepository userRepository;

    // ✅ DDD: Cache operations delegated to dedicated service
    private final UserCacheService userCacheService;

    /**
     * Main method for secure authenticated user retrieval.
     * Used by UserService.getAuthenticatedUser() and other authentication operations.
     *
     * SECURITY FLOW:
     * 1. Extract username from Spring Security context
     * 2. Input validation for security
     * 3. Cache lookup with security validation
     * 4. Database lookup with cross-validation
     * 5. Security status verification
     * 6. Cache update
     *
     * @return User entity for the authenticated user
     * @throws UserAuthenticationStateException if no user is authenticated or user is invalid
     */
    public User getSecureAuthenticatedUser() {
        // 1. Extract username from Spring Security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication instanceof AnonymousAuthenticationToken ||
                !authentication.isAuthenticated()) {
            throw new UserAuthenticationStateException("No user authenticated in security context", 401);
        }

        String username = authentication.getName();

        // 2. Input validation for security
        if (username == null || username.trim().isEmpty()) {
            log.warn("Invalid username in security context: {}", username);
            throw new UserAuthenticationStateException("Invalid authentication state", 401);
        }

        // 3. ✅ DDD: Cache lookup delegated to cache service
        Optional<CachedUserInfo> cached = userCacheService.get(username);
        if (cached.isPresent()) {
            log.debug("Authenticated user {} retrieved from cache", username);
            return cached.get().user();
        }

        // 4. Database lookup - cache miss or invalid entry
        log.debug("Cache miss for authenticated user {} - loading from database", username);

        Optional<User> userOptional = userRepository.findByUsernameIgnoreCase(username);
        if (userOptional.isEmpty()) {
            log.error("Authenticated user {} not found in database - possible data inconsistency", username);
            throw new UserAuthenticationStateException("User not found", 401);
        }

        User user = userOptional.get();

        // 5. Security status verification
        if (!user.isEnabled()) {
            log.warn("Authenticated user {} is inactive - userId: {}", username, user.getId());
            throw new UserAuthenticationStateException("User account is inactive", 401);
        }

        // 6. ✅ DDD: Cache update delegated
        userCacheService.put(user);
        log.debug("Authenticated user {} cached successfully", username);

        return user;
    }


    /**
     * Secure user loading for Spring Security authentication with caching.
     * Used by loadUserByUsername to avoid circular dependencies.
     *
     * @param username The username to load
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundAuthenticationException if user not found
     */
    public UserDetails loadUserByUsernameWithCache(String username) {
        // 1. ✅ Tentative cache
        Optional<CachedUserInfo> cached = userCacheService.get(username);
        if (cached.isPresent()) {
            log.debug("User {} loaded from cache for Spring Security authentication", username);
            return cached.get().user();
        }

        // 2. ✅ Cache MISS → DB via repository (pas via UserService pour éviter circularité)
        log.debug("Cache miss for user {} during Spring Security authentication - loading from database", username);

            // Search by email OR username
        Optional<User> userOptional = userRepository.findByEmailOrUsername(username);

        if (userOptional.isEmpty()) {
            log.warn("Authentication failed: username '{}' not found", username);
            throw UsernameNotFoundAuthenticationException.forAuthentication(username);
        }

        User user = userOptional.get();

        // 3. ✅ Mise en cache immédiate
        userCacheService.put(user);
        log.debug("User {} cached after Spring Security authentication load", username);

        return user;
    }


    /**
     * Alternative method for user retrieval by ID with caching.
     * Used when we have userId but need full user entity.
     *
     * @param userId The user ID to retrieve
     * @return User entity if found and valid
     * @throws UserAuthenticationStateException if user not found or inactive
     */
    public User getSecureUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // ✅ DDD: Cache lookup by ID delegated
        Optional<CachedUserInfo> cached = userCacheService.getById(userId);
        if (cached.isPresent()) {
            log.debug("User {} retrieved from cache by ID", userId);
            return cached.get().user();
        }

        // Database lookup
        log.debug("Cache miss for user {} - loading from database", userId);

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new UserAuthenticationStateException("User not found: " + userId, 404);
        }

        User user = userOptional.get();

        // Security validation
        if (!user.isEnabled()) {
            throw new UserAuthenticationStateException("User account is inactive: " + userId, 401);
        }

        // Cache update
        userCacheService.put(user);
        log.debug("User {} cached successfully", userId);

        return user;
    }

    /**
     * Checks if current context has an authenticated user.
     *
     * @return true if user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * Gets username from current authentication context without full user retrieval.
     *
     * @return Optional containing username if authenticated
     */
    public Optional<String> getCurrentUsername() {
        if (!isAuthenticated()) {
            return Optional.empty();
        }

        return Optional.of(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    /**
     * ✅ DDD: Manually invalidates a specific user from cache.
     * Used for security operations or when user data changes externally.
     */
    public void invalidateUserCache(String username) {
        boolean removed = userCacheService.remove(username);
        if (removed) {
            log.info("User {} manually removed from cache", username);
        }
    }

    /**
     * ✅ DDD: Manually invalidates a specific user from cache by ID.
     */
    public void invalidateUserCacheById(Long userId) {
        boolean removed = userCacheService.removeById(userId);
        if (removed) {
            log.info("User {} manually removed from cache by ID", userId);
        }
    }

    /**
     * ✅ DDD: Gets cache statistics for monitoring.
     * Delegates to cache service for implementation details.
     */
    public Map<String, Object> getCacheStats() {
        return userCacheService.getStats();
    }

    /**
     * ✅ DDD: Forces cache cleanup.
     * Delegates to cache service for implementation.
     */
    public int cleanExpiredCache() {
        return userCacheService.cleanExpired();
    }

    /**
     * ✅ DDD: Checks if cache needs attention.
     * Business logic can react to cache capacity issues.
     */
    public boolean isCacheNearCapacity() {
        return userCacheService.isNearCapacity();
    }

    /**
     * ✅ DDD: Triggers cache maintenance.
     * Can be called by admin endpoints or scheduled tasks.
     */
    public void performCacheMaintenance() {
        userCacheService.performMaintenance();
        log.info("Cache maintenance completed via UserAuthenticationService");
    }

    /**
     * ✅ DDD: Bulk invalidation by role.
     * Useful when role permissions change globally.
     */
    public int invalidateUsersByRole(String role) {
        int count = userCacheService.invalidateByRole(role);
        log.info("Invalidated {} users with role {} from cache", count, role);
        return count;
    }
}