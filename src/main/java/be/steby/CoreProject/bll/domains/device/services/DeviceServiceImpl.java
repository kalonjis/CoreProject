package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.events.DeviceDetectedEvent;
import be.steby.CoreProject.bll.domains.device.events.DeviceTrustLevelChangedEvent;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserAgentAnalyzer userAgentAnalyzer;
    private final RequestContextService requestContextService;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // New services for DRY refactoring
    private final DeviceFingerprintService deviceFingerprintService;

    // =========================================================================
    // Public interface methods
    // =========================================================================

    @Override
    public Device getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new DoesntExistException("Device with ID " + id + " does not exist"));
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
        return getUserDevice(authenticatedUser);
    }

    @Override
    public List<Device> getUserDevice(User user) {
        return deviceRepository.findAllByUser(user);
    }

    @Override
    @Transactional
    public Device detectAndRegisterDevice(HttpServletRequest request, User user, boolean confirmDevice) {
        String userAgentString = request.getHeader("User-Agent");
        String ipAddress = IpLocationUtils.extractClientIp(request);

        // Use new fingerprint service
        String fingerprint = deviceFingerprintService.generateFingerprint(request, user.getId());

        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        Device device = deviceRepository.findByFingerprint(fingerprint)
                .map(existingDevice -> updateExistingDevice(user, existingDevice, ipAddress, confirmDevice))
                .orElseGet(() -> createNewDevice(user, agent, request, fingerprint, ipAddress, confirmDevice));

        publishDeviceDetectedEvent(device, user, request);
        return device;
    }

    @Override
    public Device detectCurrentDevice(HttpServletRequest request) {
        User user = userService.getAuthenticatedUser();
        return detectAndRegisterDevice(request, user, false);
    }

    @Override
    @Transactional
    public Device detectFromRequestContext(RequestContext requestContext, User user) {
        String userAgentString = requestContext.getUserAgent();
        String ipAddress = requestContext.getClientIp();

        // Use new fingerprint service with RequestContext
        String fingerprint = deviceFingerprintService.generateFingerprint(requestContext, user.getId());

        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        Device device = deviceRepository.findByFingerprint(fingerprint)
                .map(existingDevice -> updateExistingDevice(user, existingDevice, ipAddress, false))
                .orElseGet(() -> createNewDeviceFromContext(user, agent, requestContext, fingerprint, ipAddress));

        publishDeviceDetectedEventFromContext(device, user, requestContext);
        return device;
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
        deviceRepository.save(device);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new DeviceTrustLevelChangedEvent(
                device.getId(),
                level,
                oldLevel.name(),
                userService.getAuthenticatedUser(),
                device,
                requestContext
        ));

        log.info("Device trust level changed from {} to {} for device {}",
                oldLevel, level, device.getId());
    }

    @Override
    public void saveDevice(Device device) {
        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public Device confirmDevice(String token) {
        Device device = getDeviceByToken(token);
        device.setConfirmed(true);
        deviceRepository.save(device);

        log.info("Device {} confirmed for user {}", device.getId(), device.getUser().getUsername());
        return device;
    }

    @Override
    @Transactional
    public void rejectDevice(String token) {
        Device device = getDeviceByToken(token);
        device.setBlacklisted(true);
        device.setBlacklistedTime(Instant.now());
        deviceRepository.save(device);

        log.info("Device {} rejected and blacklisted for user {}", device.getId(), device.getUser().getUsername());
    }

    @Override
    public Long getTotalDevices() {
        return deviceRepository.count();
    }

    @Override
    @Transactional
    public void requestConfirmationLink(HttpServletRequest request) {
        Device currentDevice = detectCurrentDevice(request);
        User user = userService.getAuthenticatedUser();

        DeviceConfirmationToken token = deviceConfirmationTokenService.createDeviceConfirmationToken(
                user, currentDevice.getId());

        // Here you would typically send an email with the confirmation link
        log.info("Confirmation link requested for device {} of user {}",
                currentDevice.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public void disconnectDevice(Long deviceId, HttpServletRequest request) {
        Device currentDevice = detectCurrentDevice(request);
        User currentUser = currentDevice.getUser();
        Device deviceToDisconnect = getDeviceById(deviceId);

        if (!authenticatedUserOwnsDevice(deviceToDisconnect)) {
            throw new OwnershipException("Access denied: You can only disconnect your own devices.");
        }

        if (currentDevice.getId().equals(deviceToDisconnect.getId())) {
            throw new CurrentDeviceDisconnectionException(
                    "You cannot disconnect your current device remotely. Please use the logout function instead.");
        }

        refreshTokenService.revokeDeviceTokens(currentUser, deviceToDisconnect);
        deviceToDisconnect.setLoggedOut(true);
        deviceToDisconnect.setLogoutTime(Instant.now());
        deviceRepository.save(deviceToDisconnect);

        log.info("Device {} marked as disconnected for user {}",
                deviceId, currentUser.getUsername());
    }

    @Override
    @Transactional
    public void disconnectAllOtherDevices(HttpServletRequest request) {
        Device currentDevice = detectCurrentDevice(request);
        User currentUser = currentDevice.getUser();
        List<Device> userDevices = getMyDeviceList();

        log.info("Starting disconnection of all other devices for user {}",
                currentUser.getUsername());

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

        log.info("{} devices successfully disconnected for user {}",
                count, currentUser.getUsername());
    }

    // =========================================================================
    // Private helper methods
    // =========================================================================

    private Device createNewDevice(User user, UserAgent agent, HttpServletRequest request,
                                   String fingerprint, String ipAddress, boolean confirmDevice) {
        Device device = Device.builder()
                .user(user)
                .fingerprint(fingerprint)
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress(ipAddress)
                .location(IpLocationUtils.resolveLocationFromIp(ipAddress))
                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
                .confirmed(confirmDevice)
                .blacklisted(false)
                .build();

        // Use new UserAgentUtils for device info population
        UserAgentUtils.populateDeviceInfo(device, agent, request);

        if (isFirstDevice(user)) {
            device.setFirstDeviceUsed(true);
        }

        deviceRepository.save(device);
        log.info("New device created with ID {} for user {}", device.getId(), user.getUsername());

        return device;
    }

    private Device createNewDeviceFromContext(User user, UserAgent agent, RequestContext requestContext,
                                              String fingerprint, String ipAddress) {
        Device device = Device.builder()
                .user(user)
                .fingerprint(fingerprint)
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress(ipAddress)
                .location(IpLocationUtils.resolveLocationFromIp(ipAddress))
                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
                .confirmed(false)  // Never auto-confirm devices from context
                .blacklisted(false)
                .build();

        // Use new UserAgentUtils for device info population from RequestContext
        UserAgentUtils.populateDeviceInfo(device, agent, requestContext);

        if (isFirstDevice(user)) {
            device.setFirstDeviceUsed(true);
        }

        deviceRepository.save(device);
        log.info("New device created from context with ID {} for user {}", device.getId(), user.getUsername());

        return device;
    }

    private Device updateExistingDevice(User user, Device existingDevice, String ipAddress, boolean confirmDevice) {
        existingDevice.setLastSeen(Instant.now());
        existingDevice.setLastIpAddress(ipAddress);
        existingDevice.setLocation(IpLocationUtils.resolveLocationFromIp(ipAddress));

        if (confirmDevice) {
            existingDevice.setConfirmed(true);
        }

        deviceRepository.save(existingDevice);
        return existingDevice;
    }

    private void publishDeviceDetectedEvent(Device device, User user, HttpServletRequest request) {
        RequestContext requestContext = requestContextService.captureRequestContext(request);
        publishDeviceDetectedEventFromContext(device, user, requestContext);
    }

    private void publishDeviceDetectedEventFromContext(Device device, User user, RequestContext requestContext) {
        boolean isNewDevice = device.getFirstSeen().equals(device.getLastSeen());

        eventPublisher.publishEvent(new DeviceDetectedEvent(
                device,
                user,
                isNewDevice,
                device.isBlacklisted(),
                device.isConfirmed(),
                device.isFirstDeviceUsed(),
                requestContext
        ));
    }

    private Device getDeviceByToken(String token) {
        DeviceConfirmationToken deviceConfirmationToken = deviceConfirmationTokenService.getToken(token);
        deviceConfirmationTokenService.verifyTokenValidity(deviceConfirmationToken);
        deviceConfirmationTokenService.revokeToken(deviceConfirmationToken);
        Long deviceId = deviceConfirmationToken.getDeviceId();
        return getDeviceById(deviceId);
    }

    private boolean authenticatedUserOwnsDevice(Device device) {
        User auth = userService.getAuthenticatedUser();
        return device.getUser().getId().equals(auth.getId());
    }

    private void validateDeviceOwnership(Device device) {
        if (!authenticatedUserOwnsDevice(device)) {
            throw new OwnershipException("Access denied: You can only access your own devices.");
        }
    }

    private boolean isFirstDevice(User user) {
        List<Device> devices = getUserDevice(user);
        return devices == null || devices.isEmpty();
    }
}