package be.steby.CoreProject.dl.enums.actionLogTypes;

public enum DeviceAction implements ActionLogType {

    DEVICE_REGISTERED("Device registered"),
    DEVICE_TRUSTED("Device marked as trusted"),
    DEVICE_BLOCKED("Device blocked"),
    NEW_DEVICE_LOGIN("Login from new device"),
    SUSPICIOUS_DEVICE_DETECTED("Suspicious device detected");

    private final String description;

    DeviceAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "DEVICE";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            case SUSPICIOUS_DEVICE_DETECTED -> 5;
            case NEW_DEVICE_LOGIN, DEVICE_BLOCKED -> 4;
            case DEVICE_REGISTERED -> 2;
            case DEVICE_TRUSTED -> 1;
        };
    }
}