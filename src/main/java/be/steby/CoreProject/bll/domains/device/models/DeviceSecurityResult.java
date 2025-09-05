package be.steby.CoreProject.bll.domains.device.models;

import be.steby.CoreProject.dl.entities.Device;
import lombok.Value;

/**
 * Value object representing the result of a device security validation.
 * Used to encapsulate both success/failure state and any relevant data or error messages.
 */
@Value
public class DeviceSecurityResult {
    boolean success;
    Device device;
    String errorMessage;

    /**
     * Creates a successful result with the validated device.
     */
    public static DeviceSecurityResult success(Device device) {
        return new DeviceSecurityResult(true, device, null);
    }

    /**
     * Creates a failure result with an error message.
     */
    public static DeviceSecurityResult failure(String errorMessage) {
        return new DeviceSecurityResult(false, null, errorMessage);
    }

    /**
     * Checks if the result indicates a successful validation.
     */
    public boolean isValid() {
        return success && device != null;
    }
}