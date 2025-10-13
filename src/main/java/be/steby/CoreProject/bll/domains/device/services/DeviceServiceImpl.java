package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.events.DevicePersistedEvent;
import be.steby.CoreProject.bll.domains.device.events.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.domains.device.exceptions.DeviceDomainException;
import be.steby.CoreProject.bll.domains.device.exceptions.DeviceNotFoundException;
import be.steby.CoreProject.bll.domains.device.exceptions.InvalidDeviceArgumentException;
import be.steby.CoreProject.bll.domains.device.services.tokens.confirmation.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.utils.UserAgentUtils;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.bll.exceptions.CurrentDeviceDisconnectionException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.OwnershipException;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Service implementation for device management operations.
 * Handles device detection, registration, security management, and lifecycle operations.
 *
 * This service is responsible for:
 * - Device detection and registration from HTTP requests
 * - Device trust level management
 * - Device confirmation and rejection
 * - Device disconnection and logout operations
 * - Cache consistency through events publishing
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserAgentAnalyzer userAgentAnalyzer;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final DeviceFingerprintService deviceFingerprintService;

    @Override
    public Device getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> DeviceNotFoundException.byId(id));
    }


    @Override
    public Device getDeviceByPublicId(String publicId) {
        return deviceRepository.findByPublicId(publicId)
                .orElseThrow(() -> DeviceNotFoundException.byPublicId(publicId));
    }

    @Override
    public Device getMyDevice(Long deviceId) {
        Device device = getDeviceById(deviceId);
        validateDeviceOwnership(device);
        return device;
    }

    @Override
    public List<Device> getMyDeviceList() {
        User authenticatedUser = userService.getAuthenticatedUser();
        return getUserDevices(authenticatedUser);
    }

    @Override
    public List<Device> getUserDevices(User user) {
        if (user == null) {
            log.error("Attempted to fetch devices with null user");
            throw new InvalidDeviceArgumentException("User cannot be null");
        }

        if (user.getId() == null) {
            log.error("Attempted to fetch devices with non-persisted user: {}", user.getUsername());
            throw new InvalidDeviceArgumentException("User must be persisted (ID required)");
        }

        log.debug("Fetching devices for user: {} (ID: {})", user.getUsername(), user.getId());
        List<Device> devices = deviceRepository.findAllByUser(user);

        if (devices.isEmpty()) {
            log.info("No devices found for user {}", user.getUsername());
            return Collections.emptyList();
        }

        log.debug("Found {} devices for user {}", devices.size(), user.getUsername());

        return devices;
    }

    @Override
    @Transactional
    public Device detectAndRegisterDevice(HttpServletRequest request, User user) {
        String userAgentString = request.getHeader("User-Agent");
        String ipAddress = IpLocationUtils.extractClientIp(request);
        String fingerprint = deviceFingerprintService.generateFingerprint(request, user.getId());
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        Device device = deviceRepository.findByFingerprint(fingerprint)
                .map(existingDevice -> {
                    Device updated = updateExistingDevice(user, existingDevice, ipAddress);
                    return updated;
                })
                .orElseGet(() -> {
                    Device created = createNewDevice(user, agent, request, fingerprint, ipAddress);
                    return created;
                });

        return device;
    }

    @Override
    public Device detectCurrentDevice(HttpServletRequest request) {
        User user = userService.getAuthenticatedUser();
        return detectAndRegisterDevice(request, user);
    }


    @Override
    @Transactional
    public void updateTrustLevel(Long deviceId, DeviceTrustLevel level, HttpServletRequest request) {
        Device device = getMyDevice(deviceId);
        DeviceTrustLevel oldLevel = device.getDeviceTrustLevel();

        if (oldLevel == level) {
            throw new AttributeUnchangedException("Device trust level is already set to " + level.name());
        }

        device.setDeviceTrustLevel(level);
        // Use service method to ensure cache consistency
        saveDevice(device);

        eventPublisher.publishEvent(new DeviceTrustLevelChangedEvent(
                device.getId(),
                level,
                oldLevel.name(),
                userService.getAuthenticatedUser(),
                device
        ));

        log.info("Device trust level changed from {} to {} for device {}",
                oldLevel, level, device.getId());
    }

    @Override
    public void saveDevice(Device device) {
        deviceRepository.save(device);
        // Publish update events for cache management
        eventPublisher.publishEvent(new DevicePersistedEvent(device));
    }

    @Override
    @Transactional
    public Device confirmDevice(String token) {
        DeviceConfirmationToken confirmationToken = deviceConfirmationTokenService.getSecureValidToken(token, TokenType.DEVICE_CONFIRMATION);

        Device device = getDeviceById(confirmationToken.getDeviceId());
        device.setConfirmed(true);
        device.setDeviceTrustLevel(DeviceTrustLevel.TRUSTED);

        // Use service method to ensure cache consistency
        saveDevice(device);

        deviceConfirmationTokenService.revokeToken(confirmationToken);

        log.info("Device {} confirmed and trust level updated to TRUSTED", device.getId());
        return device;
    }

    @Override
    @Transactional
    public void rejectDevice(String token) {
        Device device = getDeviceByToken(token);
        device.setDeviceTrustLevel(DeviceTrustLevel.UNTRUSTED);
        device.setConfirmed(false);
        device.setBlacklisted(true);
        device.setBlacklistedTime(Instant.now());

        User user = device.getUser();
        refreshTokenService.revokeDeviceTokens(user, device);

        // Use service method to ensure cache consistency
        saveDevice(device);

        log.info("Device {} rejected and blacklisted for user {}", device.getId(), user.getUsername());
    }

    @Override
    public void requestConfirmationLink(HttpServletRequest request) {
        User user = userService.getAuthenticatedUser();
        Device currentDevice = detectCurrentDevice(request);

        DeviceConfirmationToken token = deviceConfirmationTokenService.createDeviceConfirmationToken(
                user, currentDevice.getId());

        log.info("Confirmation link requested for device {} of user {}",
                currentDevice.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public void disconnectDevice(Long deviceId, HttpServletRequest request) {
        Device currentDevice = detectCurrentDevice(request);
        User currentUser = currentDevice.getUser();
        Device deviceToDisconnect = getMyDevice(deviceId);

        if (deviceToDisconnect.isLoggedOut()) {
            throw new AttributeUnchangedException("Device with id : " + deviceId + " was already disconnected !");
        }

        if (currentDevice.getId().equals(deviceToDisconnect.getId())) {
            throw new CurrentDeviceDisconnectionException("You cannot disconnect your current device remotely. Please use the logout function instead.");
        }

        refreshTokenService.revokeDeviceTokens(currentUser, deviceToDisconnect);
        deviceToDisconnect.setLoggedOut(true);
        deviceToDisconnect.setLogoutTime(Instant.now());

        // Use service method to ensure cache consistency
        saveDevice(deviceToDisconnect);

        log.info("Device {} marked as disconnected for user {}", deviceId, currentUser.getUsername());
    }

    @Override
    @Transactional
    public void disconnectAllOtherDevices(HttpServletRequest request) {
        Device currentDevice = detectCurrentDevice(request);
        User currentUser = currentDevice.getUser();
        List<Device> userDevices = getMyDeviceList();

        log.info("Starting disconnection of all other devices for user {}", currentUser.getUsername());

        int count = 0;
        for (Device device : userDevices) {
            if (!device.getId().equals(currentDevice.getId())) {
                try {
                    disconnectDevice(device.getId(), request);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to disconnect device {} : {}", device.getId(), e.getMessage());
                }
            }
        }

        log.info("{} devices successfully disconnected for user {}", count, currentUser.getUsername());
    }


    @Override
    @Transactional
    public int disconnectAllDevicesForUser(User user) {
        log.info("Disconnecting all devices for user: {}", user.getUsername());

        List<Device> userDevices = getUserDevices(user);
        int disconnectedCount = 0;

        for (Device device : userDevices) {
            if (!device.isLoggedOut()) {
                device.setLoggedOut(true);
                device.setLogoutTime(Instant.now());
                saveDevice(device);
                disconnectedCount++;
            }
        }

        log.info("Disconnected {} devices for user: {}", disconnectedCount, user.getUsername());
        return disconnectedCount;
    }

    @Override
    public Long getTotalDevices() {
        return deviceRepository.count();
    }

    // =========================================================================
    // Private helper methods
    // =========================================================================

    /**
     * Updates an existing device with new connection information.
     *
     * @param user The user associated with the device
     * @param device The existing device to update
     * @param ipAddress The current IP address
     * @return The updated device
     */
    private Device updateExistingDevice(User user, Device device, String ipAddress) {
        device.setLastSeen(Instant.now());
        device.setLastIpAddress(ipAddress);
        device.setLocation(IpLocationUtils.resolveLocationFromIp(ipAddress));

        // Use service method to ensure cache consistency
        saveDevice(device);
        return device;
    }

    /**
     * Creates a new device from HTTP request information.
     *
     * @param user The user to associate with the device
     * @param agent Parsed user agent information
     * @param request The HTTP request
     * @param fingerprint The device fingerprint
     * @param ipAddress The client IP address
     * @return The newly created device
     */
    private Device createNewDevice(User user, UserAgent agent, HttpServletRequest request, String fingerprint, String ipAddress) {
        Device device = Device.builder()
                .user(user)
                .fingerprint(fingerprint)
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress(ipAddress)
                .location(IpLocationUtils.resolveLocationFromIp(ipAddress))
                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
                .confirmed(false)
                .blacklisted(false)
                .build();

        UserAgentUtils.populateDeviceInfo(device, agent, request);

        if (isFirstDevice(user)) {
            device.setFirstDeviceUsed(true);
        }

        // Use service method to ensure cache consistency
        saveDevice(device);
        log.info("New device created with ID {} for user {}", device.getId(), user.getUsername());
        return device;
    }


    /**
     * Retrieves a device by its confirmation token.
     * This method validates and revokes the token before returning the device.
     *
     * @param token The device confirmation token
     * @return The device associated with the token
     */
    private Device getDeviceByToken(String token) {
        DeviceConfirmationToken deviceConfirmationToken = deviceConfirmationTokenService.getSecureValidToken(token, TokenType.DEVICE_CONFIRMATION);
        deviceConfirmationTokenService.revokeToken(deviceConfirmationToken);
        Long deviceId = deviceConfirmationToken.getDeviceId();
        return getDeviceById(deviceId);
    }

    /**
     * Checks if the authenticated user owns the specified device.
     *
     * @param device The device to check ownership for
     * @return true if the authenticated user owns the device, false otherwise
     */
    private boolean authenticatedUserOwnsDevice(Device device) {
        User auth = userService.getAuthenticatedUser();
        return device.getUser().getId().equals(auth.getId());
    }

    /**
     * Validates that the authenticated user owns the specified device.
     * Throws an exception if the user does not own the device.
     *
     * @param device The device to validate ownership for
     * @throws OwnershipException if the user does not own the device
     */
    private void validateDeviceOwnership(Device device) {
        if (!authenticatedUserOwnsDevice(device)) {
            throw new OwnershipException("Access denied: You can only access your own devices.");
        }
    }

    /**
     * Checks if this is the first device for the specified user.
     *
     * @param user The user to check
     * @return true if this is the user's first device, false otherwise
     */
    private boolean isFirstDevice(User user) {
        List<Device> devices = getUserDevices(user);
        return devices == null || devices.isEmpty();
    }
}