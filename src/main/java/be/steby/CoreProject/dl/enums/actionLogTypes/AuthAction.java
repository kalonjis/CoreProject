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
        String userName = log.getUser() != null ? log.getUser().getUsername() : "unknown";

        return switch (this) {
            case LOGIN -> log.isSuccessful() ?
                    "✅ Successful login for user: " + userName :
                    "❌ Login failed for user: " + userName;

            case LOGOUT -> "👋 User " + userName + " logged out";

            case LOGIN_FAILED -> "❌ Login failed for user: " + userName;

            case TOKEN_REFRESH -> "🔄 Token refreshed for user: " + userName;

            case TWO_FA_SETUP -> "🔐 Two-factor authentication enabled for user: " + userName;
            case TWO_FA_VERIFICATION -> log.isSuccessful() ?
                    "✅ 2FA verification successful for user: " + userName :
                    "❌ 2FA verification failed for user: " + userName;
            case TWO_FA_DISABLED -> "🔓 Two-factor authentication disabled for user: " + userName;

            case PASSWORD_LOGIN -> log.isSuccessful() ?
                    "✅ Password login successful for user: " + userName :
                    "❌ Password login failed for user: " + userName;

            case SSO_LOGIN -> log.isSuccessful() ?
                    "✅ SSO login successful for user: " + userName :
                    "❌ SSO login failed for user: " + userName;

            case API_KEY_AUTHENTICATION -> log.isSuccessful() ?
                    "🔑 API key authentication successful for user: " + userName :
                    "❌ API key authentication failed for user: " + userName;

            case SESSION_EXPIRED -> "⏰ Session expired for user: " + userName;
            case SESSION_TERMINATED -> "🚪 Session terminated for user: " + userName;

            case CONCURRENT_SESSION_LIMIT -> "⚠️ Concurrent session limit reached for user: " + userName;

            case ACCOUNT_LOCKED_ATTEMPTS -> "🔒 Account locked due to failed attempts for user: " + userName;

            case SECURITY_CHALLENGE -> "🛡️ Security challenge issued for user: " + userName;
            case SECURITY_CHALLENGE_PASSED -> "✅ Security challenge passed for user: " + userName;
            case SECURITY_CHALLENGE_FAILED -> "❌ Security challenge failed for user: " + userName;
        };
    }

    /**
     * Authentication-specific log enrichment
     */
    public void enrichAuthLog(ActivityLog log) {
        // Set default risk level based on action type
        if (log.getRiskLevel() == null) {
            log.setRiskLevel(getDefaultRiskLevel());
        }

        // Set triggered alert flag for high-risk actions
        if (log.getTriggeredAlert() == null) {
            log.setTriggeredAlert(shouldTriggerAlert());
        }
    }

    /**
     * Détermine si cette action doit déclencher une alerte
     */
    private boolean shouldTriggerAlert() {
        return switch (this) {
            case LOGIN_FAILED, ACCOUNT_LOCKED_ATTEMPTS, SECURITY_CHALLENGE_FAILED,
                 CONCURRENT_SESSION_LIMIT, TWO_FA_DISABLED -> true;
            default -> false;
        };
    }

    // ================== OVERRIDDEN METHODS FOR AUTH DOMAIN ==================

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            case LOGIN_FAILED, SECURITY_CHALLENGE_FAILED, ACCOUNT_LOCKED_ATTEMPTS -> 3;
            case TWO_FA_VERIFICATION, CONCURRENT_SESSION_LIMIT -> 2;
            default -> 1;
        };
    }



}