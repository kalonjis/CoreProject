package be.steby.CoreProject.pl.domains.device.models.responses;

/**
 * Standardized responses for device operations.
 * Provides consistent response format across all device endpoints.
 */
public record DeviceOperationResponse(
        String message,
        String operation,
        Object data
) {

    // Device confirmation operations
    public static DeviceOperationResponse deviceConfirmed(String devicePublicId) {
        return new DeviceOperationResponse(
                "Device confirmed successfully",
                "DEVICE_CONFIRMED",
                devicePublicId
        );
    }

    public static DeviceOperationResponse deviceRejected() {
        return new DeviceOperationResponse(
                "Device rejected successfully",
                "DEVICE_REJECTED",
                null
        );
    }

    public static DeviceOperationResponse confirmationLinkSent() {
        return new DeviceOperationResponse(
                "Confirmation link sent to your email",
                "CONFIRMATION_LINK_SENT",
                null
        );
    }

    // Device management operations
    public static DeviceOperationResponse trustLevelUpdated() {
        return new DeviceOperationResponse(
                "Device trust level updated successfully",
                "TRUST_LEVEL_UPDATED",
                null
        );
    }

    public static DeviceOperationResponse deviceDisconnected() {
        return new DeviceOperationResponse(
                "Device disconnected successfully",
                "DEVICE_DISCONNECTED",
                null
        );
    }

    public static DeviceOperationResponse allOtherDevicesDisconnected(int count) {
        return new DeviceOperationResponse(
                "All other devices disconnected successfully",
                "ALL_OTHER_DEVICES_DISCONNECTED",
                count
        );
    }
}