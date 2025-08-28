package be.steby.CoreProject.dl.enums.actionLogTypes;

public enum AuthAction implements ActionLogType {

    LOGIN("User login successful"),
    LOGOUT("User logout"),
    LOGIN_FAILED("Failed login attempt"),
    TOKEN_REFRESH("Authentication token refresh"),
    TWO_FA_SETUP("Two-factor authentication enabled"),
    TWO_FA_VERIFICATION("Two-factor authentication verification"),
    ACCOUNT_LOCKED("Account locked due to failed attempts");

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

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            case LOGIN_FAILED, ACCOUNT_LOCKED -> 4;
            case TWO_FA_SETUP, TWO_FA_VERIFICATION -> 3;
            case LOGIN, LOGOUT, TOKEN_REFRESH -> 1;
        };
    }
}