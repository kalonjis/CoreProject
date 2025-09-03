package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.account.exceptions.AccountActivationException;
import be.steby.CoreProject.bll.domains.auth.exceptions.AccountTemporarilyLockedException;
import be.steby.CoreProject.bll.domains.auth.exceptions.BlacklistedDeviceException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidCredentialsException;
import be.steby.CoreProject.bll.domains.auth.exceptions.AccountDisabledException;
import be.steby.CoreProject.bll.domains.auth.events.DeviceSecurityEvent;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.domains.auth.services.login_attempt.LoginAttemptService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
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
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    // ✅ AJOUT: Injection d'AuthActivityLogService
    private final AuthActivityLogService authActivityLogService;

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Override
    public User login(String username, String password, HttpServletRequest request) {
        // ✅ NEW: Extract client IP for brute force protection
        String clientIpAddress = IpLocationUtils.extractClientIp(request);

        // ✅ NEW: Check if login attempts are blocked BEFORE any authentication
        if (loginAttemptService.isBlocked(username, clientIpAddress)) {
            Instant unlockTime = loginAttemptService.getUnlockTime(username, clientIpAddress);
            String message = buildLockoutMessage(unlockTime);

            log.warn("Login blocked - brute_force_protection - Username: {}, IP: {}", username, clientIpAddress);

            throw new AccountTemporarilyLockedException(message);
        }

        try {
            // 1. User authentication with domain-specific exceptions
            User user = (User) loadUserByUsername(username);

            if (!user.isEnabled()) {
                if (!user.isEverActivated()) {
                    // Record failed attempt before throwing
                    loginAttemptService.recordFailedAttempt(username, clientIpAddress);

                    // ✅ CORRECTION: Log failed login avec AuthActivityLogService
                    log.debug("🔴 [LOGIN_FAILED] Case 1: Account never activated - Username: {}, logging failed login attempt", username);
                    RequestContext requestContext = requestContextService.captureRequestContext(request);
                    authActivityLogService.logFailedLogin(user, null, "Account never activated", requestContext);
                    log.debug("✅ [LOGIN_FAILED] Case 1: Logged successfully for never activated account");

                    throw new AccountActivationException("Your account has never been activated. Please check your email and follow the activation instructions.");
                } else {
                    // Record failed attempt before throwing
                    loginAttemptService.recordFailedAttempt(username, clientIpAddress);

                    // ✅ CORRECTION: Log failed login avec AuthActivityLogService
                    log.debug("🔴 [LOGIN_FAILED] Case 2: Account disabled by admin - Username: {}, logging failed login attempt", username);
                    RequestContext requestContext = requestContextService.captureRequestContext(request);
                    authActivityLogService.logFailedLogin(user, null, "Account disabled by administrator", requestContext);
                    log.debug("✅ [LOGIN_FAILED] Case 2: Logged successfully for disabled account");

                    throw new AccountDisabledException("Your account has been disabled by an administrator. Please contact support for assistance.");
                }
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                // ✅ NEW: Record failed attempt on invalid password
                loginAttemptService.recordFailedAttempt(username, clientIpAddress);

                // ✅ CORRECTION: Log failed login avec AuthActivityLogService
                log.debug("🔴 [LOGIN_FAILED] Case 3: Invalid credentials - Username: {}, logging failed login attempt", username);
                RequestContext requestContext = requestContextService.captureRequestContext(request);
                authActivityLogService.logFailedLogin(user, null, "Invalid credentials", requestContext);
                log.debug("✅ [LOGIN_FAILED] Case 3: Logged successfully for invalid credentials");

                throw new InvalidCredentialsException("Invalid username or password. Please check your credentials and try again.");
            }

            // 2. Device detection and registration
            Device device = deviceService.detectAndRegisterDevice(request, user);

            // 3. Security check: Reject blacklisted devices
            if (device.isBlacklisted()) {
                RequestContext requestContext = requestContextService.captureRequestContext(request);

                // Publish security events for notification (email will be sent)
                eventPublisher.publishEvent(new DeviceSecurityEvent(
                        user,
                        device,
                        DeviceSecurityEvent.DeviceSecurityType.BLACKLISTED_DEVICE_ATTEMPT,
                        requestContext
                ));

                log.warn("Login attempt blocked - blacklisted device {} for user {}",
                        device.getId(), user.getUsername());

                // ✅ NEW: Record failed attempt for blacklisted device
                loginAttemptService.recordFailedAttempt(username, clientIpAddress);

                // ✅ CORRECTION: Log failed login avec AuthActivityLogService
                log.debug("🔴 [LOGIN_FAILED] Case 4: Blacklisted device - Username: {}, Device: {}, logging failed login attempt", username, device.getId());
                authActivityLogService.logFailedLogin(user, device, "Blacklisted device", requestContext);
                log.debug("✅ [LOGIN_FAILED] Case 4: Logged successfully for blacklisted device");

                // Fail the login with domain-specific exception
                throw new BlacklistedDeviceException("Access denied: This device has been blacklisted for security reasons. Check your email for instructions on how to restore access.");
            }

            // 4. Handle logged out devices
            if (device.isLoggedOut()) {
                device.setLoggedOut(false);
                deviceService.saveDevice(device);
            }

            // 5. ✅ CORRECTION: Log successful login avec AuthActivityLogService au lieu d'événements
            RequestContext requestContext = requestContextService.captureRequestContext(request);
            log.debug("🟢 [LOGIN_SUCCESS] Username: {}, Device: {}, logging successful login", username, device.getId());
            authActivityLogService.logSuccessfulLogin(user, device, requestContext);
            log.debug("✅ [LOGIN_SUCCESS] Logged successfully for successful login");

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

            // ✅ NEW: Clear failed attempts on successful login
            loginAttemptService.clearFailedAttempts(username, clientIpAddress);

            log.info("Login successful for user {} with device {} from IP {}",
                    user.getUsername(), device.getId(), clientIpAddress);
            return user;

        } catch (Exception e) {
            log.debug("🔥 [CATCH_BLOCK] Exception caught: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.debug("🔥 [CATCH_BLOCK] Checking if exception needs additional logging...");

            // ✅ NEW: Ensure failed attempts are recorded for any authentication failure
            // Only record if not already recorded above
            if (!(e instanceof InvalidCredentialsException) &&
                    !(e instanceof AccountActivationException) &&
                    !(e instanceof AccountDisabledException) &&
                    !(e instanceof BlacklistedDeviceException)) {

                log.debug("🔴 [LOGIN_FAILED] Case 5: Generic authentication error - Username: {}, Exception: {}", username, e.getClass().getSimpleName());

                loginAttemptService.recordFailedAttempt(username, clientIpAddress);

                // ✅ CORRECTION: Log des échecs génériques
                try {
                    User user = userService.getUserByUsername(username);
                    if (user != null) {
                        log.debug("🔴 [LOGIN_FAILED] Case 5: User found, logging generic authentication error");
                        RequestContext requestContext = requestContextService.captureRequestContext(request);
                        authActivityLogService.logFailedLogin(user, null, "Authentication error: " + e.getMessage(), requestContext);
                        log.debug("✅ [LOGIN_FAILED] Case 5: Logged successfully for generic authentication error");
                    } else {
                        log.debug("⚠️ [LOGIN_FAILED] Case 5: User not found, skipping logging");
                    }
                } catch (Exception logException) {
                    log.error("Failed to log authentication failure: {}", logException.getMessage());
                }
            } else {
                log.debug("⏭️ [CATCH_BLOCK] Exception already handled above ({}), skipping additional logging", e.getClass().getSimpleName());
            }

            // Re-throw the original exception
            throw e;
        }
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

            // 3. ✅ CORRECTION: Log logout avec AuthActivityLogService au lieu d'événements
            if (user != null) {
                RequestContext requestContext = requestContextService.captureRequestContext(request);
                authActivityLogService.logLogout(user, device, requestContext);
            }

            log.info("User logout successful for: {}", user != null ? user.getUsername() : "unknown");

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
     * ✅ NEW: Helper method to build lockout message
     */
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