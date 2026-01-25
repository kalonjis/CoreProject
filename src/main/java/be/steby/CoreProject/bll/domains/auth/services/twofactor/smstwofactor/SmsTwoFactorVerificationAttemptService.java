
package be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor;

import be.steby.CoreProject.dl.entities.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for rate limiting SMS 2FA code verification attempts.
 *
 * Prevents brute force attacks on verification codes by limiting
 * how many verification attempts can be made within a time window.
 *
 * Configuration:
 * - Max 5 attempts per 15 minutes
 * - Attempts are tracked per user
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@Slf4j
public class SmsTwoFactorVerificationAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MINUTES = 15;

    // In-memory storage (consider Redis for production multi-instance deployment)
    private final Map<String, AttemptRecord> attemptCache = new ConcurrentHashMap<>();

    /**
     * Check if user has exceeded verification attempt limit.
     *
     * @param user The user to check
     * @return true if limit exceeded, false otherwise
     */
    public boolean hasExceededAttempts(User user) {
        String key = getKey(user);
        AttemptRecord record = attemptCache.get(key);

        if (record == null) {
            return false;
        }

        // Check if window has expired
        if (record.windowStart.plusSeconds(WINDOW_MINUTES * 60).isBefore(Instant.now())) {
            attemptCache.remove(key);
            return false;
        }

        boolean exceeded = record.count >= MAX_ATTEMPTS;
        if (exceeded) {
            log.warn("User {} exceeded SMS 2FA verification attempts ({}/{})",
                    user.getUsername(), record.count, MAX_ATTEMPTS);
        }

        return exceeded;
    }

    /**
     * Record a verification attempt for a user.
     *
     * @param user The user making the attempt
     */
    public void recordAttempt(User user) {
        String key = getKey(user);

        attemptCache.compute(key, (k, existing) -> {
            if (existing == null) {
                return new AttemptRecord(Instant.now(), 1);
            }

            // Check if window has expired
            if (existing.windowStart.plusSeconds(WINDOW_MINUTES * 60).isBefore(Instant.now())) {
                return new AttemptRecord(Instant.now(), 1);
            }

            return new AttemptRecord(existing.windowStart, existing.count + 1);
        });

        log.debug("Recorded SMS 2FA verification attempt for user: {}", user.getUsername());
    }

    /**
     * Clear attempts for a user (called on successful verification).
     *
     * @param user The user whose attempts to clear
     */
    public void clearAttempts(User user) {
        attemptCache.remove(getKey(user));
        log.debug("Cleared SMS 2FA verification attempts for user: {}", user.getUsername());
    }

    private String getKey(User user) {
        return "sms_verification:" + user.getPublicId();
    }

    private record AttemptRecord(Instant windowStart, int count) {}
}