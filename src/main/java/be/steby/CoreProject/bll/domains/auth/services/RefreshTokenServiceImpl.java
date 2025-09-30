package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidRefreshTokenException;
import be.steby.CoreProject.dal.repositories.tokens.RefreshTokenRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;


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
public class RefreshTokenServiceImpl extends BaseTokenServiceImpl<RefreshToken> {

    @Value("${security.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    /**
     * Constructs a new {@link RefreshTokenServiceImpl} using the provided token repository.
     *
     * @param refreshTokenRepository The repository specifically for refresh tokens,
     *                              qualified to ensure correct repository injection
     */
    public RefreshTokenServiceImpl(
            @Qualifier("refreshTokenRepository") RefreshTokenRepository refreshTokenRepository, SecureTokenService secureTokenService ) {
        super(refreshTokenRepository, RefreshToken.class, secureTokenService);
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for a user with the configured expiration time.
     *
     * @param user The user for whom to create the refresh token
     * @return The newly created refresh token
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, Device device) {
        revokeDeviceTokens(user, device);

        RefreshToken token = super.createToken(user, TokenType.REFRESH_TOKEN, refreshTokenDurationMs, false);
        token.setDevice(device);
        saveToken(token);
        return token;
    }


    /**
     * Validates a refresh token using userId, deviceId, and token value.
     * This method is specifically designed for cookie-based authentication
     * where the cookie format is: userId.deviceId.tokenValue
     *
     * @param tokenValue The raw token value from the cookie
     * @param userId The user ID from the cookie
     * @param deviceId The device ID from the cookie
     * @return The validated RefreshToken entity
     * @throws InvalidRefreshTokenException if token is invalid, expired, revoked, or doesn't match user/device
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenValue, long userId, long deviceId) {
        log.debug("Validating refresh token for userId: {}, deviceId: {}", userId, deviceId);

        // Find token by value (using internal method since it's already in plain text from cookie)
        RefreshToken token = getInternalValidToken(tokenValue, TokenType.REFRESH_TOKEN);

        // Verify token belongs to the correct user
        if (token.getUser().getId() != userId) {
            log.warn("Token user mismatch. Expected userId: {}, got: {}", userId, token.getUser().getId());
            throw new InvalidRefreshTokenException("Token does not belong to specified user");
        }

        // Verify token belongs to the correct device
        if (token.getDevice() == null || token.getDevice().getId() != deviceId) {
            log.warn("Token device mismatch. Expected deviceId: {}, got: {}",
                    deviceId, token.getDevice() != null ? token.getDevice().getId() : "null");
            throw new InvalidRefreshTokenException("Token does not belong to specified device");
        }

        log.debug("Refresh token validated successfully for userId: {}, deviceId: {}", userId, deviceId);
        return token;
    }


    @Transactional
    public RefreshToken rotateToken(RefreshToken oldToken) {
        return createRefreshToken(oldToken.getUser(), oldToken.getDevice());
    }


    @Transactional
    public int getRefreshTokenDurationInSeconds() {
        long durationInSeconds = refreshTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("Refresh token duration too large for cookie max age");
        }
        return (int) durationInSeconds;
    }


    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return refreshTokenRepository.countActiveUsers(Instant.now());
    }


    @Transactional
    public void revokeDeviceTokens(User user, Device device) {
        // ✅ 1 seule query UPDATE groupée
        int revokedCount = refreshTokenRepository.revokeAllByUserAndDevice(user, device);

        log.info("Révocation de {} tokens pour l'appareil {} de l'utilisateur {}",
                revokedCount, device.getId(), user.getUsername());
    }
}