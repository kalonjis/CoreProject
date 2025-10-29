package be.steby.CoreProject.bll.domains.auth.services.oauth;

import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for handling OAuth2 authentication operations.
 * Manages user authentication and registration through OAuth2 providers (GitHub, Google, etc.).
 *
 * Business logic:
 * 1. Email reconciliation: If email is provided, attempts to find existing user by email
 * 2. Provider reconciliation: If no email match, checks for existing OAuth provider credentials
 * 3. User creation: Creates new user if no match found
 * 4. Device detection: Registers or retrieves user's device
 * 5. Token generation: Creates access and refresh tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthServiceImpl implements OAuthService {

    private final UserService userService;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LoginTokens oauth2Login(String provider, String providerId, String email,
                                   String username, String name, HttpServletRequest request) {
        log.info("OAuth2 login attempt with provider: {} for username: {}", provider, username);

        try {
            User user;

            // 1. Try to find existing user using UserService reconciliation
            Optional<User> existingUser = userService.findUserForOAuthReconciliation(provider, providerId, email);

            if (existingUser.isPresent()) {
                user = existingUser.get();
                log.info("Found existing user: {}", user.getUsername());

                // Update OAuth provider info if not already set
                if (user.getOauthProvider() == null || user.getOauthProviderId() == null) {
                    user.setOauthProvider(provider);
                    user.setOauthProviderId(providerId);
                    userService.saveUser(user);
                    log.info("Updated OAuth provider info for user: {}", user.getUsername());
                }
            } else {
                // 2. Create new user
                user = createOAuthUser(provider, providerId, email, username, name);
            }

            // 3. Detect or register device
            Device device = deviceService.detectAndRegisterDevice(request, user);

            // 4. Handle device state (reactivate if logged out)
            handleDeviceState(device);

            // 5. Generate tokens
            LoginTokens tokens = generateTokens(user, device);

            // 6. Publish login event
            eventPublisher.publishEvent(new UserLoggedInEvent(user, device));

            log.info("OAuth2 login successful for user {} with provider {}", user.getUsername(), provider);
            return tokens;

        } catch (Exception e) {
            log.error("OAuth2 login failed for username: {} with provider: {}", username, provider, e);
            throw new RuntimeException("OAuth2 authentication failed", e);
        }
    }

    /**
     * Creates a new user from OAuth2 provider data.
     * Generates temporary email if not provided by OAuth provider.
     */
    private User createOAuthUser(String provider, String providerId, String email,
                                 String username, String name) {
        log.info("Creating new OAuth2 user with provider: {}, providerId: {}", provider, providerId);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setOauthProvider(provider);
        newUser.setOauthProviderId(providerId);

        // Parse name if provided
        if (name != null && !name.isBlank()) {
            String[] nameParts = name.trim().split(" ", 2);
            newUser.setFirstname(nameParts[0]);
            if (nameParts.length > 1) {
                newUser.setLastname(nameParts[1]);
            }
        }

        // Email handling: use provided email or generate temporary one
        if (email == null || email.isBlank()) {
            String temporaryEmail = username + "@" + provider.toLowerCase() + ".oauth.local";
            newUser.setEmail(temporaryEmail);
            newUser.setEmailVerified(false);
            log.warn("No email provided by OAuth provider, using temporary email: {}", temporaryEmail);
        } else {
            newUser.setEmail(email);
            newUser.setEmailVerified(true);
        }

        // Set default user properties
        newUser.setEnabled(true);
        newUser.setUserRoles(UserRole.setRoles(UserRole.USER));
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Random password
        newUser.setMustChangePassword(false);
        newUser.setEverActivated(true);
        newUser.setActivatedAt(Instant.now());

        User savedUser = userService.saveUser(newUser);
        log.info("New OAuth2 user created successfully: {}", savedUser.getUsername());

        return savedUser;
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