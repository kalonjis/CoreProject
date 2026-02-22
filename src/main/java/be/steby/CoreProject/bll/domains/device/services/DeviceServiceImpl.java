package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.events.*;
import be.steby.CoreProject.bll.domains.device.exceptions.DeviceNotFoundException;
import be.steby.CoreProject.bll.domains.device.exceptions.InvalidDeviceArgumentException;
import be.steby.CoreProject.bll.domains.device.services.tokens.confirmation.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.utils.DeviceContextProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private final DeviceAuthenticationService deviceAuthenticationService;

    @Autowired
    private HttpServletRequest httpServletRequest;

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
    public Device getMyDeviceByPublicId(String publicId) {
        Device device = getDeviceByPublicId(publicId);
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
    public Device detectAndRegisterDevice(User user) {
        String rawUserAgent  = httpServletRequest.getHeader("User-Agent");
        String acceptLanguage = httpServletRequest.getHeader("Accept-Language");
        String ipAddress     = IpLocationUtils.extractClientIp(httpServletRequest);

        // Parse une seule fois, utilisé par les deux
        UserAgent agent      = userAgentAnalyzer.parse(rawUserAgent);
        String fingerprint   = deviceFingerprintService.generateFingerprint(
                agent, rawUserAgent, acceptLanguage, user.getId());

        return deviceRepository.findByFingerprint(fingerprint)
                .map(existing -> updateExistingDevice(user, existing, ipAddress))
                .orElseGet(() -> createNewDevice(user, agent, fingerprint, ipAddress));
    }

    @Override
    public Device detectCurrentDevice() {
        Device device = DeviceContextProvider.getAuthenticatedDevice(httpServletRequest);

        if (device != null) {
            log.debug("Current device {} retrieved from cache", device.getId());
            return device;
        }

        // ⚠️ Fallback: Si pas en cache (cas rare), détecter normalement
        log.warn("Device not found in cache, falling back to detectAndRegisterDevice");
        User authenticatedUser = userService.getAuthenticatedUser();
        return detectAndRegisterDevice(authenticatedUser);
    }


    @Override
    @Transactional
    public void updateTrustLevel(String publicId, DeviceTrustLevel level) {
        Device targetDevice = getMyDeviceByPublicId(publicId);
        DeviceTrustLevel oldLevel = targetDevice.getDeviceTrustLevel();

        if (oldLevel == level) {
            throw new AttributeUnchangedException("Device trust level is already set to " + level.name());
        }

        targetDevice.setDeviceTrustLevel(level);
        // Use service method to ensure cache consistency
        saveDevice(targetDevice);

        Device actorDevice = detectCurrentDevice();

        eventPublisher.publishEvent(new DeviceTrustLevelChangedEvent(
                level,
                oldLevel.name(),
                userService.getAuthenticatedUser(),
                actorDevice,
                targetDevice
        ));

        log.info("Device trust level changed from {} to {} for device {}",
                oldLevel, level, targetDevice.getId());
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

        Device targetDevice = getDeviceById(confirmationToken.getDeviceId());
        targetDevice.setConfirmed(true);
        if(targetDevice.isBlacklisted()){
            targetDevice.setBlacklisted(false);
        }
        targetDevice.setDeviceTrustLevel(DeviceTrustLevel.TRUSTED);

        // Use service method to ensure cache consistency
        saveDevice(targetDevice);

        deviceConfirmationTokenService.revokeToken(confirmationToken);

        User user = confirmationToken.getUser();
        Device actorDevice = detectAndRegisterDevice(user);

        eventPublisher.publishEvent(new DeviceConfirmedEvent(user,actorDevice, targetDevice));

        log.info("Device {} confirmed and trust level updated to TRUSTED", targetDevice.getId());
        return targetDevice;
    }

    @Override
    @Transactional
    public void rejectDevice(String token) {
        Device targetDevice = getDeviceByToken(token);
        targetDevice.setDeviceTrustLevel(DeviceTrustLevel.UNTRUSTED);
        targetDevice.setConfirmed(false);
        targetDevice.setBlacklisted(true);
        targetDevice.setBlacklistedTime(Instant.now());

        // ← AJOUTER : forcer le logout pour bloquer l'access token aussi
        targetDevice.setLoggedOut(true);
        targetDevice.setLogoutTime(Instant.now());

        User user = targetDevice.getUser();
        refreshTokenService.revokeDeviceTokens(user, targetDevice);

        saveDevice(targetDevice); // publie DevicePersistedEvent → cache OK

        // ← AJOUTER : invalidation explicite du cache, pas de side-effect implicite
        deviceAuthenticationService.invalidateDeviceCache(targetDevice.getId());

        Device actorDevice = detectAndRegisterDevice(user);
        eventPublisher.publishEvent(new DeviceRejectedEvent(user, actorDevice, targetDevice));
    }

    @Override
    public void requestConfirmationLink() {
        User user = userService.getAuthenticatedUser();
        Device currentDevice = detectAndRegisterDevice(user);

        eventPublisher.publishEvent(new DeviceConfirmationLinkRequestedEvent(
                user,
                currentDevice)
        );

        log.info("Confirmation link requested for device {} of user {}",
                currentDevice.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public void disconnectDevice(String publicId) {
        Device currentDevice = detectCurrentDevice();
        User currentUser = currentDevice.getUser();
        Device deviceToDisconnect = getMyDeviceByPublicId(publicId);

        if (deviceToDisconnect.isLoggedOut()) {
            throw new AttributeUnchangedException("Device with id : " + publicId + " was already disconnected !");
        }

        if (currentDevice.getId().equals(deviceToDisconnect.getId())) {
            throw new CurrentDeviceDisconnectionException("You cannot disconnect your current device remotely. Please use the logout function instead.");
        }

        refreshTokenService.revokeDeviceTokens(currentUser, deviceToDisconnect);
        deviceToDisconnect.setLoggedOut(true);
        deviceToDisconnect.setLogoutTime(Instant.now());

        // Use service method to ensure cache consistency
        saveDevice(deviceToDisconnect);

        log.info("Device {} marked as disconnected for user {}", publicId, currentUser.getUsername());
    }

    @Override
    @Transactional
    public int disconnectAllOtherDevices() {
        Device currentDevice = detectCurrentDevice();
        User currentUser = currentDevice.getUser();

        List<Device> devicesToDisconnect = getUserDevices(currentUser).stream()
                .filter(d -> !d.getId().equals(currentDevice.getId()))
                .filter(d -> !d.isLoggedOut())
                .toList();

        for (Device device : devicesToDisconnect) {
            refreshTokenService.revokeDeviceTokens(currentUser, device);
            device.setLoggedOut(true);
            device.setLogoutTime(Instant.now());
            saveDevice(device);
        }

        log.info("{} devices successfully disconnected for user {}", devicesToDisconnect.size(), currentUser.getUsername());

        return devicesToDisconnect.size();
    }


    /**
     * Disconnects all devices for a user EXCEPT the current device.
     * Used when user changes password but wants to stay logged in on current device.
     *
     * @param user The user whose devices should be disconnected
     * @param currentDeviceId Id The device to keep connected
     * @return Number of devices disconnected
     */
//    @Override
//    @Transactional
//    public int disconnectAllDevicesExceptCurrent(User user, Long currentDeviceId) {
//        log.info("Disconnecting all devices for user {} except device {}",
//                user.getUsername(), currentDeviceId);
//
//        int disconnectedCount = deviceRepository.disconnectAllDevicesExceptCurrent(user, currentDeviceId);
//
//        log.info("Disconnected {} devices for user {} (kept device {})",
//                disconnectedCount, user.getUsername(), currentDeviceId);
//
//        return disconnectedCount;
//    }


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
     * @param fingerprint The device fingerprint
     * @param ipAddress The client IP address
     * @return The newly created device
     */
    private Device createNewDevice(User user, UserAgent agent, String fingerprint, String ipAddress) {
        boolean isFirstDevice = isFirstDevice(user);
        boolean shouldAutoConfirm = isFirstDevice && isRecentActivation(user);

        Device device = Device.builder()
                .user(user)
                .fingerprint(fingerprint)
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress(ipAddress)
                .location(IpLocationUtils.resolveLocationFromIp(ipAddress))

                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
                .confirmed(shouldAutoConfirm)
                .blacklisted(false)
                .build();

        UserAgentUtils.populateDeviceInfo(device, agent, httpServletRequest);

        if (isFirstDevice) {
            device.setFirstDeviceUsed(true);
        }

        saveDevice(device);
        log.info("New device created with ID {} for user {} (auto-confirmed: {})",
                device.getId(), user.getUsername(), shouldAutoConfirm);
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


    /**
     * Checks if a user's account was activated recently (within the last 10 minutes).
     *
     * <p>This method is used to determine if a first device login should be auto-confirmed
     * without sending a notification email. The 10-minute window provides a balance between:
     * <ul>
     *   <li>User experience: No unnecessary "new device" email right after registration</li>
     *   <li>Security: Short enough window to prevent abuse if account is compromised</li>
     * </ul>
     *
     * <p><strong>Use case:</strong><br>
     * User creates account → receives activation email → clicks activation link →
     * logs in for first time. If this happens within 10 minutes, the device is
     * auto-confirmed since it's clearly the legitimate user.
     *
     * <p><strong>Edge cases:</strong>
     * <ul>
     *   <li>If activatedAt is null (account not yet activated): returns false</li>
     *   <li>If activation happened more than 10 minutes ago: returns false (device needs confirmation)</li>
     * </ul>
     *
     * @param user The user to check activation time for
     * @return true if account was activated within the last 10 minutes, false otherwise
     */
    private boolean isRecentActivation(User user) {
        if (user.getActivatedAt() == null) {
            return false;
        }

        long minutesSinceActivation = Duration.between(
                user.getActivatedAt(), Instant.now()).toMinutes();

        return minutesSinceActivation <= 10;
    }
}