package be.steby.CoreProject.pl.security.models;

import be.steby.CoreProject.dl.entities.ConnectionLog;

import java.time.Instant;

public record ConnectionLogDTO(
        Long id,
        Long userId,
        String username,
        Long deviceId,
        String deviceInfo,
        Instant timestamp,
        String ipAddress,
        boolean successful,
        String failureReason,
        String actionType
) {
    public static ConnectionLogDTO fromEntity(ConnectionLog log) {
        String deviceInfo = log.getDevice() != null ?
                String.format("%s - %s %s",
                        log.getDevice().getDeviceType(),
                        log.getDevice().getBrowser(),
                        log.getDevice().getOperatingSystem()) : "Unknown";

        return new ConnectionLogDTO(
                log.getId(),
                log.getUser().getId(),
                log.getUser().getUsername(),
                log.getDevice() != null ? log.getDevice().getId() : null,
                deviceInfo,
                log.getTimestamp(),
                log.getIpAddress(),
                log.isSuccessful(),
                log.getFailureReason(),
                log.getActionType()
        );
    }
}
