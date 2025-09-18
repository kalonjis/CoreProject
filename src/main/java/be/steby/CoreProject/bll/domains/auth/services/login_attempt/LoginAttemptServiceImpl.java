package be.steby.CoreProject.bll.domains.auth.services.login_attempt;

import be.steby.CoreProject.bll.domains.auth.events.IpBlockedLoginAttemptEvent;
import be.steby.CoreProject.dl.entities.LoginAttempt;
import be.steby.CoreProject.dl.repositories.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Implementation of LoginAttemptService that provides brute force protection
 * through multiple blocking strategies with event publishing for security monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Configuration values - can be externalized to application.yml
    @Value("${security.login-attempts.username.max-attempts:5}")
    private int maxAttemptsPerUsername;

    @Value("${security.login-attempts.username.lockout-minutes:15}")
    private int lockoutMinutesForUsername;

    @Value("${security.login-attempts.ip.max-attempts:10}")
    private int maxAttemptsPerIp;

    @Value("${security.login-attempts.ip.lockout-minutes:60}")
    private int lockoutMinutesForIp;

    @Value("${security.login-attempts.combined.max-attempts:3}")
    private int maxAttemptsForCombined;

    @Value("${security.login-attempts.combined.lockout-minutes:5}")
    private int lockoutMinutesForCombined;

    // Attempt types
    private static final String USERNAME_TYPE = "username";
    private static final String IP_TYPE = "ip";
    private static final String COMBINED_TYPE = "combined";

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String username, String ipAddress) {
        Instant now = Instant.now();

        // Check username-based blocking
        if (isBlockedByType(username, null, USERNAME_TYPE, now)) {
            log.debug("Login blocked for username: {}", username);
            return true;
        }

        // Check IP-based blocking
        if (isBlockedByType(null, ipAddress, IP_TYPE, now)) {
            log.debug("Login blocked for IP: {}", ipAddress);
            return true;
        }

        // Check combined username+IP blocking
        if (isBlockedByType(username, ipAddress, COMBINED_TYPE, now)) {
            log.debug("Login blocked for username+IP combination: {} from {}", username, ipAddress);
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void recordFailedAttempt(String username, String ipAddress) {
        Instant now = Instant.now();

        // Record attempt by username
        recordAttemptByType(username, null, USERNAME_TYPE, now,
                maxAttemptsPerUsername, lockoutMinutesForUsername);

        // Record attempt by IP
        recordAttemptByType(null, ipAddress, IP_TYPE, now,
                maxAttemptsPerIp, lockoutMinutesForIp);

        // Record combined attempt
        recordAttemptByType(username, ipAddress, COMBINED_TYPE, now,
                maxAttemptsForCombined, lockoutMinutesForCombined);

        log.info("Failed login attempt recorded for username: {} from IP: {}", username, ipAddress);
    }

    @Override
    @Transactional
    public void clearFailedAttempts(String username, String ipAddress) {
        // Clear username attempts
        clearAttemptsByType(username, null, USERNAME_TYPE);

        // Clear IP attempts
        clearAttemptsByType(null, ipAddress, IP_TYPE);

        // Clear combined attempts
        clearAttemptsByType(username, ipAddress, COMBINED_TYPE);

        log.debug("Cleared failed attempts for username: {} from IP: {}", username, ipAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public Instant getUnlockTime(String username, String ipAddress) {
        Instant now = Instant.now();
        Instant latestUnlockTime = null;

        // Check username unlock time
        Instant usernameUnlockTime = getUnlockTimeByType(username, null, USERNAME_TYPE, now);
        if (usernameUnlockTime != null) {
            latestUnlockTime = usernameUnlockTime;
        }

        // Check IP unlock time
        Instant ipUnlockTime = getUnlockTimeByType(null, ipAddress, IP_TYPE, now);
        if (ipUnlockTime != null && (latestUnlockTime == null || ipUnlockTime.isAfter(latestUnlockTime))) {
            latestUnlockTime = ipUnlockTime;
        }

        // Check combined unlock time
        Instant combinedUnlockTime = getUnlockTimeByType(username, ipAddress, COMBINED_TYPE, now);
        if (combinedUnlockTime != null && (latestUnlockTime == null || combinedUnlockTime.isAfter(latestUnlockTime))) {
            latestUnlockTime = combinedUnlockTime;
        }

        return latestUnlockTime;
    }

    @Override
    @Transactional(readOnly = true)
    public int getFailedAttemptsByUsername(String username) {
        return findAttemptByType(username, null, USERNAME_TYPE)
                .map(LoginAttempt::getAttemptCount)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public int getFailedAttemptsByIpAddress(String ipAddress) {
        return findAttemptByType(null, ipAddress, IP_TYPE)
                .map(LoginAttempt::getAttemptCount)
                .orElse(0);
    }

    @Override
    @Transactional
    public void clearAttemptsForUsername(String username) {
        clearAttemptsByType(username, null, USERNAME_TYPE);
        log.info("Manually cleared login attempts for username: {}", username);
    }

    @Override
    @Transactional
    public void clearAttemptsForIpAddress(String ipAddress) {
        clearAttemptsByType(null, ipAddress, IP_TYPE);
        log.info("Manually cleared login attempts for IP: {}", ipAddress);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private boolean isBlockedByType(String username, String ipAddress, String type, Instant now) {
        Optional<LoginAttempt> attemptOpt = findAttemptByType(username, ipAddress, type);

        if (attemptOpt.isEmpty()) {
            return false;
        }

        LoginAttempt attempt = attemptOpt.get();

        // Check if currently blocked and not expired
        return attempt.getIsBlocked() &&
                attempt.getBlockedUntil() != null &&
                now.isBefore(attempt.getBlockedUntil());
    }

    private void recordAttemptByType(String username, String ipAddress, String type, Instant now,
                                     int maxAttempts, int lockoutMinutes) {
        LoginAttempt attempt = findAttemptByType(username, ipAddress, type)
                .orElse(createNewAttempt(username, ipAddress, type));

        // Increment attempt count
        attempt.setAttemptCount(attempt.getAttemptCount() + 1);
        attempt.setLastAttemptTime(now);

        if (attempt.getFirstAttemptTime() == null) {
            attempt.setFirstAttemptTime(now);
        }

        // Check if we should block
        if (attempt.getAttemptCount() >= maxAttempts) {
            attempt.setIsBlocked(true);
            attempt.setBlockedUntil(now.plus(lockoutMinutes, ChronoUnit.MINUTES));

            // Publish security event when blocking occurs
            String blockReason = getBlockReasonFromType(type, attempt.getAttemptCount());

            eventPublisher.publishEvent(new IpBlockedLoginAttemptEvent(
                    username,                    // may be null for IP_TYPE
                    ipAddress,                   // may be null for USERNAME_TYPE
                    blockReason,
                    attempt.getAttemptCount(),
                    null                         // userAgent - can be added later if needed
            ));

            log.warn("Login attempts blocked for {} type. Username: {}, IP: {}, Attempts: {}, Unlock time: {}",
                    type, username, ipAddress, attempt.getAttemptCount(), attempt.getBlockedUntil());
        }

        loginAttemptRepository.save(attempt);
    }

    private String getBlockReasonFromType(String type, int attemptCount) {
        return switch (type) {
            case USERNAME_TYPE -> "Username blocked after " + attemptCount + " attempts";
            case IP_TYPE -> "IP address blocked after " + attemptCount + " attempts";
            case COMBINED_TYPE -> "Username+IP combination blocked after " + attemptCount + " attempts";
            default -> "Login blocked after " + attemptCount + " attempts";
        };
    }

    private Optional<LoginAttempt> findAttemptByType(String username, String ipAddress, String type) {
        return switch (type) {
            case USERNAME_TYPE -> loginAttemptRepository.findByUsernameAndAttemptType(username, type);
            case IP_TYPE -> loginAttemptRepository.findByIpAddressAndAttemptType(ipAddress, type);
            case COMBINED_TYPE -> loginAttemptRepository.findByUsernameAndIpAddressAndAttemptType(username, ipAddress, type);
            default -> Optional.empty();
        };
    }

    private LoginAttempt createNewAttempt(String username, String ipAddress, String type) {
        return new LoginAttempt(username, ipAddress, type);
    }

    private void clearAttemptsByType(String username, String ipAddress, String type) {
        findAttemptByType(username, ipAddress, type)
                .ifPresent(loginAttemptRepository::delete);
    }

    private Instant getUnlockTimeByType(String username, String ipAddress, String type, Instant now) {
        return findAttemptByType(username, ipAddress, type)
                .filter(attempt -> attempt.getIsBlocked() &&
                        attempt.getBlockedUntil() != null &&
                        now.isBefore(attempt.getBlockedUntil()))
                .map(LoginAttempt::getBlockedUntil)
                .orElse(null);
    }

    /**
     * Scheduled cleanup of expired login attempts
     * Runs every hour to clean up old blocked attempts
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredAttempts() {
        Instant now = Instant.now();
        int deletedCount = loginAttemptRepository.deleteByBlockedUntilBefore(now);

        if (deletedCount > 0) {
            log.info("Cleaned up {} expired login attempts", deletedCount);
        }
    }
}