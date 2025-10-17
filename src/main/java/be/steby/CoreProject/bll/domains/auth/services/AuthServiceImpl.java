package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.account.exceptions.AccountActivationException;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorVerificationRequestedEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.*;
import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.domains.auth.models.LoginInitiationResult;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorSessionInfo;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorTokenClaims;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.TwoFactorVerificationService;
import be.steby.CoreProject.bll.domains.device.events.DeviceSecurityEvent;
import be.steby.CoreProject.bll.domains.auth.services.login_attempt.LoginAttemptService;
import be.steby.CoreProject.bll.domains.device.services.tokens.confirmation.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UserAuthenticationService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.domains.user.exceptions.UsernameNotFoundAuthenticationException;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import io.jsonwebtoken.Claims;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserAuthenticationService userAuthenticationService;
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final TwoFactorVerificationService twoFactorVerificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;
    private final JwtUtil jwtUtil;


    // ✅ NEW: Login attempt service for brute force protection
    private final LoginAttemptService loginAttemptService;

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Override
    public LoginTokens login(String username, String password, HttpServletRequest request) {
        String clientIpAddress = IpLocationUtils.extractClientIp(request);

        try {
            // 1. Load user and detect device
            User user = loadUser(username);
            Device device = deviceService.detectAndRegisterDevice(request, user);

            // 2. Validate all login preconditions
            validateLoginPreconditions(username, clientIpAddress, user, device, password);

            // 3. Handle device state (logged out devices)
            handleDeviceState(device);

            // 4. Clear failed attempts on successful validation
            loginAttemptService.clearFailedAttempts(username, clientIpAddress);

            // 5. Generate authentication tokens
            LoginTokens tokens = generateTokens(user, device);

            // 6. Publish success events and notifications
            publishSuccessEvents(user, device);

            log.info("Login successful for user {} with device {} from IP {}",
                    user.getUsername(), device.getId(), clientIpAddress);

            return tokens;

        } catch (DoesntExistException e) {
            handleNonExistentUser(username, clientIpAddress);
            throw e;

        } catch (CoreProjectException e) {
            handleAuthenticationFailure(username, clientIpAddress, e);
            throw e;
        }
    }


    public LoginTokens verifyTwoFactorAndCompleteLogin(String twoFactorToken, String verificationCode, HttpServletRequest httpRequest) {
        log.info("Verifying 2FA code for login completion");

        // 1. Validate and extract 2FA token
        Claims claims = jwtUtil.validate2FAToken(twoFactorToken);
        TwoFactorTokenClaims twoFactorClaims = extractTwoFactorClaims(claims);

        // 2. Verify the provided code against the hashed code from token
        boolean isValidCode = twoFactorVerificationService.verifyCode(
                verificationCode,
                twoFactorClaims.verificationCode()
        );

        if (!isValidCode) {
            log.warn("Invalid 2FA verification code provided");
            throw new InvalidTwoFactorCodeException("Invalid verification code");
        }

        // 3. Load user and complete login
        User user = userService.getUserById(Long.parseLong(twoFactorClaims.userId()));
        Device device = deviceService.detectAndRegisterDevice(httpRequest, user);

        // 4. Update 2FA success tracking
        Optional<TwoFactorAuth> twoFactorAuth = twoFactorAuthRepository.findByUserAndTypeAndEnabledTrue(
                user,
                twoFactorClaims.twoFactorType()
        );

        if (twoFactorAuth.isPresent()) {
            twoFactorAuth.get().recordSuccessfulVerification();
            twoFactorAuthRepository.save(twoFactorAuth.get());
        }

        // 5. Generate final login tokens
        LoginTokens tokens = generateTokens(user, device);

        // 6. Publish successful login event
        eventPublisher.publishEvent(new UserLoggedInEvent(user, device));

        log.info("2FA verification successful - login completed for user: {}", user.getUsername());
        return tokens;
    }


    public void resendTwoFactorCode(String twoFactorToken, HttpServletRequest httpRequest) {
        log.info("Resending 2FA verification code");

        // 1. Validate token and extract claims
        Claims claims = jwtUtil.validate2FAToken(twoFactorToken);
        TwoFactorTokenClaims twoFactorClaims = extractTwoFactorClaims(claims);

        // 2. Load user
        User user = userService.getUserById(Long.parseLong(twoFactorClaims.userId()));

        // 3. Generate new verification code
        String newVerificationCode = twoFactorVerificationService.generateVerificationCode();

        // 4. Publish resend event
        eventPublisher.publishEvent(new TwoFactorVerificationRequestedEvent(
                user,
                twoFactorClaims.twoFactorType(),
                newVerificationCode,
                httpRequest
        ));

        log.info("2FA verification code resent for user: {}", user.getUsername());
    }

    @Override
    public TwoFactorSessionInfo getTwoFactorSessionInfo(String twoFactorToken) {
        // 1. Validate token and extract claims
        Claims claims = jwtUtil.validate2FAToken(twoFactorToken);
        TwoFactorTokenClaims twoFactorClaims = extractTwoFactorClaims(claims);

        // 2. Calculate time remaining
        long currentTime = System.currentTimeMillis();
        long timeRemaining = Math.max(0, twoFactorClaims.expiresAt() - currentTime);

        return new TwoFactorSessionInfo(
                twoFactorClaims.userId(),
                twoFactorClaims.username(),
                twoFactorClaims.twoFactorType(),
                maskEmail(twoFactorClaims.email()),
                timeRemaining / 1000, // Convert to seconds
                3 // TODO: Track actual attempts remaining
        );
    }



    @Override
    public LoginInitiationResult initiateLogin(String username, String password, HttpServletRequest httpRequest) {
        log.info("Initiating login for username: {}", username);
        String clientIpAddress = IpLocationUtils.extractClientIp(httpRequest);

        try {
            // 1. Load user and detect device (COMME DANS LOGIN EXISTANT)
            User user = loadUser(username);
            Device device = deviceService.detectAndRegisterDevice(httpRequest, user);

            // 2. Validate all login preconditions (COMME DANS LOGIN EXISTANT)
            validateLoginPreconditions(username, clientIpAddress, user, device, password);

            // 3. Check if user has 2FA enabled
            Optional<TwoFactorAuth> primaryTwoFactor = twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user);

            if (primaryTwoFactor.isEmpty() || !primaryTwoFactor.get().getEnabled()) {
                // No 2FA - complete login immediately (COMME DANS LOGIN EXISTANT)
                log.info("No 2FA required for user: {}", username);

                // Handle device state
                handleDeviceState(device);

                // Clear failed attempts on successful validation
                loginAttemptService.clearFailedAttempts(username, clientIpAddress);

                // Generate authentication tokens
                LoginTokens tokens = generateTokens(user, device);

                // Publish success events and notifications
                publishSuccessEvents(user, device);

                return LoginInitiationResult.completeLogin(tokens);
            }

            // 4. 2FA required - generate verification code and JWT token
            TwoFactorAuth twoFactorAuth = primaryTwoFactor.get();
            log.info("2FA required for user: {} with type: {}", username, twoFactorAuth.getType());

            // Handle device state et clear failed attempts (même si 2FA requis, les credentials sont ok)
            handleDeviceState(device);
            loginAttemptService.clearFailedAttempts(username, clientIpAddress);

            // Generate verification code
            String verificationCode = twoFactorVerificationService.generateVerificationCode();
            String verificationCodeHash = twoFactorVerificationService.hashVerificationCode(verificationCode);

            // Generate 2FA JWT token
            String twoFactorToken = jwtUtil.generate2FAToken(user, verificationCodeHash, twoFactorAuth.getType());

            // Publish 2FA verification event (triggers email sending)
            eventPublisher.publishEvent(new TwoFactorVerificationRequestedEvent(
                    user,
                    twoFactorAuth.getType(),
                    verificationCode,
                    httpRequest
            ));

            String maskedEmail = maskEmail(user.getEmail());
            return LoginInitiationResult.requiresTwoFactor(twoFactorAuth.getType(), twoFactorToken, maskedEmail);

        } catch (DoesntExistException e) {
            handleNonExistentUser(username, clientIpAddress);
            throw e;
        } catch (CoreProjectException e) {
            handleAuthenticationFailure(username, clientIpAddress, e);
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
    public LoginTokens refreshAuthTokens(RefreshToken validatedToken) {
        log.debug("Refreshing auth tokens for user: {} on device: {}",
                validatedToken.getUser().getUsername(),
                validatedToken.getDevice().getId());

        // Rotate refresh token
        RefreshToken newRefreshToken = refreshTokenService.rotateToken(validatedToken);

        String newAccessToken = jwtUtil.generateAccessToken(
                newRefreshToken.getUser(),
                newRefreshToken.getDevice()
        );

        return new LoginTokens(
                newAccessToken,
                newRefreshToken.getToken(),
                newRefreshToken.getUser().getId(),
                newRefreshToken.getDevice().getId()
        );
    }



    @Override
    public UserDetails loadUserByUsername(String username) {
        return userAuthenticationService.loadUserByUsernameWithCache(username);
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


    // =========================================================================
// Private Helper Methods - User Loading
// =========================================================================

    /**
     * Loads user by username.
     *
     * @param username Username to load
     * @return User entity
     * @throws UsernameNotFoundAuthenticationException if user not found
     */
    private User loadUser(String username) {
        return (User) loadUserByUsername(username);
    }

// =========================================================================
// Private Helper Methods - Validation
// =========================================================================

    /**
     * Validates all login preconditions in the correct order:
     * 1. Brute force protection
     * 2. Account status
     * 3. Password validation
     * 4. Device security
     *
     * @throws AccountTemporarilyLockedException if account is locked
     * @throws AccountActivationException if account not activated
     * @throws AccountDisabledException if account is disabled
     * @throws InvalidCredentialsException if password is invalid
     * @throws BlacklistedDeviceException if device is blacklisted
     */
    private void validateLoginPreconditions(String username, String clientIpAddress,
                                            User user, Device device, String password) {
        // 1. Check brute force protection FIRST
        validateBruteForceProtection(username, clientIpAddress);

        // 2. Validate account status
        validateAccountStatus(user);

        // 3. Validate password - CRITICAL: Before device check
        validatePassword(password, user);

        // 4. Validate device security - AFTER password validation
        validateDeviceSecurity(user, device);
    }

    /**
     * Checks if login attempts are blocked due to brute force protection.
     */
    private void validateBruteForceProtection(String username, String clientIpAddress) {
        if (loginAttemptService.isBlocked(username, clientIpAddress)) {
            Instant unlockTime = loginAttemptService.getUnlockTime(username, clientIpAddress);
            String message = buildLockoutMessage(unlockTime);

            log.warn("Login blocked - brute force protection - Username: {}, IP: {}",
                    username, clientIpAddress);
            throw new AccountTemporarilyLockedException(message);
        }
    }

    /**
     * Validates account is enabled and activated.
     */
    private void validateAccountStatus(User user) {
        if (!user.isEnabled()) {
            if (!user.isEverActivated()) {
                throw new AccountActivationException(
                        "Your account has never been activated. Please check your email and follow the activation instructions."
                );
            }
            throw new AccountDisabledException("Your account has been disabled.");
        }
    }

    /**
     * Validates password matches.
     */
    private void validatePassword(String password, User user) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException(
                    "Invalid username or password. Please check your credentials and try again."
            );
        }
    }

    /**
     * Validates device is not blacklisted.
     * Publishes security event if blacklisted device attempts login.
     */
    private void validateDeviceSecurity(User user, Device device) {
        if (device.isBlacklisted()) {
            // Publish security event for notification
            eventPublisher.publishEvent(new DeviceSecurityEvent(
                    user,
                    device,
                    DeviceSecurityEvent.DeviceSecurityType.BLACKLISTED_DEVICE_ATTEMPT
            ));

            log.warn("Login attempt blocked - blacklisted device {} for user {}",
                    device.getId(), user.getUsername());

            throw new BlacklistedDeviceException(
                    "Access denied: This device has been blacklisted for security reasons. " +
                            "Check your email for instructions on how to restore access."
            );
        }
    }

