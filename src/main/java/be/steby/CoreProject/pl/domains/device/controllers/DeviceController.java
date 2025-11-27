package be.steby.CoreProject.pl.domains.device.controllers;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.pl.domains.device.models.requests.DeviceTrustLevelRequest;
import be.steby.CoreProject.pl.domains.device.models.responses.DeviceOperationResponse;
import be.steby.CoreProject.pl.domains.device.models.responses.DeviceInfoResponse;
import be.steby.CoreProject.pl.domains.device.models.responses.DeviceSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
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
     * @param request HTTP request for device detection
     * @return Current device information
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/current")
    public ResponseEntity<DeviceInfoResponse> getCurrentDevice(HttpServletRequest request) {
        log.info("Get current device information request");
        
        Device device = deviceService.detectCurrentDevice(request);
        
        log.info("Current device retrieved - deviceId: {}", device.getId());
        return ResponseEntity.ok(DeviceInfoResponse.fromEntity(device));
    }


    /**
     * Get current device information based on request headers/fingerprint.
     *
     * @param request HTTP request for device detection
     * @return Current device information
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/session")
    public ResponseEntity<DeviceSessionResponse> getDeviceSessionInfo(HttpServletRequest request) {
        log.info("Get current device information request");

        Device device = deviceService.detectCurrentDevice(request);

        log.info("Current device retrieved - deviceId: {}", device.getId());
        return ResponseEntity.ok(DeviceSessionResponse.fromEntity(device));
    }

    /**
     * Get specific device details (must belong to authenticated user).
     * 
     * @param deviceId Device ID to retrieve
     * @return Device details
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceInfoResponse> getDevice(@PathVariable Long deviceId) {
        log.info("Get device details - deviceId: {}", deviceId);
        
        var device = deviceService.getMyDevice(deviceId);
        
        log.info("Device retrieved - deviceId: {}", deviceId);
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
     * @param request HTTP request for device detection
     * @return Operation result
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/request-confirmation")
    public ResponseEntity<DeviceOperationResponse> requestConfirmationLink(
            HttpServletRequest request) {
        log.info("Device confirmation link request");
        
        deviceService.requestConfirmationLink(request);
        
        log.info("Confirmation link sent successfully");
        return ResponseEntity.ok(DeviceOperationResponse.confirmationLinkSent());
    }

    // =========================================================================
    // DEVICE MANAGEMENT ENDPOINTS
    // =========================================================================

    /**
     * Update device trust level (user can only modify their own devices).
     * 
     * @param deviceId Device ID to update
     * @param request Trust level update request
     * @param httpRequest HTTP request for context
     * @return No content response
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/trust-level/{deviceId}")
    public ResponseEntity<DeviceOperationResponse> updateDeviceTrustLevel(
            @PathVariable Long deviceId,
            @Valid @RequestBody DeviceTrustLevelRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Update device trust level - deviceId: {}, newLevel: {}", 
                deviceId, request.deviceTrustLevel());
        
        deviceService.updateTrustLevel(deviceId, request.deviceTrustLevel(), httpRequest);
        
        log.info("Device trust level updated successfully - deviceId: {}", deviceId);
        return ResponseEntity.ok(DeviceOperationResponse.trustLevelUpdated());
    }

    /**
     * Disconnect specific device (revoke all sessions).
     * 
     * @param deviceId Device ID to disconnect
     * @param request HTTP request for context
     * @return Operation result
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/disconnect/{deviceId}")
    public ResponseEntity<DeviceOperationResponse> disconnectDevice(
            @PathVariable Long deviceId, 
            HttpServletRequest request) {
        
        log.info("Disconnect device request - deviceId: {}", deviceId);
        
        deviceService.disconnectDevice(deviceId, request);
        
        log.info("Device disconnected successfully - deviceId: {}", deviceId);
        return ResponseEntity.ok(DeviceOperationResponse.deviceDisconnected());
    }

    /**
     * Disconnect all other devices except current one.
     * 
     * @param request HTTP request for current device detection
     * @return Operation result
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/disconnect-all-others")
    public ResponseEntity<DeviceOperationResponse> disconnectAllOtherDevices(
            HttpServletRequest request) {
        
        log.info("Disconnect all other devices request");
        
        int disconnectedCount = deviceService.disconnectAllOtherDevices(request);
        
        log.info("Disconnected {} other devices successfully", disconnectedCount);
        return ResponseEntity.ok(DeviceOperationResponse.allOtherDevicesDisconnected(disconnectedCount));
    }
}