package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * Authentication domain actions
 */
public enum AuthAction implements ActionLogType {
    LOGIN("User login"),
    LOGIN_FAILED("Failed login attempt"),
    LOGOUT("User logout"),
    ACCOUNT_LOCKED("Account locked due to security"),
    SESSION_EXPIRED("User session expired"),
    TWO_FACTOR_SUCCESS("Two-factor authentication success"),
    TWO_FACTOR_FAILED("Two-factor authentication failed"),
    SECURITY_QUESTION_ANSWERED("Security question answered"),
    REMEMBER_ME_TOKEN_CREATED("Remember me token created"),
    REMEMBER_ME_TOKEN_USED("Remember me token used");

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
}
