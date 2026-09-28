package be.steby.CoreProject.bll.domains.device.services.tokens.confirmation;

import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.common.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.tokens.DeviceTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


/**
 * Service implementation for managing refresh tokens. This service extends {@link BaseTokenServiceImpl}
 * and specifically handles operations related to JWT refresh tokens.
 * It provides functionality for creating, rotating, and verifying refresh tokens with proper
 * security measures.
 *
 * @see BaseTokenServiceImpl
 */
@Service
@Slf4j
public class DeviceConfirmationTokenServiceImpl extends BaseTokenServiceImpl<DeviceConfirmationToken> {

    @Value("${security.device-confirmation.token.expiration}")
    private Long deviceconfirmationTokenDurationMs;

    private final DeviceConfirmationAttemptServiceImpl attemptService;
    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceConfirmationTokenServiceImpl(
            @Qualifier("deviceTokenRepository") DeviceTokenRepository deviceTokenRepository,
            DeviceConfirmationAttemptServiceImpl attemptService,
            SecureTokenService secureTokenService) {
        super(deviceTokenRepository, DeviceConfirmationToken.class, secureTokenService);
        this.attemptService = attemptService;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Creates a device confirmation token, reusing a still-valid one for this
     * device if it exists instead of revoking and reminting it — avoids
     * invalidating a link already sent by email when a second login/request
     * comes in for the same unconfirmed device before it expires.
     */
    @Transactional
    public DeviceConfirmationToken createDeviceConfirmationToken(User user, Long deviceId) {
        if (attemptService.hasExceededAttempts(user, deviceId)) {
            throw new MaxAttemptsReachedException("Too many attempts. Please try again later.");
        }

        attemptService.recordAttempt(user, deviceId);

        DeviceConfirmationToken existingToken = deviceTokenRepository
                .findValidByUserIdAndDeviceId(user.getId(), deviceId, Instant.now())
                .orElse(null);
        if (existingToken != null) {
            log.debug("Reusing existing device confirmation token for device {} of user {}",
                    deviceId, user.getUsername());
            return existingToken;
        }

        int revokedCount = deviceTokenRepository.revokeAllByUserAndDevice(user.getId(), deviceId);
        if (revokedCount > 0) {
            log.debug("Revoked {} existing device confirmation token(s) for device {} of user {}",
                    revokedCount, deviceId, user.getUsername());
        }

        DeviceConfirmationToken token = super.createToken(
                user,
                TokenType.DEVICE_CONFIRMATION,
                deviceconfirmationTokenDurationMs,
                false
        );

        token.setDeviceId(deviceId);
        saveToken(token);
        return token;
    }
    @Transactional
    public int getDeviceConfirmationTokenDurationInSeconds() {
        long durationInSeconds = deviceconfirmationTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("device-confirmation token duration too large");
        }
        return (int) durationInSeconds;
    }
}