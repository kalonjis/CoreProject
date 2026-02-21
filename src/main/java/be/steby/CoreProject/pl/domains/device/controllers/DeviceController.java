package be.steby.CoreProject.pl.domains.device.controllers;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.pl.domains.device.models.requests.DeviceTrustLevelRequest;
import be.steby.CoreProject.pl.domains.device.models.responses.DeviceOperationResponse;
import be.steby.CoreProject.pl.domains.device.models.responses.DeviceInfoResponse;
import be.steby.CoreProject.pl.domains.device.models.responses.DeviceSessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for device management operations.
 * Handles authenticated user's device operations including:
 * - Device listing and details
 * - Device confirmation/rejection
 * - Trust level management
 * - Device disconnection
 * 
 * All endpoints require user authentication.
 * Business logic is delegated to DeviceService.
 */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
@Slf4j
//@PreAuthorize("isAuthenticated()")
public class DeviceController {

    private final DeviceService deviceService;

    // =========================================================================
    // DEVICE INFORMATION ENDPOINTS
    // =========================================================================

    /**
     * Get current device information based on request headers/fingerprint.
     *
     * @return Current device information
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/current")
    public ResponseEntity<DeviceInfoResponse> getCurrentDevice() {
        log.info("Get current device information request");
        
        Device device = deviceService.detectCurrentDevice();
        
        log.info("Current device retrieved - deviceId: {}", device.getId());
        return ResponseEntity.ok(DeviceInfoResponse.fromEntity(device));
    }


    /**
     * Get current device information based on request headers/fingerprint.
     *
     * @return Current device information
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/session")
    public ResponseEntity<DeviceSessionResponse> getDeviceSessionInfo() {
        log.info("Get current device information request");

        Device device = deviceService.detectCurrentDevice();

        log.info("Current device retrieved - deviceId: {}", device.getId());
        return ResponseEntity.ok(DeviceSessionResponse.fromEntity(device));
    }

    /**
     * Get specific device details (must belong to authenticated user).
     * 
     * @param publicId Device publicID to retrieve
     * @return Device details
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{publicId}")
    public ResponseEntity<DeviceInfoResponse> getDevice(@PathVariable String publicId) {
        log.info("Get device details - publicId: {}", publicId);
        
        var device = deviceService.getMyDeviceByPublicId(publicId);
        
        log.info("Device retrieved - publicId: {}", publicId);
        return ResponseEntity.ok(DeviceInfoResponse.fromEntity(device));
    }

    /**
     * Get all devices belonging to the authenticated user.
     * 
     * @return List of user's devices
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("my-devices")
    public ResponseEntity<List<DeviceInfoResponse>> getMyDevices() {
        log.info("Get user devices list request");
        
        var devices = deviceService.getMyDeviceList()
                .stream()
                .map(DeviceInfoResponse::fromEntity)
                .toList();
        
        log.info("Retrieved {} devices for user", devices.size());
        return ResponseEntity.ok(devices);
    }

    // =========================================================================
    // DEVICE CONFIRMATION ENDPOINTS
    // =========================================================================

    /**
     * Confirm device using token from email link.
     * 
     * @param token Device confirmation token
     * @return Confirmation result
     */
    @GetMapping("/confirm")
    public ResponseEntity<DeviceOperationResponse> confirmDevice(@RequestParam String token) {
        log.info("Device confirmation attempt");
        
        var device = deviceService.confirmDevice(token);
        
        log.info("Device confirmed successfully - deviceId: {}", device.getId());
        return ResponseEntity.ok(DeviceOperationResponse.deviceConfirmed(device.getId()));
    }

    /**
     * Reject device using token from email link.
     * 
     * @param token Device rejection token
     * @return No content response
     */
    @GetMapping("/reject")
    public ResponseEntity<DeviceOperationResponse> rejectDevice(@RequestParam String token) {
        log.info("Device rejection attempt");
        
        deviceService.rejectDevice(token);
        
        log.info("Device rejected successfully");
        return ResponseEntity.ok(DeviceOperationResponse.deviceRejected());
    }

    /**
     * Request new confirmation link for current device.
     *
     * @return Operation result
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/request-confirmation")
    public ResponseEntity<DeviceOperationResponse> requestConfirmationLink() {
        log.info("Device confirmation link request");
        
        deviceService.requestConfirmationLink();
        
        log.info("Confirmation link sent successfully");
        return ResponseEntity.ok(DeviceOperationResponse.confirmationLinkSent());
    }

    // =========================================================================
    // DEVICE MANAGEMENT ENDPOINTS
    // =========================================================================

    /**
     * Update device trust level (user can only modify their own devices).
     * 
     * @param publicId Device publicId to update
     * @param request Trust level update request
     * @return No content response
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/trust-level/{publicId}")
    public ResponseEntity<DeviceOperationResponse> updateDeviceTrustLevel(
            @PathVariable String publicId,
            @Valid @RequestBody DeviceTrustLevelRequest request) {
        
        log.info("Update device trust level - publicId: {}, newLevel: {}",
                publicId, request.deviceTrustLevel());
        
        deviceService.updateTrustLevel(publicId, request.deviceTrustLevel());
        
        log.info("Device trust level updated successfully - publicId: {}", publicId);
        return ResponseEntity.ok(DeviceOperationResponse.trustLevelUpdated());
    }

    /**
     * Disconnect specific device (revoke all sessions).
     * 
     * @param publicId DevicepublicID to disconnect
     * @return Operation result
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/disconnect/{publicId}")
    public ResponseEntity<DeviceOperationResponse> disconnectDevice(
            @PathVariable String publicId) {
        
        log.info("Disconnect device request - publicId: {}", publicId);
        
        deviceService.disconnectDevice(publicId);
        
        log.info("Device disconnected successfully - deviceId: {}", publicId);
        return ResponseEntity.ok(DeviceOperationResponse.deviceDisconnected());
    }

    /**
     * Disconnect all other devices except current one.
     *
     * @return Operation result
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/disconnect-all-others")
    public ResponseEntity<DeviceOperationResponse> disconnectAllOtherDevices() {
        
        log.info("Disconnect all other devices request");
        
        int disconnectedCount = deviceService.disconnectAllOtherDevices();
        
        log.info("Disconnected {} other devices successfully", disconnectedCount);
        return ResponseEntity.ok(DeviceOperationResponse.allOtherDevicesDisconnected(disconnectedCount));
    }
}