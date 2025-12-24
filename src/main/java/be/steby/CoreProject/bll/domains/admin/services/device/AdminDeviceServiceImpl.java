package be.steby.CoreProject.bll.domains.admin.services.device;

import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of AdminDeviceService.
 * Handles administrative device operations with proper permission validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDeviceServiceImpl implements AdminDeviceService {

    private final DeviceService deviceService;
    private final DeviceRepository deviceRepository;
    private final UserService userService;
    private final AdminPermissionValidator adminPermissionValidator;

    @Override
    @Transactional(readOnly = true)
    public List<Device> getUserDevices(String publicUserId) {
        log.debug("Admin retrieving devices for user: {}", publicUserId);

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Get user and their devices
        User targetUser = userService.getUserByPublicId(publicUserId);
        List<Device> devices = deviceRepository.findByUser(targetUser);

        log.debug("Retrieved {} devices for user: {}", devices.size(), publicUserId);

        return devices;
    }

    @Override
    @Transactional(readOnly = true)
    public Device getDeviceByPublicId(String devicePublicId) {
        log.debug("Admin retrieving device: {}", devicePublicId);

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Get device
        Device device = deviceService.getDeviceByPublicId(devicePublicId);

        log.debug("Retrieved device: {}", devicePublicId);

        return device;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDeviceStatistics() {
        log.debug("Admin retrieving device statistics");

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Gather all device statistics atomically
        Map<String, Object> stats = new HashMap<>();

        // Total device count
        Long totalDevices = deviceRepository.count();
        stats.put("totalDevices", totalDevices);

        // Trusted devices (TRUSTED + HIGHLY_TRUSTED)
        Long trustedDevices = deviceRepository.countTrustedDevices();
        stats.put("trustedDevices", trustedDevices);

        // Untrusted devices (UNTRUSTED + BASIC)
        Long untrustedDevices = deviceRepository.countUntrustedDevices();
        stats.put("untrustedDevices", untrustedDevices);

        // Breakdown by exact trust level (optional, mais utile pour debug)
        Long untrustedOnly = deviceRepository.countByDeviceTrustLevel(DeviceTrustLevel.UNTRUSTED);
        Long basicOnly = deviceRepository.countByDeviceTrustLevel(DeviceTrustLevel.BASIC);
        Long trustedOnly = deviceRepository.countByDeviceTrustLevel(DeviceTrustLevel.TRUSTED);
        Long highlyTrustedOnly = deviceRepository.countByDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED);

        stats.put("untrustedOnly", untrustedOnly);
        stats.put("basicOnly", basicOnly);
        stats.put("trustedOnly", trustedOnly);
        stats.put("highlyTrustedOnly", highlyTrustedOnly);

        // Active devices (last 30 days)
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Long activeDevices = deviceRepository.countActiveDevicesSince(thirtyDaysAgo);
        Long inactiveDevices = totalDevices - activeDevices;
        stats.put("activeDevices", activeDevices);
        stats.put("inactiveDevices", inactiveDevices);

        log.debug("Device statistics: total={}, trusted={}, untrusted={}, active={}",
                totalDevices, trustedDevices, untrustedDevices, activeDevices);

        return stats;
    }
}