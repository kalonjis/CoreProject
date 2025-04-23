package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.events.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.OwnershipException;
import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.security.SecurityService;
import be.steby.CoreProject.bll.services.security.impl.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.utils.DeviceDetectionUtils;
import be.steby.CoreProject.bll.utils.IpUtils;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {


    private final DeviceRepository deviceRepository;
    private final SecurityService securityService;
    private final UserAgentAnalyzer userAgentAnalyzer;
    private final MailerService mailerService;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${security.device-confirmation.alert.threshold-minutes}")
    private long deviceConfirmationAlertThresholdMinutes;


    // =========================================================================
    // Méthodes de récupération des appareils
    // =========================================================================

    @Override
    public Device getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new DoesntExistException("Device with id " + id + " not found"));
    }

    @Override
    public Device getMyDevice(Long deviceId) {
        Device device = getDeviceById(deviceId);
        if (!authenticatedUserOwnsDevice(device)) {
            throw new OwnershipException("Access denied: You can only access to your own devices.");
        }
        return device;
    }

    @Override
    public List<Device> getMyDeviceList() {
        User user = securityService.getAuthenticatedUser();
        return deviceRepository.findAllByUser(user);
    }

    @Override
    public List<Device> getUserDevice(User user) {
        return deviceRepository.findAllByUser(user);
    }

    // =========================================================================
    // Méthodes de détection et enregistrement des appareils
    // =========================================================================

    @Override
    @Transactional
    public Device detectAndRegisterDevice(HttpServletRequest request, User user, boolean notifyNewDevice ) {
        String userAgentString = request.getHeader("User-Agent");
        String ipAddress = IpUtils.getClientIp(request);
        String fingerprint = DeviceDetectionUtils.generateFingerprint(request, user.getId());
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        return deviceRepository.findByFingerprint(fingerprint)
                .map(device -> updateExistingDevice(user, device, ipAddress, notifyNewDevice ))
                .orElseGet(() -> createNewDevice(user, agent, request, fingerprint, ipAddress, notifyNewDevice ));
    }

    @Override
    public Device detectCurrentDevice(HttpServletRequest request) {
        User user = securityService.getAuthenticatedUser();
        Device device = detectAndRegisterDevice(request, user, false);
        return device;
    }

    // =========================================================================
    // Méthodes de gestion des niveaux de confiance
    // =========================================================================

    @Transactional
    @Override
    public void updateTrustLevel(Long deviceId, DeviceTrustLevel newTrustLevel) {
        Device device = getDeviceById(deviceId);
        validateDeviceOwnership(device);
        validateTrustLevelChange(device, newTrustLevel);

        device.setDeviceTrustLevel(newTrustLevel);
        deviceRepository.save(device);

        eventPublisher.publishEvent(new DeviceTrustLevelChangedEvent(deviceId, newTrustLevel));
    }

    private void validateTrustLevelChange(Device device, DeviceTrustLevel newLevel) {
        if (device.getDeviceTrustLevel() == newLevel) {
            throw new AttributeUnchangedException("This device is already considered as " + newLevel);
        }
    }

    @Override
    public void saveDevice(Device device) {
        deviceRepository.save(device);
    }

    // =========================================================================
    // Méthodes de confirmation et rejet d'appareil
    // =========================================================================

    @Transactional
    @Override
    public Device confirmDevice(String token) {
        Device device = getDeviceByToken(token);
        device.setDeviceTrustLevel(DeviceTrustLevel.BASIC);
        device.setConfirmed(true);
        device.setBlacklisted(false);
        deviceRepository.save(device);
        return device;
    }

    @Transactional
    @Override
    public void rejectDevice(String token) {
        Device device = getDeviceByToken(token);
        device.setDeviceTrustLevel(DeviceTrustLevel.UNTRUSTED);
        device.setConfirmed(false);
        device.setBlacklisted(true);
        deviceRepository.save(device);
    }


    @Override
    public void requestConfirmationLink(HttpServletRequest request) {
        User auth = securityService.getAuthenticatedUser();
        detectAndRegisterDevice(request, auth, true);
    }

    @Override
    public Long getTotalDevices() {
        return deviceRepository.count();
    }

    // =========================================================================
    // Méthodes utilitaires privées
    // =========================================================================

    private Device getDeviceByToken(String token) {
        DeviceConfirmationToken deviceConfirmationToken = deviceConfirmationTokenService.getToken(token);
        deviceConfirmationTokenService.verifyTokenValidity(deviceConfirmationToken);
        deviceConfirmationTokenService.revokeToken(deviceConfirmationToken);
        Long deviceId = deviceConfirmationToken.getDeviceId();
        return getMyDevice(deviceId);
    }

    private boolean authenticatedUserOwnsDevice(Device device) {
        User auth = securityService.getAuthenticatedUser();
        return device.getUser().getId().equals(auth.getId());
    }

    private void validateDeviceOwnership(Device device) {
        if (!authenticatedUserOwnsDevice(device)) {
            throw new OwnershipException("Access denied: You can only update your own devices.");
        }
    }

    private Device updateExistingDevice(User user, Device device, String ipAddress, boolean notifyNewDevice) {
        System.out.println("Device under update");

        // On n'envoie une notification que si explicitement demandé (notifyNewDevice=true)
        if (notifyNewDevice) {
            boolean shouldSendNewDeviceAlert = true; // Par défaut, on envoie si notifyNewDevice=true

            // Cas 1: appareil blacklisté → pas d'alerte standard (gestion spécifique plus bas)
            if (device.isBlacklisted()) {
                shouldSendNewDeviceAlert = false;
                DeviceConfirmationToken confirmationToken = deviceConfirmationTokenService.createDeviceConfirmationToken(user, device.getId());
                mailerService.sendBlacklistedDeviceAlert(user, device, confirmationToken.getToken());
            }
            // Cas 2: premier appareil → vérifier le délai écoulé depuis l'activation
            if (device.isFirstDeviceUsed() && !device.isConfirmed()) {
                // Si moins de 10 minutes depuis l'activation, on n'envoie pas d'alerte
                if (calculateTimeFromActivationInMinutes(user) <= 10) {
                    shouldSendNewDeviceAlert = false;
                }
            }

            // On n'envoie que si toutes les conditions sont satisfaites
            if (shouldSendNewDeviceAlert) {
                sendNewDeviceAlert(device);
            }
        }

        // Mise à jour des informations de l'appareil (inchangé)
        device.setLastSeen(Instant.now());
        device.setLastIpAddress(ipAddress);
        device.setLocation(IpUtils.getLocationFromIp(ipAddress));
        return deviceRepository.save(device);
    }

    private Device createNewDevice(User user, UserAgent agent, HttpServletRequest request, String fingerprint, String ipAddress, boolean notifyNewDevice ) {
        System.out.println("Device under creation");
        Device device = Device.builder()
                .user(user)
                .fingerprint(fingerprint)
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress(ipAddress)
                .location(IpUtils.getLocationFromIp(ipAddress))
                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
                .confirmed(false)
                .blacklisted(false)
                .build();

        // Remplir les informations détaillées de l'appareil
        DeviceDetectionUtils.populateDeviceInfo(device, agent, request);

        if(isFirstDevice(user)){
            device.setFirstDeviceUsed(true);
        }


        deviceRepository.save(device);

        if(notifyNewDevice ){
            sendNewDeviceAlert(device);
        }
        return device;
    }

    private long calculateTimeFromActivationInMinutes(User user) {
        return Duration.between(user.getActivatedAt(), Instant.now()).toMinutes();
    }

    private void sendNewDeviceAlert(Device device){
        User user = device.getUser();
        DeviceConfirmationToken confirmationToken = deviceConfirmationTokenService.createDeviceConfirmationToken(
                user, device.getId());

        mailerService.sendNewDeviceAlert(user, device, confirmationToken.getToken());
    }

    private boolean isFirstDevice(User user){
        List<Device> devices = getUserDevice(user);
        return devices == null || devices.isEmpty();
    }
}
