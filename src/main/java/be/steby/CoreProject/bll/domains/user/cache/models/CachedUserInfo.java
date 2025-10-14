package be.steby.CoreProject.bll.domains.user.cache.models;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Cached user information record with security validation.
 *
 * Simple immutable cache entry for user data with basic expiration
 * and security validation. Keeps it simple - no complex LRU tracking
 * since cache invalidation is event-driven.
 */
@Slf4j
public record CachedUserInfo(
        User user,
        String username,
        Long userId,
        Set<UserRole> userRoles,
        UserRole highestRole,
        boolean isEnabled,
        Instant cachedAt,
        Instant expiresAt
) {

    /**
     * Creates a new cached user entry.
     */
    public CachedUserInfo(User user, long cacheExpirationMs) {
        this(
                Objects.requireNonNull(user, "User cannot be null"),
                user.getUsername(),
                user.getId(),
                Set.copyOf(user.getUserRoles()),
                user.getHighestRole(),
                user.isEnabled(),
                Instant.now(),
                Instant.now().plusMillis(cacheExpirationMs)
        );

        log.debug("User {} cached with expiration at {}", user.getUsername(),
                Instant.now().plusMillis(cacheExpirationMs));
    }

    /**
     * Checks if cached entry is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Validates if cached user is still valid for authentication.
     */
    public boolean isValidForAuthentication() {
        return !isExpired() && isEnabled && user.isEnabled();
    }

    /**
     * Gets all user roles.
     */
    public Set<UserRole> getAllRoles() {
        return userRoles;
    }

    /**
     * Returns basic cache statistics for this entry.
     * Essential for monitoring and admin operations.
     */
    public java.util.Map<String, Object> getEntryStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("username", username);
        stats.put("userId", userId);
        stats.put("roles", userRoles.stream().map(UserRole::name).collect(java.util.stream.Collectors.toSet()));
        stats.put("highestRole", highestRole.name());
        stats.put("isEnabled", isEnabled);
        stats.put("cachedAt", cachedAt);
        stats.put("expiresAt", expiresAt);
        stats.put("isExpired", isExpired());
        return stats;
    }
}