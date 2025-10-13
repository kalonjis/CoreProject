package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.device.AdminDeviceService;
import be.steby.CoreProject.pl.models.device.DeviceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Arrays.stream;

/**
 * REST Controller for administrative device management operations.
 * Handles device-related operations that require administrative privileges.
 * All business logic is delegated to AdminDeviceService while this controller
 * focuses solely on HTTP concerns (status codes, headers, response formatting).
 *
 * All endpoints require ADMIN authority.
 * 
 * Endpoint pattern: /api/admin/devices/*
 */
@RestController
@RequestMapping("/api/admin/device")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Slf4j
public class AdminDeviceController {

    private final AdminDeviceService adminDeviceService;

    // =========================================================================
    // DEVICE QUERY OPERATIONS
    // =========================================================================

    /**
     * Retrieves all devices for a specific user.
     *
     * @param publicUserId User public ID to get devices for
     * @return ResponseEntity with list of user's devices
     */
    @GetMapping("/list/user/{publicUserId}")
    public ResponseEntity<List<DeviceDTO>> getUserDevices(@PathVariable String publicUserId) {
        log.info("Admin requesting devices for user: {}", publicUserId);

        List<DeviceDTO> deviceDTOS = adminDeviceService.getUserDevices(publicUserId)
                        .stream()
                        .map(DeviceDTO::fromEntity)
                        .toList();

        log.info("Successfully retrieved devices for user: {}", publicUserId);

        return ResponseEntity.ok(deviceDTOS);
    }

    /**
     * Retrieves details of a specific device.
     *
     * @param devicePublicId Device public ID to retrieve
     * @return ResponseEntity with device details
     */
    @GetMapping("/{devicePublicId}")
    public ResponseEntity<DeviceDTO> getDeviceDetails(@PathVariable String devicePublicId) {
        log.info("Admin requesting details for device: {}", devicePublicId);

        DeviceDTO deviceDTO = DeviceDTO.fromEntity(
             adminDeviceService.getDeviceByPublicId(devicePublicId)
        );

        log.info("Successfully retrieved device details: {}", devicePublicId);
        return ResponseEntity.ok(deviceDTO);
    }
//
//    /**
//     * Retrieves total count of devices in the system.
//     *
//     * @return ResponseEntity with total device count
//     */
//    @GetMapping("/count")
//    public ResponseEntity<AdminDeviceOperationResponse> getTotalDevices() {
//        log.info("Admin requesting total device count");
//
//        adminDeviceService.getTotalDevices();
//
//        log.info("Successfully retrieved total device count");
//        return ResponseEntity.ok(AdminDeviceOperationResponse.deviceCountRetrieved());
//    }
//
//    // =========================================================================
//    // DEVICE SECURITY OPERATIONS
//    // =========================================================================
//
//    /**
//     * Updates the trust level of a specific device.
//     *
//     * @param deviceId Device ID to update
//     * @param request HTTP request for context capture
//     * @return ResponseEntity confirming trust level update
//     */
//    @PatchMapping("/{deviceId}/trust-level")
//    public ResponseEntity<AdminDeviceOperationResponse> updateDeviceTrustLevel(
//            @PathVariable Long deviceId,
//            HttpServletRequest request) {
//        log.info("Admin updating trust level for device: {}", deviceId);
//
//        adminDeviceService.updateTrustLevel(deviceId, request);
//
//        log.info("Successfully updated trust level for device: {}", deviceId);
//        return ResponseEntity.ok(AdminDeviceOperationResponse.trustLevelUpdated());
//    }
//
//    /**
//     * Blacklists a specific device, preventing future authentication.
//     *
//     * @param deviceId Device ID to blacklist
//     * @param request HTTP request for context capture
//     * @return ResponseEntity confirming device blacklist
//     */
//    @PostMapping("/{deviceId}/blacklist")
//    public ResponseEntity<AdminDeviceOperationResponse> blacklistDevice(
//            @PathVariable Long deviceId,
//            HttpServletRequest request) {
//        log.info("Admin blacklisting device: {}", deviceId);
//
//        adminDeviceService.blacklistDevice(deviceId, request);
//
//        log.info("Successfully blacklisted device: {}", deviceId);
//        return ResponseEntity.ok(AdminDeviceOperationResponse.deviceBlacklisted());
//    }
//
//    /**
//     * Removes a device from the blacklist, allowing future authentication.
//     *
//     * @param deviceId Device ID to remove from blacklist
//     * @param request HTTP request for context capture
//     * @return ResponseEntity confirming blacklist removal
//     */
//    @DeleteMapping("/{deviceId}/blacklist")
//    public ResponseEntity<AdminDeviceOperationResponse> removeDeviceFromBlacklist(
//            @PathVariable Long deviceId,
//            HttpServletRequest request) {
//        log.info("Admin removing device from blacklist: {}", deviceId);
//
//        adminDeviceService.removeFromBlacklist(deviceId, request);
//
//        log.info("Successfully removed device from blacklist: {}", deviceId);
//        return ResponseEntity.ok(AdminDeviceOperationResponse.deviceRemovedFromBlacklist());
//    }
//
//    // =========================================================================
//    // DEVICE SESSION MANAGEMENT
//    // =========================================================================
//
//    /**
//     * Disconnects all devices for a specific user, forcing re-authentication.
//     *
//     * @param userId User ID whose devices should be disconnected
//     * @param request HTTP request for context capture
//     * @return ResponseEntity confirming devices disconnection
//     */
//    @PostMapping("/user/{userId}/disconnect-all")
//    public ResponseEntity<AdminDeviceOperationResponse> disconnectAllUserDevices(
//            @PathVariable Long userId,
//            HttpServletRequest request) {
//        log.info("Admin disconnecting all devices for user: {}", userId);
//
//        adminDeviceService.disconnectAllUserDevices(userId, request);
//
//        log.info("Successfully disconnected all devices for user: {}", userId);
//        return ResponseEntity.ok(AdminDeviceOperationResponse.allDevicesDisconnected());
//    }
//
//    /**
//     * Disconnects a specific device, invalidating its session.
//     *
//     * @param deviceId Device ID to disconnect
//     * @param request HTTP request for context capture
//     * @return ResponseEntity confirming device disconnection
//     */
//    @PostMapping("/{deviceId}/disconnect")
//    public ResponseEntity<AdminDeviceOperationResponse> disconnectDevice(
//            @PathVariable Long deviceId,
//            HttpServletRequest request) {
//        log.info("Admin disconnecting device: {}", deviceId);
//
//        adminDeviceService.disconnectDevice(deviceId, request);
//
//        log.info("Successfully disconnected device: {}", deviceId);
//        return ResponseEntity.ok(AdminDeviceOperationResponse.deviceDisconnected());
//    }
}