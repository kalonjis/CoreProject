package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.tokens.DeviceTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    /**
     * Constructs a new {@link DeviceConfirmationTokenServiceImpl} using the provided token repository.
     *
     * @param deviceTokenRepository The repository specifically for refresh tokens,
     *                              qualified to ensure correct repository injection
     */
    public DeviceConfirmationTokenServiceImpl(
            @Qualifier("deviceTokenRepository")
            DeviceTokenRepository deviceTokenRepository,
            DeviceConfirmationAttemptServiceImpl attemptService, SecureTokenService secureTokenService) {
        super(deviceTokenRepository, DeviceConfirmationToken.class, secureTokenService);
        this.attemptService = attemptService;
    }

    /**
     * Creates a new refresh token for a user with the configured expiration time.
     *
     * @param user The user for whom to create the refresh token
     * @return The newly created refresh token
     */
    @Transactional
    public DeviceConfirmationToken createDeviceConfirmationToken(User user, Long deviceId) {
        if (attemptService.hasExceededAttempts(user, deviceId)) {
            throw new MaxAttemptsReachedException("Too many attempts. Please try again later.");
        }
        attemptService.recordAttempt(user, deviceId);
        DeviceConfirmationToken token = super.createToken(user, TokenType.DEVICE_CONFIRMATION, deviceconfirmationTokenDurationMs, false);
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