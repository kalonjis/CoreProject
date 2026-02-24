package be.steby.CoreProject.dl.entities.tokens.enums;

/**
 * Enumeration of all token types managed by the token infrastructure.
 *
 * <p>Each value maps to a concrete {@link be.steby.CoreProject.dl.entities.tokens.BaseToken}
 * subclass and drives type-safe lookups, cleanup, and security checks throughout
 * the application.
 *
 * <p>Token type registry:
 * <ul>
 *   <li>{@link #ACCOUNT_CONFIRMATION}  — email verification on signup</li>
 *   <li>{@link #ACCOUNT_DEACTIVATION}  — reversible account deactivation confirmation</li>
 *   <li>{@link #ACCOUNT_DELETION}      — irreversible GDPR deletion confirmation</li>
 *   <li>{@link #ACCOUNT_REACTIVATION}  — account reactivation after deactivation</li>
 *   <li>{@link #DEVICE_CONFIRMATION}   — new device trust confirmation</li>
 *   <li>{@link #EMAIL_CONFIRMATION}    — email address change confirmation</li>
 *   <li>{@link #PASSWORD_RESET}        — password reset via email link</li>
 *   <li>{@link #REFRESH_TOKEN}         — JWT refresh token</li>
 *   <li>{@link #SMS_PASSWORD_RESET}    — password reset via SMS code</li>
 * </ul>
 */
public enum TokenType {

    ACCOUNT_CONFIRMATION("account_confirmation"),
    ACCOUNT_DEACTIVATION("account_deactivation"),
    ACCOUNT_DELETION("account_deletion"),
    ACCOUNT_REACTIVATION("account_reactivation"),
    DEVICE_CONFIRMATION("device_confirmation"),
    EMAIL_CONFIRMATION("email_confirmation"),
    PASSWORD_RESET("password_reset"),
    REFRESH_TOKEN("refresh_token"),
    SMS_PASSWORD_RESET("sms_password_reset");

    private final String description;

    TokenType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}