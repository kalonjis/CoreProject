package be.steby.CoreProject.dl.enums.actionLogTypes;

public enum PasswordAction implements ActionLogType {

    PASSWORD_CHANGED("Password changed"),
    PASSWORD_RESET_REQUESTED("Password reset requested"),
    PASSWORD_RESET_COMPLETED("Password reset completed"),
    PASSWORD_COMPROMISED_DETECTED("Compromised password detected"),
    PASSWORD_LOCKOUT_TRIGGERED("Password lockout triggered");

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
    public int getDefaultRiskLevel() {
        return switch (this) {
            case PASSWORD_COMPROMISED_DETECTED, PASSWORD_LOCKOUT_TRIGGERED -> 5;
            case PASSWORD_RESET_COMPLETED -> 4;
            case PASSWORD_RESET_REQUESTED -> 3;
            case PASSWORD_CHANGED -> 2;
        };
    }
}