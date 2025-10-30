package be.steby.CoreProject.bll.domains.auth.services.oauth;

import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidOAuth2ProviderException;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.enums.OAuthProvider;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service implementation for handling OAuth2 authentication operations.
 * Orchestrates the complete OAuth login flow.
 *
 * Flow:
 * 1. Validate and convert provider to enum
 * 2. Find or create user via OAuthAccountService
 * 3. Detect and register device
 * 4. Generate JWT tokens
 * 5. Publish login event
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthServiceImpl implements OAuthService {

    private final OAuthAccountService oauthAccountService;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LoginTokens oauth2Login(String provider, String providerId, String email,
                                   String username, String name, HttpServletRequest request) {
        log.info("OAuth2 login attempt - provider: {}, username: {}", provider, username);

        try {
            // 1. Convert and validate OAuth provider
            OAuthProvider oauthProvider = convertToOAuthProvider(provider);

            // 2. Find or create user (delegates to OAuthAccountService)
            User user = oauthAccountService.findOrCreateUserForOAuth(
                    oauthProvider,
                    providerId,
                    email,
                    username,
                    name
            );

            // 3. Detect or register device
            Device device = deviceService.detectAndRegisterDevice(request, user);

            // 4. Handle device state (reactivate if logged out)
            handleDeviceState(device);

            // 5. Generate tokens
            LoginTokens tokens = generateTokens(user, device);

            // 6. Publish login event
            eventPublisher.publishEvent(new UserLoggedInEvent(user, device));

            log.info("OAuth2 login successful - user: {}, provider: {}", user.getUsername(), provider);
            return tokens;

        } catch (InvalidOAuth2ProviderException e) {
            log.error("Invalid OAuth2 provider - provider: {}", provider);
            throw e;
        } catch (Exception e) {
            log.error("OAuth2 login failed - username: {}, provider: {}", username, provider, e);
            throw new RuntimeException("OAuth2 authentication failed", e);
        }
    }

    /**
     * Converts string provider to OAuthProvider enum.
     * Throws InvalidOAuth2ProviderException if provider is not supported.
     */
    private OAuthProvider convertToOAuthProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidOAuth2ProviderException.unsupportedProvider(provider);
        }
    }

    /**
     * Reactivates device if it was logged out.
     */
    private void handleDeviceState(Device device) {
        if (device.isLoggedOut()) {
            device.setLoggedOut(false);
            deviceService.saveDevice(device);
            log.debug("Device {} reactivated after logout", device.getId());
        }
    }

    /**
     * Generates access and refresh tokens for successful login.
     */
    private LoginTokens generateTokens(User user, Device device) {
        String accessToken = jwtUtil.generateAccessToken(user, device);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, device);

        return new LoginTokens(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                device.getId()
        );
    }
}