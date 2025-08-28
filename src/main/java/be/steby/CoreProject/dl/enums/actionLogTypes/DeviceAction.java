package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;

/**
 * Device domain actions with specialized business logic.
 * Complete enum with all device-related actions for comprehensive tracking.
 */
public enum DeviceAction implements ActionLogType {

    // ================== DEVICE REGISTRATION ==================
    DEVICE_REGISTERED("Device registered"),
    DEVICE_UNREGISTERED("Device unregistered"),
    DEVICE_REMOVED("Device removed"),

    // ================== DEVICE VERIFICATION ==================
    DEVICE_VERIFICATION_REQUESTED("Device verification requested"),
    DEVICE_VERIFICATION_COMPLETED("Device verification completed"),
    DEVICE_VERIFICATION_FAILED("Device verification failed"),
    DEVICE_VERIFICATION_EXPIRED("Device verification expired"),
    DEVICE_VERIFIED("Device verified"),

    // ================== DEVICE TRUST MANAGEMENT ==================
    DEVICE_TRUSTED("Device marked as trusted"),
    DEVICE_UNTRUSTED("Device marked as untrusted"),
    DEVICE_BLOCKED("Device blocked"),
    DEVICE_UNBLOCKED("Device unblocked"),
    DEVICE_SUSPENDED("Device suspended"),
    DEVICE_REACTIVATED("Device reactivated"),

    // ================== DEVICE INFORMATION ==================
    DEVICE_INFO_UPDATED("Device information updated"),
    DEVICE_FINGERPRINT_CHANGED("Device fingerprint changed"),
    DEVICE_LOCATION_UPDATED("Device location updated"),
    UNUSUAL_DEVICE_LOCATION("Unusual device location detected"),
    DEVICE_OS_UPDATED("Device OS updated"),
    DEVICE_BROWSER_CHANGED("Device browser changed"),

    // ================== DEVICE AUTHENTICATION ==================
    DEVICE_LOGIN_SUCCESS("Device login successful"),
    DEVICE_LOGIN_FAILED("Device login failed"),
    DEVICE_AUTH_CHALLENGE("Device authentication challenge issued"),
    DEVICE_AUTH_SUCCESS("Device authentication successful"),
    DEVICE_AUTH_FAILED("Device authentication failed"),
    NEW_DEVICE_LOGIN("Login from new device"),

    // ================== DEVICE SESSIONS ==================
    DEVICE_SESSION_STARTED("Device session started"),
    DEVICE_SESSION_ENDED("Device session ended"),
    DEVICE_SESSION_TIMEOUT("Device session timeout"),
    DEVICE_LIMIT_EXCEEDED("Device limit exceeded"),

    // ================== DEVICE SECURITY ==================
    SUSPICIOUS_DEVICE_DETECTED("Suspicious device detected"),
    DEVICE_COMPROMISE_DETECTED("Device compromise detected"),
    DEVICE_QUARANTINED("Device quarantined"),
    DEVICE_QUARANTINE_RELEASED("Device quarantine released"),
    DEVICE_SECURITY_SCAN_PASSED("Device security scan passed"),
    DEVICE_SECURITY_SCAN_FAILED("Device security scan failed"),

    // ================== DEVICE COMPLIANCE ==================
    DEVICE_COMPLIANCE_CHECK("Device compliance check"),
    DEVICE_SECURITY_POLICY_APPLIED("Device security policy applied"),
    MOBILE_DEVICE_ENROLLED("Mobile device enrolled"),
    MOBILE_DEVICE_WIPED("Mobile device remotely wiped"),

    // ================== ADMIN DEVICE ACTIONS ==================
    DEVICE_FORCE_REMOVED("Device force removed by admin"),
    DEVICE_ADMIN_OVERRIDE("Device admin override applied");

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
            // Critical risk actions
            case SUSPICIOUS_DEVICE_DETECTED, DEVICE_COMPROMISE_DETECTED,
                 DEVICE_FINGERPRINT_CHANGED, MOBILE_DEVICE_WIPED -> 4;

            // High risk actions
            case NEW_DEVICE_LOGIN, DEVICE_LIMIT_EXCEEDED, DEVICE_BLOCKED,
                 DEVICE_QUARANTINED, UNUSUAL_DEVICE_LOCATION, DEVICE_FORCE_REMOVED -> 3;

            // Medium risk actions
            case DEVICE_REGISTERED, DEVICE_LOCATION_UPDATED, DEVICE_VERIFICATION_FAILED,
                 DEVICE_AUTH_FAILED, DEVICE_LOGIN_FAILED, DEVICE_SUSPENDED -> 2;

