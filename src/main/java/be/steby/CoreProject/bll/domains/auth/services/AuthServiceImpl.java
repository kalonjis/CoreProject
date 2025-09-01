package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountActivationException;
import be.steby.CoreProject.bll.domains.auth.exceptions.BlacklistedDeviceException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidCredentialsException;
import be.steby.CoreProject.bll.domains.auth.exceptions.AccountDisabledException;
import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.domains.auth.events.DeviceSecurityEvent;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.Device;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Override
    public User login(String username, String password, HttpServletRequest request) {
        // 1. User authentication with domain-specific exceptions
        User user = (User) loadUserByUsername(username);

        if (!user.isEnabled()) {
            if (!user.isEverActivated()) {
                throw new AccountActivationException("Your account has never been activated. Please check your email and follow the activation instructions.");
            } else {
                throw new AccountDisabledException("Your account has been disabled by an administrator. Please contact support for assistance.");
            }
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password. Please check your credentials and try again.");
        }

        // 2. Device detection and registration
        Device device = deviceService.detectAndRegisterDevice(request, user);

        // 3. Security check: Reject blacklisted devices
        if (device.isBlacklisted()) {
            RequestContext requestContext = requestContextService.captureRequestContext(request);

            // Publish security event for notification (email will be sent)
            eventPublisher.publishEvent(new DeviceSecurityEvent(
                    user,
                    device,
                    DeviceSecurityEvent.DeviceSecurityType.BLACKLISTED_DEVICE_ATTEMPT,
                    requestContext
            ));

            log.warn("Login attempt blocked - blacklisted device {} for user {}",
                    device.getId(), user.getUsername());

            // Fail the login with domain-specific exception
            throw new BlacklistedDeviceException("Access denied: This device has been blacklisted for security reasons. Check your email for instructions on how to restore access.");
        }

        // 4. Handle logged out devices
        if (device.isLoggedOut()) {
            device.setLoggedOut(false);
            deviceService.saveDevice(device);
        }

        // 5. Event publishing for successful login
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new UserLoggedInEvent(
                user, device, true, null, requestContext
        ));

        // Send notification for unconfirmed devices
        if (!device.isConfirmed()) {
            eventPublisher.publishEvent(new DeviceSecurityEvent(
                    user,
                    device,
                    DeviceSecurityEvent.DeviceSecurityType.UNCONFIRMED_DEVICE,
                    requestContext
            ));
        }

        // 6. Store device in request for controller access
        request.setAttribute("currentDevice", device);

        log.info("Login successful for user {} with device {}", user.getUsername(), device.getId());
        return user;
    }

    @Override
    public void logout(String refreshTokenCookie, HttpServletRequest request) {
        User user = null;
        Device device = null;

        try {
            // 1. Get user and device information for business logic
            user = getAuthenticatedUser();
            device = deviceService.detectCurrentDevice(request);

            // 2. Revoke refresh token if provided
            if (refreshTokenCookie != null) {
                revokeRefreshToken(refreshTokenCookie);
            }

            log.info("User logout successful for: {}", user.getUsername());

        } catch (Exception e) {
            log.warn("Error during logout process for user {}: {}",
                    user != null ? user.getUsername() : "unknown", e.getMessage());
        } finally {
            // 3. Always publish logout event for audit purposes
            publishLogoutEvent(user, device, request);
        }
    }

    @Override
    public User getAuthenticatedUser() {
        return userService.getAuthenticatedUser();
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userService.getUserByUsername(username);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Revokes refresh token during logout process.
     */
    private void revokeRefreshToken(String refreshTokenCookie) {
        try {
            String[] parts = refreshTokenCookie.split("\\.", 2);
            if (parts.length == 2) {
                Long tokenId = Long.parseLong(parts[0]);
                String tokenValue = parts[1];

                refreshTokenService.verifyToken(tokenId, tokenValue)
                        .ifPresent(token -> {
                            token.setRevoked(true);
                            refreshTokenService.saveToken(token);
                            log.debug("Refresh token {} revoked successfully during logout", tokenId);
                        });
            }
        } catch (Exception e) {
            log.warn("Error revoking refresh token during logout: {}", e.getMessage());
        }
    }

    /**
     * Publishes logout event for audit and cleanup purposes.
     */
    private void publishLogoutEvent(User user, Device device, HttpServletRequest request) {
        try {
            if (user != null) {
                RequestContext requestContext = requestContextService.captureRequestContext(request);
                eventPublisher.publishEvent(new UserLogoutEvent(user, device, requestContext));
                log.debug("Logout event published for user: {}", user.getUsername());
            }
        } catch (Exception e) {
            log.error("Error publishing logout event: {}", e.getMessage(), e);
        }
    }
}