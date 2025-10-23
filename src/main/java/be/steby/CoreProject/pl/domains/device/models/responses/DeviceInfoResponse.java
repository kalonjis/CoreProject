package be.steby.CoreProject.pl.domains.device.models.responses;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Response containing device information for frontend display.
 */
public record DeviceInfoResponse(
        Long id,
        String deviceType,
        String browser,
        String browserVersion,
        String operatingSystem,
        String osVersion,
        String device_cpu,
        String device_cpu_bits,
        String language,
        String deviceClass,
        String deviceBrand,
        String fingerprint,
        Instant firstSeen,
        Instant lastSeen,
        String lastIpAddress,
        DeviceTrustLevel level,
        boolean confirmed,
        boolean blacklisted,
        Instant blacklistedTime,
        boolean loggedOut,
        Instant logoutTime
) {
    public static DeviceInfoResponse fromEntity(Device device){
        return new DeviceInfoResponse(
                device.getId(),
                device.getDeviceType(),
                device.getBrowser(),
                device.getBrowserVersion(),
                device.getOperatingSystem(),
                device.getOsVersion(),
                device.getDevice_cpu(),
                device.getDevice_cpu_bits(),
                device.getLanguage(),
                device.getDeviceClass(),
                device.getDeviceBrand(),
                device.getFingerprint(),
                device.getFirstSeen(),
                device.getLastSeen(),
                device.getLastIpAddress(),
                device.getDeviceTrustLevel(),
                device.isConfirmed(),
                device.isBlacklisted(),
                device.getBlacklistedTime(),
                device.isLoggedOut(),
                device.getLogoutTime()
        );
    }
}