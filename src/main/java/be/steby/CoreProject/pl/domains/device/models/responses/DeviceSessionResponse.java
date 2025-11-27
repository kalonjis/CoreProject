package be.steby.CoreProject.pl.domains.device.models.responses;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;

import java.time.Instant;

/**
 * Response containing device information for session frontend side.
 */
public record DeviceSessionResponse(
        String publicId,
        boolean confirmed,
        DeviceTrustLevel level,
        boolean blacklisted

) {
    public static DeviceSessionResponse fromEntity(Device device){
        return new DeviceSessionResponse(
                device.getPublicId(),
                device.isConfirmed(),
                device.getDeviceTrustLevel(),
                device.isBlacklisted()
        );
    }
}