            // Low risk actions
            default -> 1;
        };
    }

    public String processDeviceAction(ActivityLog log) {
        String userName = log.getUser() != null ? log.getUser().getUsername() : "unknown";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            // Device Registration
            case DEVICE_REGISTERED -> log.isSuccessful() ?
                    "📱 New device registered for user: " + userName + " - " + details :
                    "❌ Failed to register device for user: " + userName;

            case DEVICE_UNREGISTERED, DEVICE_REMOVED -> log.isSuccessful() ?
                    "📱 Device removed for user: " + userName + " - " + details :
                    "❌ Failed to remove device for user: " + userName;

            // Device Verification
            case DEVICE_VERIFICATION_REQUESTED -> "📋 Device verification requested for user: " + userName;

            case DEVICE_VERIFICATION_COMPLETED, DEVICE_VERIFIED -> log.isSuccessful() ?
                    "✅ Device verification completed for user: " + userName :
                    "❌ Device verification failed for user: " + userName;

            case DEVICE_VERIFICATION_FAILED -> "❌ Device verification failed for user: " + userName + " - " + details;

            case DEVICE_VERIFICATION_EXPIRED -> "⏰ Device verification expired for user: " + userName;

            // Device Trust Management
            case DEVICE_TRUSTED -> "✅ Device trusted for user: " + userName + " - " + details;

            case DEVICE_UNTRUSTED -> "⚠️ Device untrusted for user: " + userName + " - " + details;

            case DEVICE_BLOCKED -> log.isSuccessful() ?
                    "🚫 Device blocked for user: " + userName + " - " + details :
                    "❌ Failed to block device for user: " + userName;

            case DEVICE_UNBLOCKED -> "✅ Device unblocked for user: " + userName;

            case DEVICE_SUSPENDED -> "⏸️ Device suspended for user: " + userName + " - " + details;

            case DEVICE_REACTIVATED -> "▶️ Device reactivated for user: " + userName;

            // Device Information
            case DEVICE_INFO_UPDATED -> "ℹ️ Device information updated for user: " + userName + " - " + details;

            case DEVICE_FINGERPRINT_CHANGED -> "🔍 Device fingerprint changed for user: " + userName + " - " + details;

            case DEVICE_LOCATION_UPDATED -> "📍 Device location updated for user: " + userName + " - " + details;

            case UNUSUAL_DEVICE_LOCATION -> "⚠️ Unusual device location for user: " + userName + " - " + details;

            case DEVICE_OS_UPDATED -> "🔄 Device OS updated for user: " + userName;

            case DEVICE_BROWSER_CHANGED -> "🌐 Device browser changed for user: " + userName;

            // Device Authentication
            case DEVICE_LOGIN_SUCCESS -> "✅ Device login successful for user: " + userName;

            case DEVICE_LOGIN_FAILED -> "❌ Device login failed for user: " + userName + " - " + details;

            case DEVICE_AUTH_CHALLENGE -> "🔐 Device authentication challenge for user: " + userName + " - " + details;

            case DEVICE_AUTH_SUCCESS -> "✅ Device authentication successful for user: " + userName;

            case DEVICE_AUTH_FAILED -> "❌ Device authentication failed for user: " + userName + " - " + details;

            case NEW_DEVICE_LOGIN -> "🔍 Login from new device for user: " + userName + " - " + details;

            // Device Sessions
            case DEVICE_SESSION_STARTED -> "▶️ Device session started for user: " + userName;

            case DEVICE_SESSION_ENDED -> "⏹️ Device session ended for user: " + userName + " - " + details;

            case DEVICE_SESSION_TIMEOUT -> "⏰ Device session timeout for user: " + userName;

            case DEVICE_LIMIT_EXCEEDED -> "⚠️ Device limit exceeded for user: " + userName + " - " + details;

            // Device Security
            case SUSPICIOUS_DEVICE_DETECTED -> "🚨 Suspicious device detected for user: " + userName + " - " + details;

            case DEVICE_COMPROMISE_DETECTED -> "🚨 Device compromise detected for user: " + userName + " - " + details;

            case DEVICE_QUARANTINED -> "🔒 Device quarantined for user: " + userName + " - " + details;

            case DEVICE_QUARANTINE_RELEASED -> "🔓 Device quarantine released for user: " + userName;

            case DEVICE_SECURITY_SCAN_PASSED -> "✅ Device security scan passed for user: " + userName;

            case DEVICE_SECURITY_SCAN_FAILED -> "❌ Device security scan failed for user: " + userName + " - " + details;

            // Device Compliance
            case DEVICE_COMPLIANCE_CHECK -> log.isSuccessful() ?
                    "✅ Device compliance check passed for user: " + userName :
                    "❌ Device compliance check failed for user: " + userName + " - " + details;

            case DEVICE_SECURITY_POLICY_APPLIED -> "🛡️ Security policy applied to device for user: " + userName;

            case MOBILE_DEVICE_ENROLLED -> "📱 Mobile device enrolled for user: " + userName;

            case MOBILE_DEVICE_WIPED -> log.isSuccessful() ?
                    "🧹 Mobile device wiped for user: " + userName :
                    "❌ Failed to wipe mobile device for user: " + userName;

            // Admin Actions
            case DEVICE_FORCE_REMOVED -> "🚫 Device force removed for user: " + userName + " - " + details;

            case DEVICE_ADMIN_OVERRIDE -> "👨‍💼 Admin override applied for user: " + userName + " - " + details;

            // Default for other actions
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

        // Mark security-critical actions for immediate review
        switch (this) {
            case SUSPICIOUS_DEVICE_DETECTED, DEVICE_COMPROMISE_DETECTED,
                 DEVICE_FINGERPRINT_CHANGED, UNUSUAL_DEVICE_LOCATION -> {
                log.setTriggeredAlert(true);
                log.setMetadata("{\"requiresReview\":true,\"securityCritical\":true}");
            }
        }

        // Add device-specific metadata if not already present
        if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
            log.setMetadata("{}");
        }
    }
}