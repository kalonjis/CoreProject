package be.steby.CoreProject.dl.enums.actionLogTypes;

public enum AccountAction implements ActionLogType {

    ACCOUNT_CREATED("Account created"),
    ACCOUNT_ACTIVATED("Account activated"),
    ACCOUNT_DEACTIVATED("Account deactivated"),
    ACCOUNT_DELETED("Account deleted"),
    TWO_FACTOR_ENABLED("Two-factor authentication enabled"),
    TWO_FACTOR_DISABLED("Two-factor authentication disabled"),
    DATA_DELETION_REQUESTED("Data deletion requested");

    private final String description;

    AccountAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "ACCOUNT";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            case ACCOUNT_DELETED, DATA_DELETION_REQUESTED -> 5;
            case TWO_FACTOR_DISABLED -> 4;
            case ACCOUNT_DEACTIVATED -> 3;
            case TWO_FACTOR_ENABLED -> 2;
            case ACCOUNT_CREATED, ACCOUNT_ACTIVATED -> 1;
        };
    }
}