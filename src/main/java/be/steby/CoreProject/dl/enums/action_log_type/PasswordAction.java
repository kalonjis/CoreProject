package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * Password domain actions
 * Defines all trackable actions related to password management
 */
public enum PasswordAction implements ActionLogType {
    PASSWORD_CHANGED("Password changed successfully"),
    PASSWORD_CHANGED_FAILED("Password change failed"),
    PASSWORD_RESET("Password reset successfully"),
    PASSWORD_RESET_FAILED("Password reset failed"),
    PASSWORD_RESET_REQUESTED("Password reset requested"),
    PASSWORD_RESET_TOKEN_REQUESTED("New password reset token requested"),
    PASSWORD_POLICY_VIOLATION("Password policy violation detected"),
    CURRENT_PASSWORD_INCORRECT("Current password verification failed"),
    PASSWORD_EXPIRED("Password expired - change required"),
    PASSWORD_FORCE_CHANGED("Password forcibly changed by administrator");

    private final String description;

    PasswordAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "PASSWORD";
    }

    @Override
    public String getName() {
        return this.name();
    }
}