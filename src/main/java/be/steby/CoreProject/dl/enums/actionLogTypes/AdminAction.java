package be.steby.CoreProject.dl.enums.actionLogTypes;

public enum AdminAction implements ActionLogType {

    USER_CREATED("User created by admin"),
    USER_UPDATED("User updated by admin"),
    USER_DELETED("User deleted by admin"),
    USER_SUSPENDED("User suspended by admin"),
    USER_PASSWORD_RESET("Password reset by admin"),
    ROLE_GRANTED("Role granted"),
    ROLE_REVOKED("Role revoked"),
    EMERGENCY_ACCESS_GRANTED("Emergency access granted");

    private final String description;

    AdminAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "ADMIN";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            case USER_DELETED, EMERGENCY_ACCESS_GRANTED -> 5;
            case ROLE_GRANTED, ROLE_REVOKED, USER_SUSPENDED -> 4;
            case USER_PASSWORD_RESET -> 3;
            case USER_CREATED, USER_UPDATED -> 2;
        };
    }
}