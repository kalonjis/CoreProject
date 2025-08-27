package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;

/**
 * Device domain actions with specialized business logic.
 */
public enum DeviceAction implements ActionLogType {
    DEVICE_REGISTERED("Device registered"),
    DEVICE_UNREGISTERED("Device unregistered"),
    DEVICE_VERIFIED("Device verified"),
    DEVICE_TRUSTED("Device marked as trusted"),
    DEVICE_UNTRUSTED("Device marked as untrusted"),
    DEVICE_BLOCKED("Device blocked"),
    DEVICE_UNBLOCKED("Device unblocked"),
    DEVICE_FINGERPRINT_CHANGED("Device fingerprint changed"),
    DEVICE_LIMIT_EXCEEDED("Device limit exceeded"),
    SUSPICIOUS_DEVICE_DETECTED("Suspicious device detected"),
    NEW_DEVICE_LOGIN("Login from new device"),
    DEVICE_LOCATION_CHANGED("Device location changed"),
    DEVICE_OS_UPDATED("Device OS updated"),
    DEVICE_BROWSER_CHANGED("Device browser changed"),
    DEVICE_COMPLIANCE_CHECK("Device compliance check"),
    DEVICE_SECURITY_POLICY_APPLIED("Device security policy applied"),
    MOBILE_DEVICE_ENROLLED("Mobile device enrolled"),
    MOBILE_DEVICE_WIPED("Mobile device remotely wiped");

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
            case SUSPICIOUS_DEVICE_DETECTED, DEVICE_FINGERPRINT_CHANGED -> 4;
            case NEW_DEVICE_LOGIN, DEVICE_LIMIT_EXCEEDED, DEVICE_BLOCKED -> 3;
            case DEVICE_REGISTERED, DEVICE_LOCATION_CHANGED -> 2;
            default -> 1;
        };
    }

    public String processDeviceAction(ActivityLog log) {
        String userName = log.getUser() != null ? log.getUser().getUsername() : "unknown";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            case DEVICE_REGISTERED -> log.isSuccessful() ?
                    "📱 New device registered for user: " + userName + " - " + details :
                    "❌ Failed to register device for user: " + userName;
            case NEW_DEVICE_LOGIN -> "🔍 Login from new device for user: " + userName + " - " + details;
            case SUSPICIOUS_DEVICE_DETECTED -> "⚠️ Suspicious device detected for user: " + userName + " - " + details;
            case DEVICE_BLOCKED -> log.isSuccessful() ?
                    "🚫 Device blocked for user: " + userName + " - " + details :
                    "❌ Failed to block device for user: " + userName;
            case MOBILE_DEVICE_WIPED -> log.isSuccessful() ?
                    "🧹 Mobile device wiped for user: " + userName :
                    "❌ Failed to wipe mobile device for user: " + userName;
            default -> log.isSuccessful() ?
                    "📱 " + this.getDescription() + " for user: " + userName :
                    "❌ Failed: " + this.getDescription() + " for user: " + userName;
        };
    }

    public void enrichDeviceLog(ActivityLog log) {
        log.setRiskLevel(this.getDefaultRiskLevel());
        log.setComplianceCategory("DEVICE");

        // High-risk device events trigger alerts
        if (this.getDefaultRiskLevel() >= 3) {
            log.setTriggeredAlert(true);
        }

        // Add device-specific metadata if not already present
        if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
            log.setMetadata("{}");
        }
    }
}