package be.steby.CoreProject.bll.domains.auth.services.login_attempt;

import java.time.Instant;

/**
 * Service for tracking and managing failed login attempts to prevent brute force attacks.
 * Implements multiple blocking strategies: by username, by IP address, and combined.
 */
public interface LoginAttemptService {

    /**
     * Checks if login attempts are currently blocked for the given username and IP address.
     * This method combines all blocking strategies to determine if a login should be allowed.
     *
     * @param username the username attempting to login
     * @param ipAddress the IP address of the login attempt
     * @return true if login attempts should be blocked, false otherwise
     */
    boolean isBlocked(String username, String ipAddress);

    boolean isIpBlocked(String ipAddress);

    /**
     * Records a failed login attempt for the given username and IP address.
     * This updates attempt counters and may trigger blocking if limits are exceeded.
     *
     * @param username the username that failed to login
     * @param ipAddress the IP address of the failed attempt
     */
    void recordFailedAttempt(String username, String ipAddress);

    /**
     * Clears all failed attempt records for the given username and IP address.
     * This should be called after a successful login to reset counters.
     *
     * @param username the username that successfully logged in
     * @param ipAddress the IP address of the successful attempt
     */
    void clearFailedAttempts(String username, String ipAddress);

    /**
     * Gets the earliest unlock time for the given username and IP address.
     * Returns null if no blocking is in effect.
     *
     * @param username the username to check
     * @param ipAddress the IP address to check
     * @return the unlock time, or null if not blocked
     */
    Instant getUnlockTime(String username, String ipAddress);

    Instant getUnlockTimeForIp(String ipAddress);

    LoginAttemptServiceImpl.BlockReason getBlockReason(String username, String ipAddress);


    /**
     * Gets the number of failed attempts for a specific username.
     *
     * @param username the username to check
     * @return number of failed attempts
     */
    int getFailedAttemptsByUsername(String username);

    /**
     * Gets the number of failed attempts for a specific IP address.
     *
     * @param ipAddress the IP address to check
     * @return number of failed attempts
     */
    int getFailedAttemptsByIpAddress(String ipAddress);

    /**
     * Manually clears attempts for a username (admin function).
     *
     * @param username the username to clear attempts for
     */
    void clearAttemptsForUsername(String username);

    /**
     * Manually clears attempts for an IP address (admin function).
     *
     * @param ipAddress the IP address to clear attempts for
     */
    void clearAttemptsForIpAddress(String ipAddress);
}