package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

import java.time.Duration;
import java.util.List;

/**
 * Authentication domain actions with specialized business logic.
 * Contains all authentication-related actions and their specific behaviors.
 */
public enum AuthAction implements ActionLogType {

    // ================== AUTHENTICATION ACTIONS ==================

    LOGIN("User login successful"),
    LOGOUT("User logout"),
    LOGIN_FAILED("Failed login attempt"),
    TOKEN_REFRESH("Authentication token refresh"),
    TWO_FA_SETUP("Two-factor authentication setup"),
    TWO_FA_VERIFICATION("Two-factor authentication verification"),
    TWO_FA_DISABLED("Two-factor authentication disabled"),
    PASSWORD_LOGIN("Password-based login"),
    SSO_LOGIN("Single Sign-On login"),
    API_KEY_AUTHENTICATION("API key authentication"),
    SESSION_EXPIRED("User session expired"),
    SESSION_TERMINATED("User session terminated"),
    CONCURRENT_SESSION_LIMIT("Concurrent session limit reached"),
    ACCOUNT_LOCKED_ATTEMPTS("Account locked due to failed attempts"),
    SECURITY_CHALLENGE("Security challenge issued"),
    SECURITY_CHALLENGE_PASSED("Security challenge passed"),
    SECURITY_CHALLENGE_FAILED("Security challenge failed");

    private final String description;

    AuthAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "AUTH";
    }

    // ================== DOMAIN-SPECIFIC PROCESSING ==================

    /**
     * Authentication-specific action processing with detailed logic
     */
    public String processAuthAction(ActivityLog log) {
        String userName = getUserName(log);

        return switch (this) {
            case LOGIN -> {
                if (!log.isSuccessful()) {
                    yield "❌ Login failed for user: " + userName;
                }
                String deviceInfo = getDeviceInfo(log);
                String locationInfo = getLocationInfo(log);
                yield String.format("✅ Successful login for user: %s%s%s",
                        userName, deviceInfo, locationInfo);
            }

            case LOGOUT -> "👋 User " + userName + " logged out successfully";

            case LOGIN_FAILED -> {
                String reason = log.getFailureReason() != null ?
                        " (Reason: " + log.getFailureReason() + ")" : "";
                String attempts = getFailedAttemptsInfo(log);
                yield String.format("🚨 Failed login attempt%s for user: %s%s",
                        attempts, userName, reason);
            }

            case TOKEN_REFRESH -> {
                if (log.isSuccessful()) {
                    yield "🔄 Token successfully refreshed for user: " + userName;
                } else {
                    yield "❌ Token refresh failed for user: " + userName;
                }
            }

            case TWO_FA_SETUP -> {
                if (log.isSuccessful()) {
                    yield "🛡️ Two-factor authentication configured for user: " + userName;
                } else {
                    yield "❌ Two-factor authentication setup failed for user: " + userName;
                }
            }

            case TWO_FA_VERIFICATION -> {
                if (log.isSuccessful()) {
                    yield "✅ Two-factor authentication verified for user: " + userName;
                } else {
                    yield "❌ Two-factor authentication verification failed for user: " + userName;
                }
            }

            case TWO_FA_DISABLED -> "⚠️ Two-factor authentication disabled for user: " + userName;

            case PASSWORD_LOGIN -> {
                if (log.isSuccessful()) {
                    yield "🔐 Password authentication successful for user: " + userName;
                } else {
                    yield "❌ Password authentication failed for user: " + userName;
                }
            }

            case SSO_LOGIN -> {
                if (log.isSuccessful()) {
                    yield "🎫 SSO authentication successful for user: " + userName;
                } else {
                    yield "❌ SSO authentication failed for user: " + userName;
                }
            }

            case API_KEY_AUTHENTICATION -> {
                if (log.isSuccessful()) {
                    yield "🔑 API key authentication successful for user: " + userName;
                } else {
                    yield "❌ API key authentication failed for user: " + userName;
                }
            }

            case SESSION_EXPIRED -> "⏰ Session expired for user: " + userName;

            case SESSION_TERMINATED -> "🚫 Session terminated for user: " + userName;

            case CONCURRENT_SESSION_LIMIT -> "⚠️ Concurrent session limit reached for user: " + userName;

            case ACCOUNT_LOCKED_ATTEMPTS -> "🔒 Account locked due to failed attempts for user: " + userName;

            case SECURITY_CHALLENGE -> "🛡️ Security challenge issued for user: " + userName;

            case SECURITY_CHALLENGE_PASSED -> "✅ Security challenge passed for user: " + userName;

            case SECURITY_CHALLENGE_FAILED -> "❌ Security challenge failed for user: " + userName;
        };
    }

    // ================== DOMAIN-SPECIFIC ENRICHMENT ==================

    /**
     * Authentication-specific log enrichment
     */
    public void enrichAuthLog(ActivityLog log) {
        // Set default risk levels based on action type
        switch (this) {
            case LOGIN_FAILED, SECURITY_CHALLENGE_FAILED -> {
                log.setRiskLevel(2); // Medium risk

                // Check for brute force patterns
                if (isBruteForceAttempt(log)) {
                    log.setRiskLevel(4); // Critical risk
                    log.setTriggeredAlert(true);
                    log.setComplianceCategory("SECURITY_INCIDENT");
                }

                // Check for suspicious patterns
                if (isSuspiciousActivity(log)) {
                    log.setRiskLevel(3); // High risk
                    log.setTriggeredAlert(true);
                }
            }

            case LOGIN, PASSWORD_LOGIN, SSO_LOGIN -> {
                if (log.isSuccessful()) {
                    // Check for new location or device
                    if (isNewLocation(log)) {
                        log.setRiskLevel(1); // Low risk
                        log.setActionDetails("Login from new location detected");
                    }

                    if (isNewDevice(log)) {
                        log.setRiskLevel(1); // Low risk
                        log.setActionDetails("Login from new device detected");
                    }

                    // Normal successful login
                    if (log.getRiskLevel() == null) {
                        log.setRiskLevel(0); // No risk
                    }
                } else {
                    log.setRiskLevel(2); // Medium risk for failed login
                }
            }

            case TWO_FA_SETUP, TWO_FA_VERIFICATION -> {
                if (log.isSuccessful()) {
                    log.setRiskLevel(0); // Security-enhancing action
                    log.setActionDetails("Security enhancement action completed");
                } else {
                    log.setRiskLevel(2); // Medium risk for failed 2FA
                }
            }

            case TWO_FA_DISABLED -> {
                log.setRiskLevel(2); // Medium risk - security downgrade
                log.setActionDetails("Security downgrade: 2FA disabled");
                log.setRequiresCompliance(true);
                log.setComplianceCategory("SECURITY_CHANGE");
            }

            case ACCOUNT_LOCKED_ATTEMPTS -> {
                log.setRiskLevel(3); // High risk
                log.setTriggeredAlert(true);
                log.setRequiresCompliance(true);
                log.setComplianceCategory("SECURITY_INCIDENT");
            }

            case CONCURRENT_SESSION_LIMIT -> {
                log.setRiskLevel(2); // Medium risk
                log.setActionDetails("Multiple concurrent sessions detected");
            }

            case API_KEY_AUTHENTICATION -> {
                if (log.isSuccessful()) {
                    log.setRiskLevel(1); // Low risk - automated access
                } else {
                    log.setRiskLevel(2); // Medium risk - failed API access
                }
                log.setRequiresCompliance(true);
                log.setComplianceCategory("API_ACCESS");
            }
        }

        // Set processing time based on action complexity
        if (log.getProcessingTimeMs() == null) {
            log.setProcessingTimeMs(getExpectedDuration().toMillis());
        }
    }

    // ================== DOMAIN-SPECIFIC VALIDATION ==================

    /**
     * Authentication-specific validation
     */
    public boolean isValidForAuth(ActivityLog log, User user) {
        return switch (this) {
            // These actions can happen without an authenticated user
            case LOGIN, LOGIN_FAILED, PASSWORD_LOGIN, SSO_LOGIN, API_KEY_AUTHENTICATION ->
                    validateBasicRequirements(log);

            // These actions require an authenticated user
            case LOGOUT, TOKEN_REFRESH, TWO_FA_SETUP, TWO_FA_VERIFICATION,
                 TWO_FA_DISABLED, SESSION_EXPIRED, SESSION_TERMINATED ->
                    validateBasicRequirements(log) && user != null && user.isActive();

            // Security actions require additional validation
            case SECURITY_CHALLENGE, SECURITY_CHALLENGE_PASSED, SECURITY_CHALLENGE_FAILED ->
                    validateBasicRequirements(log) && user != null;

            // Account status actions
            case CONCURRENT_SESSION_LIMIT, ACCOUNT_LOCKED_ATTEMPTS ->
                    validateBasicRequirements(log) && user != null;
        };
    }

    // ================== ACTION-SPECIFIC PROPERTIES ==================

    /**
     * Expected duration for each authentication action
     */
    @Override
    public Duration getExpectedDuration() {
        return switch (this) {
            case LOGIN, PASSWORD_LOGIN, SSO_LOGIN -> Duration.ofSeconds(3);
            case LOGOUT -> Duration.ofSeconds(1);
            case LOGIN_FAILED -> Duration.ofSeconds(2);
            case TOKEN_REFRESH -> Duration.ofSeconds(1);
            case TWO_FA_SETUP -> Duration.ofMinutes(2);
            case TWO_FA_VERIFICATION -> Duration.ofSeconds(10);
            case TWO_FA_DISABLED -> Duration.ofSeconds(5);
            case API_KEY_AUTHENTICATION -> Duration.ofSeconds(1);
            case SECURITY_CHALLENGE -> Duration.ofSeconds(5);
            case SECURITY_CHALLENGE_PASSED, SECURITY_CHALLENGE_FAILED -> Duration.ofSeconds(3);
            default -> Duration.ofSeconds(5);
        };
    }

    /**
     * Default risk level for each action
     */
    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            case TWO_FA_SETUP, TWO_FA_VERIFICATION -> 0; // Security-enhancing
            case LOGIN, LOGOUT, TOKEN_REFRESH -> 0; // Normal operations
            case LOGIN_FAILED, TWO_FA_DISABLED -> 2; // Medium risk
            case ACCOUNT_LOCKED_ATTEMPTS -> 3; // High risk
            case SECURITY_CHALLENGE_FAILED -> 4; // Critical
            default -> 1; // Low risk default
        };
    }

    /**
     * UI color for frontend display
     */
    @Override
    public String getUIColor() {
        return switch (this) {
            case LOGIN, PASSWORD_LOGIN, SSO_LOGIN, TWO_FA_SETUP,
                 TWO_FA_VERIFICATION, SECURITY_CHALLENGE_PASSED -> "#28a745"; // Green
            case LOGOUT, SESSION_EXPIRED, SESSION_TERMINATED -> "#6c757d"; // Gray
            case LOGIN_FAILED, TWO_FA_DISABLED, SECURITY_CHALLENGE_FAILED -> "#dc3545"; // Red
            case TOKEN_REFRESH, API_KEY_AUTHENTICATION -> "#007bff"; // Blue
            case ACCOUNT_LOCKED_ATTEMPTS, CONCURRENT_SESSION_LIMIT -> "#fd7e14"; // Orange
            case SECURITY_CHALLENGE -> "#ffc107"; // Yellow
            default -> "#6c757d"; // Default gray
        };
    }

    /**
     * Icon class for frontend display
     */
    @Override
    public String getIconClass() {
        return switch (this) {
            case LOGIN, PASSWORD_LOGIN -> "fas fa-sign-in-alt";
            case LOGOUT -> "fas fa-sign-out-alt";
            case LOGIN_FAILED -> "fas fa-times-circle";
            case TOKEN_REFRESH -> "fas fa-sync";
            case TWO_FA_SETUP, TWO_FA_VERIFICATION -> "fas fa-shield-alt";
            case TWO_FA_DISABLED -> "fas fa-shield-alt text-warning";
            case SSO_LOGIN -> "fas fa-id-card";
            case API_KEY_AUTHENTICATION -> "fas fa-key";
            case SESSION_EXPIRED -> "fas fa-clock";
            case SESSION_TERMINATED -> "fas fa-ban";
            case ACCOUNT_LOCKED_ATTEMPTS -> "fas fa-lock";
            case SECURITY_CHALLENGE -> "fas fa-question-circle";
            case SECURITY_CHALLENGE_PASSED -> "fas fa-check-circle";
            case SECURITY_CHALLENGE_FAILED -> "fas fa-exclamation-triangle";
            default -> "fas fa-circle";
        };
    }

    /**
     * Whether this action requires compliance logging
     */
    @Override
    public boolean requiresComplianceLogging() {
        return switch (this) {
            case TWO_FA_DISABLED, ACCOUNT_LOCKED_ATTEMPTS,
                 API_KEY_AUTHENTICATION, SECURITY_CHALLENGE -> true;
            default -> false;
        };
    }

    // ================== AUTH-SPECIFIC BUSINESS METHODS ==================

    /**
     * Check if this is a login-type action
     */
    public boolean isLoginAction() {
        return switch (this) {
            case LOGIN, PASSWORD_LOGIN, SSO_LOGIN, API_KEY_AUTHENTICATION -> true;
            default -> false;
        };
    }

    /**
     * Check if this is a logout-type action
     */
    public boolean isLogoutAction() {
        return switch (this) {
            case LOGOUT, SESSION_EXPIRED, SESSION_TERMINATED -> true;
            default -> false;
        };
    }

    /**
     * Check if this is a security-related action
     */
    public boolean isSecurityAction() {
        return switch (this) {
            case TWO_FA_SETUP, TWO_FA_VERIFICATION, TWO_FA_DISABLED,
                 SECURITY_CHALLENGE, SECURITY_CHALLENGE_PASSED,
                 SECURITY_CHALLENGE_FAILED, ACCOUNT_LOCKED_ATTEMPTS -> true;
            default -> false;
        };
    }

    /**
     * Check if this action requires two-factor authentication
     */
    public boolean requiresTwoFactor() {
        return switch (this) {
            case LOGIN, PASSWORD_LOGIN -> true;
            default -> false;
        };
    }

    /**
     * Check if this action can trigger account lockout
     */
    public boolean canTriggerLockout() {
        return switch (this) {
            case LOGIN_FAILED, SECURITY_CHALLENGE_FAILED, TWO_FA_VERIFICATION -> true;
            default -> false;
        };
    }

    /**
     * Get the maximum allowed failures for this action before triggering security measures
     */
    public int getMaxAllowedFailures() {
        return switch (this) {
            case LOGIN_FAILED, PASSWORD_LOGIN -> 5;
            case TWO_FA_VERIFICATION -> 3;
            case SECURITY_CHALLENGE_FAILED -> 2;
            default -> 10;
        };
    }

    // ================== PRIVATE HELPER METHODS ==================

    private String getUserName(ActivityLog log) {
        return log.getUser() != null ? log.getUser().getUsername() : "unknown";
    }

    private String getDeviceInfo(ActivityLog log) {
        if (log.getDevice() != null) {
            return " from device: " + log.getDevice().getName();
        }
        return "";
    }

    private String getLocationInfo(ActivityLog log) {
        if (log.getLocation() != null && !log.getLocation().trim().isEmpty()) {
            return " (Location: " + log.getLocation() + ")";
        }
        return "";
    }

    private String getFailedAttemptsInfo(ActivityLog log) {
        // This would typically query recent failed attempts
        // For now, return empty string - implement based on your business logic
        return "";
    }

    private boolean isBruteForceAttempt(ActivityLog log) {
        // Implement brute force detection logic
        // Check recent failed attempts from same IP/user within time window
        return false; // Placeholder
    }

    private boolean isSuspiciousActivity(ActivityLog log) {
        // Implement suspicious activity detection
        // Check for unusual patterns, locations, times, etc.
        return false; // Placeholder
    }

    private boolean isNewLocation(ActivityLog log) {
        // Check if location is different from user's usual locations
        return false; // Placeholder
    }

    private boolean isNewDevice(ActivityLog log) {
        // Check if device is new for this user
        return false; // Placeholder
    }

    // ================== STATIC UTILITY METHODS FOR AUTH DOMAIN ==================

    /**
     * Get all failed login action types
     */
    public static List<AuthAction> getFailedLoginActions() {
        return List.of(LOGIN_FAILED, SECURITY_CHALLENGE_FAILED, TWO_FA_VERIFICATION);
    }

    /**
     * Get all successful login action types
     */
    public static List<AuthAction> getSuccessfulLoginActions() {
        return List.of(LOGIN, PASSWORD_LOGIN, SSO_LOGIN, API_KEY_AUTHENTICATION);
    }

    /**
     * Get all security-related action types
     */
    public static List<AuthAction> getSecurityActions() {
        return List.of(TWO_FA_SETUP, TWO_FA_VERIFICATION, TWO_FA_DISABLED,
                SECURITY_CHALLENGE, SECURITY_CHALLENGE_PASSED,
                SECURITY_CHALLENGE_FAILED, ACCOUNT_LOCKED_ATTEMPTS);
    }

    /**
     * Get all session-related action types
     */
    public static List<AuthAction> getSessionActions() {
        return List.of(LOGIN, LOGOUT, SESSION_EXPIRED, SESSION_TERMINATED,
                CONCURRENT_SESSION_LIMIT, TOKEN_REFRESH);
    }
}