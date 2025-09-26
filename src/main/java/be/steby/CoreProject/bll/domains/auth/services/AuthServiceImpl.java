package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.account.exceptions.AccountActivationException;
import be.steby.CoreProject.bll.domains.auth.events.UserLoginFailedEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.AccountTemporarilyLockedException;
import be.steby.CoreProject.bll.domains.auth.exceptions.BlacklistedDeviceException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidCredentialsException;
import be.steby.CoreProject.bll.domains.auth.exceptions.AccountDisabledException;
import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.domains.device.events.DeviceSecurityEvent;
import be.steby.CoreProject.bll.domains.auth.services.login_attempt.LoginAttemptService;
import be.steby.CoreProject.bll.domains.device.services.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;

    // ✅ NEW: Login attempt service for brute force protection
    private final LoginAttemptService loginAttemptService;

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Override
    public User login(String username, String password, HttpServletRequest request) {

        // Extract client IP for brute force protection
        String clientIpAddress = IpLocationUtils.extractClientIp(request);

        User user = null;
        Device device = null;

        try {
            // 1. Load user and create/detect device first
            user = (User) loadUserByUsername(username); // May throw DoesntExistException
            device = deviceService.detectAndRegisterDevice(request, user);

            // 2. Check if login attempts are blocked BEFORE password validation
            if (loginAttemptService.isBlocked(username, clientIpAddress)) {
                Instant unlockTime = loginAttemptService.getUnlockTime(username, clientIpAddress);
                String message = buildLockoutMessage(unlockTime);

                log.warn("Login blocked - brute_force_protection - Username: {}, IP: {}", username, clientIpAddress);
                throw new AccountTemporarilyLockedException(message);
            }

            // 3. User account validation
            if (!user.isEnabled()) {
                if (!user.isEverActivated()) {
                    throw new AccountActivationException("Your account has never been activated. Please check your email and follow the activation instructions.");
                } else {
                    throw new AccountDisabledException("Your account has been disabled.");
                }
            }

            // 4. Password validation - CRITICAL: Do this BEFORE checking device blacklist
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new InvalidCredentialsException("Invalid username or password. Please check your credentials and try again.");
            }

            // 5. Security check: Reject blacklisted devices (AFTER successful password validation)
            if (device.isBlacklisted()) {
                // Publish security events for notification (email will be sent)
                eventPublisher.publishEvent(new DeviceSecurityEvent(
                        user,
                        device,
                        DeviceSecurityEvent.DeviceSecurityType.BLACKLISTED_DEVICE_ATTEMPT
                ));

                log.warn("Login attempt blocked - blacklisted device {} for user {}",
                        device.getId(), user.getUsername());

                throw new BlacklistedDeviceException("Access denied: This device has been blacklisted for security reasons. Check your email for instructions on how to restore access.");
            }

            // 6. Handle logged out devices
            if (device.isLoggedOut()) {
                device.setLoggedOut(false);
                deviceService.saveDevice(device);
            }

            // 7. Clear failed attempts on successful login
            loginAttemptService.clearFailedAttempts(username, clientIpAddress);

            // 8. ✅ Publish success event - simplified
            eventPublisher.publishEvent(new UserLoggedInEvent(user, device));

            // 9. Send notification for unconfirmed devices
            if (!device.isConfirmed()) {
                eventPublisher.publishEvent(new DeviceSecurityEvent(
                        user,
                        device,
                        DeviceSecurityEvent.DeviceSecurityType.UNCONFIRMED_DEVICE));
            }

            // 10. Store device in request for controller access
            request.setAttribute("currentDevice", device);

            log.info("Login successful for user {} with device {} from IP {}",
                    user.getUsername(), device.getId(), clientIpAddress);
            return user;

        } catch (DoesntExistException e) {
            loginAttemptService.recordFailedAttempt(username, clientIpAddress);

            log.debug("Login failed for non-existent user: {} from IP: {}", username, clientIpAddress);
            throw e;

        } catch (CoreProjectException e) {
            // ✅ Existing user but failure - PUBLISH failure event
            // Important for security: someone is trying to crack a known account

            if (!(e instanceof AccountTemporarilyLockedException)) {
                loginAttemptService.recordFailedAttempt(username, clientIpAddress);
            }

            // Publish failure event for known users only
            if (user != null) {
                eventPublisher.publishEvent(new UserLoginFailedEvent(user, device, e.getMessage()));
            }

            log.warn("Login failed for existing user: {} from IP: {} - Reason: {}",
                    user != null ? user.getUsername() : username, clientIpAddress, e.getMessage());
            throw e;
        }
    }


    @Override
    public void logout(String refreshTokenCookie, HttpServletRequest request) {
        User user = null;
        Device device = null;

        try {
            user = getAuthenticatedUser();
            device = deviceService.detectCurrentDevice(request);

            if (refreshTokenCookie != null) {
                revokeRefreshToken(refreshTokenCookie);
            }

            revokeDeviceConfirmationTokens(user);

            eventPublisher.publishEvent(new UserLogoutEvent(user, device));

            log.info("User logout successful for: {}", user.getUsername());


        } catch (Exception e) {
            log.warn("Error during logout process for user {}: {}",
                    user != null ? user.getUsername() : "unknown", e.getMessage());
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
     * Revokes all device confirmation tokens for a user during logout process.
     * This ensures that any pending device confirmation tokens are invalidated when the user logs out.
     */
    private void revokeDeviceConfirmationTokens(User user) {
        try {
            deviceConfirmationTokenService.revokeAllUserTokens(user);
            log.debug("Device confirmation tokens revoked for user: {}", user.getUsername());
        } catch (Exception e) {
            log.warn("Error revoking device confirmation tokens for user {}: {}",
                    user.getUsername(), e.getMessage());
        }
    }




    // Helper method to build lockout message
    private String buildLockoutMessage(Instant unlockTime) {
        if (unlockTime == null) {
            return "Account temporarily locked due to too many failed login attempts. Please try again later.";
        }

        Instant now = Instant.now();
        if (unlockTime.isBefore(now)) {
            return "Account temporarily locked due to too many failed login attempts. Please try again.";
        }

        long minutesUntilUnlock = ChronoUnit.MINUTES.between(now, unlockTime);
        if (minutesUntilUnlock < 1) {
            long secondsUntilUnlock = ChronoUnit.SECONDS.between(now, unlockTime);
            return String.format("Account temporarily locked due to too many failed login attempts. Try again in %d seconds.", secondsUntilUnlock);
        } else {
            return String.format("Account temporarily locked due to too many failed login attempts. Try again in %d minutes.", minutesUntilUnlock);
        }
    }
}
