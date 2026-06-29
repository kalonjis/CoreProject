package be.steby.CoreProject.pl.domains.test.controller;

import be.steby.CoreProject.bll.domains.device.utils.DeviceSecurityEvaluator;
import be.steby.CoreProject.bll.domains.device.utils.DeviceContextProvider;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.il.device.RequiresDeviceTrustLevel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/test/device-security")
@RequiredArgsConstructor
public class TestDeviceSecurityController {

    private final DeviceSecurityEvaluator deviceSecurityEvaluator;

    /**
     * Test endpoint without any device trust level requirement
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDeviceInfo(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Device device = DeviceContextProvider.getAuthenticatedDevice(request);

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("username", auth != null ? auth.getName() : null);
        response.put("deviceId", device != null ? device.getId() : null);
        response.put("deviceTrustLevel", device != null ? device.getDeviceTrustLevel() : null);
        response.put("deviceConfirmed", device != null ? device.isConfirmed() : null);
        response.put("deviceBlacklisted", device != null ? device.isBlacklisted() : null);

        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint requiring BASIC trust level
     */
    @GetMapping("/basic-access")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.BASIC)
    public ResponseEntity<Map<String, Object>> testBasicAccess() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Access granted - BASIC trust level verified");
        response.put("requiredLevel", "BASIC");
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint requiring TRUSTED trust level
     */
    @GetMapping("/trusted-access")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.TRUSTED)
    public ResponseEntity<Map<String, Object>> testTrustedAccess() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Access granted - TRUSTED trust level verified");
        response.put("requiredLevel", "TRUSTED");
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint requiring HIGH trust level
     */
    @GetMapping("/high-access")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
    public ResponseEntity<Map<String, Object>> testHighAccess() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Access granted - HIGH trust level verified");
        response.put("requiredLevel", "HIGH");
        return ResponseEntity.ok(response);
    }

    /**
     * Manual test of DeviceSecurityEvaluator methods
     */
    @GetMapping("/manual-check")
    public ResponseEntity<Map<String, Object>> manualDeviceCheck(
            HttpServletRequest request,
            @RequestParam(defaultValue = "BASIC") String requiredLevel) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        DeviceTrustLevel level = DeviceTrustLevel.valueOf(requiredLevel.toUpperCase());

        Map<String, Object> response = new HashMap<>();
        response.put("requiredLevel", requiredLevel);
        response.put("hasRequiredTrustLevel",
                deviceSecurityEvaluator.hasRequiredTrustLevel(auth, request, level));
        response.put("isDeviceConfirmed",
                deviceSecurityEvaluator.isDeviceConfirmed(auth, request));
        response.put("isDeviceBlacklisted",
                deviceSecurityEvaluator.isDeviceBlacklisted(auth, request));
        response.put("currentDeviceTrustLevel",
                deviceSecurityEvaluator.getCurrentDeviceTrustLevel(auth, request));

        return ResponseEntity.ok(response);
    }

    /**
     * Test complex security criteria
     */
    @GetMapping("/security-criteria")
    public ResponseEntity<Map<String, Object>> testSecurityCriteria(
            HttpServletRequest request,
            @RequestParam(defaultValue = "BASIC") String requiredLevel,
            @RequestParam(defaultValue = "false") boolean mustBeConfirmed,
            @RequestParam(defaultValue = "false") boolean allowBlacklisted) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        DeviceTrustLevel level = DeviceTrustLevel.valueOf(requiredLevel.toUpperCase());

        boolean meetsCriteria = deviceSecurityEvaluator.meetsSecurityCriteria(
                auth, request, level, mustBeConfirmed, allowBlacklisted);

        Map<String, Object> response = new HashMap<>();
        response.put("requiredLevel", requiredLevel);
        response.put("mustBeConfirmed", mustBeConfirmed);
        response.put("allowBlacklisted", allowBlacklisted);
        response.put("meetsCriteria", meetsCriteria);

        return ResponseEntity.ok(response);
    }
}