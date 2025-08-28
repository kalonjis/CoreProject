package be.steby.CoreProject.dl.enums.actionLogTypes;

public enum EmailAction implements ActionLogType {

    EMAIL_CHANGE_REQUESTED("Email change requested"),
    EMAIL_CHANGE_CONFIRMED("Email change confirmed"),
    EMAIL_VERIFICATION_SENT("Email verification sent"),
    EMAIL_VERIFIED("Email address verified");

    private final String description;

    EmailAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "EMAIL";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            case EMAIL_CHANGE_CONFIRMED -> 4;
            case EMAIL_CHANGE_REQUESTED -> 3;
            case EMAIL_VERIFICATION_SENT, EMAIL_VERIFIED -> 2;
        };
    }
}