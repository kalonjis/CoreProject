package be.steby.CoreProject.dl.enums;

/**
 * Enumeration defining the different types of user attempts that can be tracked and rate limited.
 *
 * <p>This enum is used by the attempt tracking system to categorize and separately manage
 * rate limiting for different types of operations. Each attempt type can have its own
 * configuration for maximum attempts and lockout duration.</p>
 *
 * <h4>Usage:</h4>
 * <p>Each attempt type corresponds to a specific service that extends BaseAttemptTrackerServiceImpl:</p>
 * <ul>
 *   <li>{@code ACCOUNT_CONFIRMATION} - Account activation attempts</li>
 *   <li>{@code ACCOUNT_DEACTIVATION} - Account deactivation requests</li>
 *   <li>{@code ACCOUNT_REACTIVATION} - Account reactivation requests</li>
 *   <li>{@code DEVICE_CONFIRMATION} - Device verification attempts</li>
 *   <li>{@code EMAIL_CONFIRMATION} - Email verification attempts</li>
 *   <li>{@code EMAIL_PASSWORD_RESET} - Email-based password reset attempts</li>
 *   <li>{@code SMS_PASSWORD_RESET} - SMS-based password reset attempts</li>
 *   <li>{@code EMAIL_2FA_ACTIVATION} - Email-based 2FA activation attempts</li>
 * </ul>
 *
 * <p>The separation allows for fine-grained rate limiting policies. For example,
 * SMS password resets might have stricter limits due to cost considerations,
 * while email confirmations might be more lenient.</p>
 */
public enum AttemptType {

    /**
     * Account confirmation attempts during user registration process.
     */
    ACCOUNT_CONFIRMATION,

    /**
     * Account deactivation requests by users.
     */
    ACCOUNT_DEACTIVATION,

    /**
     * Account reactivation requests for suspended accounts.
     */
    ACCOUNT_REACTIVATION,

    /**
     * Device confirmation attempts for new device verification.
     */
    DEVICE_CONFIRMATION,

    /**
     * Email confirmation attempts for email address verification.
     */
    EMAIL_CONFIRMATION,

    /**
     * Password reset attempts via email (traditional flow).
     *
     * <p><strong>Note:</strong> Maintained as EMAIL_PASSWORD_RESET for backward compatibility
     * with existing database records.</p>
     */
    EMAIL_PASSWORD_RESET,

    /**
     * SMS-based password reset attempts.
     *
     * <p>This type specifically tracks attempts to request SMS verification codes
     * for password reset operations. Due to SMS costs and potential for abuse,
     * this type typically has stricter rate limiting than email-based resets.</p>
     */
    SMS_PASSWORD_RESET,

    /**
     * Email-based two-factor authentication activation attempts.
     *
     * <p>This type tracks attempts to initiate email 2FA activation, including
     * sending verification emails for 2FA setup. Rate limiting prevents spam
     * and abuse of email services while allowing legitimate activation attempts.</p>
     *
     * <p><strong>Rate Limiting Rationale:</strong> Email 2FA activation typically
     * has moderate rate limiting (e.g., 5 attempts per 15 minutes) to balance
     * user experience with abuse prevention.</p>
     */
    EMAIL_2FA_ACTIVATION,

    /**
     * Email-based two-factor authentication verification attempts.
     *
     * <p>This type tracks attempts to verify email 2FA codes during the activation
     * process. Rate limiting prevents brute force attacks on 6-digit verification
     * codes while allowing legitimate verification attempts.</p>
     *
     * <p><strong>Rate Limiting Rationale:</strong> Email 2FA verification typically
     * has stricter rate limiting (e.g., 10 attempts per 5 minutes) due to the
     * limited keyspace of 6-digit codes (1 million combinations).</p>
     */
    EMAIL_2FA_VERIFICATION,

    /**
     * TOTP-based two-factor authentication activation attempts.
     *
     * <p>This type tracks attempts to initiate TOTP 2FA activation, including
     * generating secret keys and QR codes. Rate limiting prevents abuse while
     * allowing legitimate activation attempts.</p>
     *
     * <p><strong>Rate Limiting Rationale:</strong> TOTP 2FA activation typically
     * has moderate rate limiting (e.g., 5 attempts per 15 minutes) to balance
     * user experience with abuse prevention.</p>
     */
    TOTP_2FA_ACTIVATION,

    /**
     * TOTP-based two-factor authentication verification attempts.
     *
     * <p>This type tracks attempts to verify TOTP codes during the activation
     * process. Rate limiting prevents brute force attacks on 6-digit TOTP codes
     * while allowing legitimate verification attempts.</p>
     *
     * <p><strong>Rate Limiting Rationale:</strong> TOTP 2FA verification typically
     * has stricter rate limiting (e.g., 10 attempts per 5 minutes) due to the
     * limited keyspace of 6-digit codes (1 million combinations).</p>
     */
    TOTP_2FA_VERIFICATION

}