package be.steby.CoreProject.bll.domains.auth.events;

/**
 * Event published when a login attempt is blocked due to IP restrictions
 * Important for security monitoring - tracking blocked IPs and attack patterns
 */
public record IpBlockedLoginAttemptEvent(
        String attemptedUsername,  // May be null or non-existent user
        String blockedIpAddress,   // The blocked IP
        String blockReason,        // "IP blocked", "Combined limit reached", etc.
        int attemptCount,          // Number of attempts that triggered the block
        String userAgent          // For additional context (may be null)
) {}