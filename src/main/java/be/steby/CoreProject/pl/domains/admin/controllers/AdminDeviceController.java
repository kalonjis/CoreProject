package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.device.AdminDeviceService;
import be.steby.CoreProject.pl.domains.device.models.responses.DeviceInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for administrative device management operations.
 * Handles device-related operations that require administrative privileges.
 * All business logic is delegated to AdminDeviceService while this controller
 * focuses solely on HTTP concerns (status codes, headers, response formatting).
 *
 * All endpoints require ADMIN authority.
 *
 * Endpoint pattern: /api/admin/device/*
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
    public ResponseEntity<List<DeviceInfoResponse>> getUserDevices(@PathVariable String publicUserId) {
        log.info("Admin requesting devices for user: {}", publicUserId);

        List<DeviceInfoResponse> deviceInfoResponses = adminDeviceService.getUserDevices(publicUserId)
                .stream()
                .map(DeviceInfoResponse::fromEntity)
                .toList();

        log.info("Successfully retrieved {} devices for user: {}", deviceInfoResponses.size(), publicUserId);

        return ResponseEntity.ok(deviceInfoResponses);
    }

    /**
     * Retrieves details of a specific device.
     *
     * @param devicePublicId Device public ID to retrieve
     * @return ResponseEntity with device details
     */
    @GetMapping("/{devicePublicId}")
    public ResponseEntity<DeviceInfoResponse> getDeviceDetails(@PathVariable String devicePublicId) {
        log.info("Admin requesting details for device: {}", devicePublicId);

        DeviceInfoResponse deviceInfoResponse = DeviceInfoResponse.fromEntity(
                adminDeviceService.getDeviceByPublicId(devicePublicId)
        );

        log.info("Successfully retrieved device details: {}", devicePublicId);
        return ResponseEntity.ok(deviceInfoResponse);
    }

    // =========================================================================
    // DEVICE STATISTICS
    // =========================================================================

    /**
     * Retrieves comprehensive device statistics for admin dashboard.
     * Provides all device-related metrics in a single efficient call.
     *
     * GET /api/admin/device/stats
     *
     * Response format:
     * {
     *   "totalDevices": 320,
     *   "trustedDevices": 280,
     *   "untrustedDevices": 40,
     *   "activeDevices": 150,
     *   "inactiveDevices": 170,
     *   "timestamp": "2024-12-24T10:00:00Z"
     * }
     *
     * All metrics are computed atomically within a single transaction
     * to ensure consistency across all statistics.
     *
     * Active devices are defined as devices with activity in the last 30 days.
     *
     * @return ResponseEntity with comprehensive device statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDeviceStats() {
        log.info("Admin requesting device statistics");

        Map<String, Object> stats = adminDeviceService.getDeviceStatistics();
        stats.put("timestamp", Instant.now());

        log.info("Device statistics retrieved - total: {}, trusted: {}, active: {}",
                stats.get("totalDevices"), stats.get("trustedDevices"), stats.get("activeDevices"));

        return ResponseEntity.ok(stats);
    }
}