package be.steby.CoreProject.dl.entities.tokens.enums;

public enum TokenType {
    ACCOUNT_CONFIRMATION("account_confirmation"),
    ACCOUNT_DEACTIVATION("account_deactivation"),
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