// =========================================================================
// Private Helper Methods - Device Management
// =========================================================================

    /**
     * Handles device state for logged out devices.
     * Reactivates device if it was previously logged out.
     */
    private void handleDeviceState(Device device) {
        if (device.isLoggedOut()) {
            device.setLoggedOut(false);
            deviceService.saveDevice(device);
            log.debug("Device {} reactivated after logout", device.getId());
        }
    }

// =========================================================================
// Private Helper Methods - Token Generation
// =========================================================================

    /**
     * Generates access and refresh tokens for successful login.
     *
     * @param user Authenticated user
     * @param device User's device
     * @return LoginTokens containing access token and refresh token data
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


    /**
     * Extracts TwoFactorTokenClaims from JWT claims
     */
    private TwoFactorTokenClaims extractTwoFactorClaims(Claims claims) {
        return new TwoFactorTokenClaims(
                claims.get("userId", String.class),
                claims.get("username", String.class),
                claims.get("email", String.class),
                TwoFactorType.valueOf(claims.get("twoFactorType", String.class)),
                claims.get("verificationCodeHash", String.class),
                claims.getIssuedAt().getTime(),
                claims.getExpiration().getTime()
        );
    }

    /**
     * Masks email for display purposes
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        // Mask local part: show first and last character
        String maskedLocal = localPart.length() > 2
                ? localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1)
                : "***";

        return maskedLocal + "@" + domain;
    }

// =========================================================================
// Private Helper Methods - Event Publishing
// =========================================================================

    /**
     * Publishes success events and notifications after successful login.
     * - Always publishes UserLoggedInEvent for activity logging
     * - Publishes DeviceSecurityEvent if device is unconfirmed
     */
    private void publishSuccessEvents(User user, Device device) {
        // Publish login success event for activity logging
        eventPublisher.publishEvent(new UserLoggedInEvent(user, device));

        // Notify user if device is not confirmed
        if (!device.isConfirmed()) {
            eventPublisher.publishEvent(new DeviceSecurityEvent(
                    user,
                    device,
                    DeviceSecurityEvent.DeviceSecurityType.UNCONFIRMED_DEVICE
            ));
        }
    }

// =========================================================================
// Private Helper Methods - Failure Handling
// =========================================================================

    /**
     * Handles login failure for non-existent users.
     * Records failed attempt to prevent username enumeration attacks.
     */
    private void handleNonExistentUser(String username, String clientIpAddress) {
        loginAttemptService.recordFailedAttempt(username, clientIpAddress);
        log.debug("Login failed - non-existent user: {} from IP: {}", username, clientIpAddress);
    }

    /**
     * Handles authentication failures for existing users.
     * Records failed attempt unless account is already locked.
     * Publishes failure event for security monitoring.
     */
    private void handleAuthenticationFailure(String username, String clientIpAddress,
                                             CoreProjectException e) {
        // Don't record attempt if account is already locked (avoid double-counting)
        if (!(e instanceof AccountTemporarilyLockedException)) {
            loginAttemptService.recordFailedAttempt(username, clientIpAddress);
        }

        log.warn("Login failed for user: {} from IP: {} - Reason: {}",
                username, clientIpAddress, e.getMessage());
    }
}
