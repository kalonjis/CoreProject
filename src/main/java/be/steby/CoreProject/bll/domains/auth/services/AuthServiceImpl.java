package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.exceptions.mail.MailDeliveryException;
import be.steby.CoreProject.bll.common.exceptions.phone.SmsSendingException;
import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.account.exceptions.AccountActivationException;
import be.steby.CoreProject.bll.domains.auth.events.*;
import be.steby.CoreProject.bll.domains.auth.exceptions.*;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorCodeDeliveryException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.models.*;
import be.steby.CoreProject.bll.domains.auth.services.jwt.AuthJwtService;
import be.steby.CoreProject.bll.domains.auth.services.login_attempt.LoginAttemptService;
import be.steby.CoreProject.bll.domains.auth.services.login_attempt.LoginAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.auth.services.notifications.email.AuthMailerService;
import be.steby.CoreProject.bll.domains.auth.services.notifications.sms.AuthSmsService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.TwoFactorFactory;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.jwt.TwoFactorJwtService;
import be.steby.CoreProject.bll.domains.device.events.DeviceSecurityEvent;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.device.services.tokens.confirmation.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.exceptions.UsernameNotFoundAuthenticationException;
import be.steby.CoreProject.bll.domains.user.services.UserAuthenticationService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.RateLimitExceededException;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.TwoFactorType;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserAuthenticationService userAuthenticationService;
    private final TwoFactorFactory twoFactorFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;
    private final AuthJwtService authJwtService;
    private final TwoFactorJwtService twoFactorJwtService;
    private final AuthMailerService authMailerService;
    private final AuthSmsService authSmsService;
    private final LoginAttemptService loginAttemptService;

    @Value("${url.front_server}")
    private String FRONT_URL;


    // =========================================================================
    // Public API — Login (legacy endpoint, no 2FA)
    // =========================================================================

    @Override
    public LoginTokens login(String username, String password, HttpServletRequest httpRequest) {
        String clientIpAddress = IpLocationUtils.extractClientIp(httpRequest);

        // FIX #3 — early IP block publishes an audit event before throwing
        if (loginAttemptService.isIpBlocked(clientIpAddress)) {
            Instant unlockTime = loginAttemptService.getUnlockTimeForIp(clientIpAddress);
            eventPublisher.publishEvent(new LoginBlockedEvent(null, null,
                    "IP blocked: " + clientIpAddress));
            throw new RateLimitExceededException(
                    "Too many login attempts from your network. Please try again later.",
                    unlockTime);
        }

        User user = null;
        Device device = null;

        try {
            user = loadUser(username);
            device = deviceService.detectAndRegisterDevice(user);

            validateLoginPreconditions(username, clientIpAddress, user, device, password);

            handleDeviceState(device);
            loginAttemptService.clearFailedAttempts(username, clientIpAddress);

            LoginTokens tokens = generateTokens(user, device);
            publishSuccessEvents(user, device);

            log.info("Login successful for user {} with device {} from IP {}",
                    user.getUsername(), device.getId(), clientIpAddress);
            return tokens;

        } catch (DoesntExistException | UsernameNotFoundAuthenticationException e) {
            handleNonExistentUser(username, clientIpAddress);
            throw new InvalidCredentialsException("Invalid username or password.");

        } catch (CoreProjectException e) {
            handleAuthenticationFailure(user, device, e);
            throw e;
        }
    }


    // =========================================================================
    // Public API — Phase 1 : initiateLogin (credentials + 2FA check)
    // =========================================================================

    @Override
    public LoginInitiationResult initiateLogin(String username, String password,
                                               HttpServletRequest httpRequest) {
        log.info("Initiating login for username: {}", username);
        String clientIpAddress = IpLocationUtils.extractClientIp(httpRequest);

        if (loginAttemptService.isIpBlocked(clientIpAddress)) {
            Instant unlockTime = loginAttemptService.getUnlockTimeForIp(clientIpAddress);
            eventPublisher.publishEvent(new LoginBlockedEvent(null, null,
                    "IP blocked: " + clientIpAddress));
            throw new RateLimitExceededException(
                    "Too many login attempts from your network. Please try again later.",
                    unlockTime);
        }

        User user = null;
        Device device = null;

        try {
            user = loadUser(username);
            device = deviceService.detectAndRegisterDevice(user);

            validateLoginPreconditions(username, clientIpAddress, user, device, password);

            boolean has2FA = twoFactorFactory.hasTwoFactorEnabled(user);

            handleDeviceState(device);
            loginAttemptService.clearFailedAttempts(username, clientIpAddress);

            if (!has2FA) {
                LoginTokens tokens = generateTokens(user, device);
                publishSuccessEvents(user, device);
                log.info("Login completed without 2FA for user: {}", username);
                return LoginInitiationResult.loginComplete(tokens);
            } else {
                String sessionToken = twoFactorJwtService.generateSessionToken(user);
                log.info("2FA required for user: {}", username);
                return LoginInitiationResult.requiresTwoFactor(sessionToken);
            }

        } catch (DoesntExistException | UsernameNotFoundAuthenticationException e) {
            handleNonExistentUser(username, clientIpAddress);
            throw new InvalidCredentialsException("Invalid username or password.");

        } catch (CoreProjectException e) {
            handleAuthenticationFailure(user, device, e);
            throw e;
        }
    }


    // =========================================================================
    // Public API — 2FA methods
    // =========================================================================

    @Override
    public List<TwoFactorAuth> getEnabledTwoFactorMethods(String twoFactorSessionToken) {
        Claims claims = twoFactorJwtService.validateSessionToken(twoFactorSessionToken);
        String publicId = claims.get("publicId", String.class);
        User user = userService.getUserByPublicId(publicId);
        return twoFactorFactory.getEnabledTwoFactorMethods(user);
    }

    @Override
    public TwoFactorMethodChosenResult chooseTwoFactorMethod(String twoFactorSessionToken,
                                                             TwoFactorType chosenType,
                                                             HttpServletRequest httpRequest) {
        log.info("Processing 2FA method choice: {}", chosenType);

        Claims claims = twoFactorJwtService.validateSessionToken(twoFactorSessionToken);
        String publicId = claims.get("publicId", String.class);
        User user = userService.getUserByPublicId(publicId);

        if (!twoFactorFactory.isMethodEnabled(user, chosenType)) {
            log.warn("User {} attempted to choose unavailable 2FA method: {}",
                    user.getUsername(), chosenType);
            throw new TwoFactorNotEnabledException("Chosen two-factor method is not enabled");
        }

        String verificationCodeHash = null;
        String plainCode = null;
        boolean codeGenerated = false;

        if (chosenType != TwoFactorType.BACKUP_CODES && chosenType != TwoFactorType.TOTP) {
            CodeGenerationResult codeResult = twoFactorFactory.generateCodeForType(user, chosenType);
            verificationCodeHash = codeResult.hashedCode();
            plainCode = codeResult.plainCode();
            codeGenerated = true;

            try {
                sendVerificationCodeSync(user, chosenType, plainCode, httpRequest);
            } catch (MailDeliveryException | SmsSendingException e) {
                log.warn("Failed to send 2FA code via {}: {}", chosenType, e.getMessage());
                List<TwoFactorType> alternatives = getAvailableAlternatives(user, chosenType);
                return TwoFactorMethodChosenResult.deliveryFailed(
                        chosenType,
                        "Unable to send verification code. Please try another method.",
                        alternatives
                );
            }
        }

        String twoFactorToken = twoFactorJwtService.generateVerificationToken(
                user, verificationCodeHash, chosenType);
        String maskedTarget = getMaskedTarget(user, chosenType);

        log.info("2FA method chosen for user: {} — method: {}", user.getUsername(), chosenType);
        return TwoFactorMethodChosenResult.success(twoFactorToken, chosenType, codeGenerated, maskedTarget);
    }

    @Override
    public LoginTokens verifyTwoFactorAndCompleteLogin(String twoFactorToken,
                                                       String verificationCode,
                                                       String backupCode,
                                                       HttpServletRequest httpRequest) {
        log.info("Verifying 2FA code for login completion");

        Claims claims = twoFactorJwtService.validateVerificationToken(twoFactorToken);
        TwoFactorTokenClaims twoFactorClaims = extractTwoFactorClaims(claims);

        User user = userService.getUserByPublicId(twoFactorClaims.publicId());
        Device device = deviceService.detectAndRegisterDevice(user);

        String codeToVerify;
        if (twoFactorClaims.twoFactorType() == TwoFactorType.BACKUP_CODES) {
            if (backupCode == null) {
                throw new InvalidTwoFactorCodeException("Backup code is required for this verification");
            }
            codeToVerify = backupCode;
        } else {
            if (verificationCode == null) {
                throw new InvalidTwoFactorCodeException("Verification code is required for this verification");
            }
            codeToVerify = verificationCode;
        }

        boolean isValidCode = twoFactorFactory.verifyTwoFactorCode(
                user,
                codeToVerify,
                twoFactorClaims.verificationCode(),
                twoFactorClaims.twoFactorType()
        );

        if (!isValidCode) {
            eventPublisher.publishEvent(new TwoFactorFailedEvent(
                    user, device, twoFactorClaims.twoFactorType(), "Invalid verification code"));
            log.warn("Invalid 2FA code for user: {} (type: {})",
                    user.getUsername(), twoFactorClaims.twoFactorType());
            throw new InvalidTwoFactorCodeException("Invalid verification code");
        }

        LoginTokens tokens = generateTokens(user, device);
        eventPublisher.publishEvent(new UserLoggedInEvent(user, device));

        log.info("2FA verification successful ({}) — login completed for user: {}",
                twoFactorClaims.twoFactorType(), user.getUsername());
        return tokens;
    }

    @Override
    public void resendTwoFactorCode(String twoFactorToken, HttpServletRequest httpRequest) {
        log.info("Resending 2FA verification code");

        Claims claims = twoFactorJwtService.validateSessionToken(twoFactorToken);
        TwoFactorTokenClaims twoFactorClaims = extractTwoFactorClaims(claims);
        TwoFactorType twoFactorType = twoFactorClaims.twoFactorType();

        if (twoFactorType == TwoFactorType.TOTP || twoFactorType == TwoFactorType.BACKUP_CODES) {
            throw new IllegalStateException("Cannot resend code for " + twoFactorType);
        }

        User user = userService.getUserByPublicId(twoFactorClaims.publicId());
        String newCode = twoFactorFactory.generateCode(user);

        try {
            sendVerificationCodeSync(user, twoFactorType, newCode, httpRequest);
            log.info("2FA code resent for user: {}", user.getUsername());
        } catch (MailDeliveryException | SmsSendingException e) {
            log.warn("Failed to resend 2FA code via {}: {}", twoFactorType, e.getMessage());
            List<TwoFactorType> alternatives = getAvailableAlternatives(user, twoFactorType);
            throw new TwoFactorCodeDeliveryException(
                    "Unable to send verification code. Please try another method.",
                    twoFactorType, alternatives, e);
        }
    }

    @Override
    public TwoFactorSessionInfo getTwoFactorSessionInfo(String twoFactorToken) {
        Claims claims = twoFactorJwtService.validateSessionToken(twoFactorToken);
        TwoFactorTokenClaims twoFactorClaims = extractTwoFactorClaims(claims);

        long timeRemaining = Math.max(0, twoFactorClaims.expiresAt() - System.currentTimeMillis());
        return new TwoFactorSessionInfo(
                twoFactorClaims.publicId(),
                twoFactorClaims.username(),
                twoFactorClaims.twoFactorType(),
                maskEmail(twoFactorClaims.email()),
                timeRemaining / 1000,
                3
        );
    }


    // =========================================================================
    // Public API — Session / Tokens
    // =========================================================================

    @Override
    public void logout(String refreshTokenCookie, HttpServletRequest request) {
        User user = null;
        Device device = null;

        try {
            user = getAuthenticatedUser();
            device = deviceService.detectCurrentDevice();

            if (refreshTokenCookie != null) {
                revokeRefreshToken(refreshTokenCookie);
            }
            revokeDeviceConfirmationTokens(user);

            eventPublisher.publishEvent(new UserLogoutEvent(user, device));
            log.info("Logout successful for user: {}", user.getUsername());

        } catch (Exception e) {
            log.warn("Error during logout for user {}: {}",
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
                validatedToken.getUser().getUsername(), validatedToken.getDevice().getId());

        RefreshToken newRefreshToken = refreshTokenService.rotateToken(validatedToken);
        String newAccessToken = authJwtService.generateAccessToken(
                newRefreshToken.getUser(), newRefreshToken.getDevice());

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

    @Override
    public List<TwoFactorAuth> getAvailableTwoFactorMethods(User user) {
        return twoFactorFactory.getEnabledTwoFactorMethods(user);
    }

    @Override
    public List<TwoFactorAuth> getAllTwoFactorMethodsWithStatus(User user) {
        return twoFactorFactory.getAllMethodsWithStatus(user);
    }


    // =========================================================================
    // Private — Validation pipeline
    // =========================================================================

    /**
     * Validates all login preconditions in security-correct order.
     *
     * ORDER RATIONALE:
     *   1. Password first  → an attacker with the wrong password always gets a generic
     *      "Invalid credentials" and gains zero information about account state.
     *   2. Post-auth brute-force check → lock message is only shown when the password
     *      is correct, so a legitimate locked-out user gets a helpful unlock time
     *      while an attacker just keeps seeing "Invalid credentials".
     *   3. Account status  → activation / disabled messages are safe here because
     *      we already know the caller has the right password.
     *   4. Device security → blacklist check last, after identity is fully confirmed.
     */
    private void validateLoginPreconditions(String username, String clientIpAddress,
                                            User user, Device device, String password) {
        // FIX #2 — password is validated FIRST
        validatePassword(password, user);

        // FIX #4 — brute-force/lock check happens AFTER password is confirmed correct;
        //          only then can we safely reveal an informative lockout message
        validateBruteForceProtectionPostAuth(username, clientIpAddress);

        // FIX #6 — account status (activation / disabled) is revealed only to callers
        //          who already proved they know the password
        validateAccountStatus(user);

        validateDeviceSecurity(user, device);
    }

    /**
     * Validates the supplied password against the stored hash.
     * Throws a deliberately generic exception on failure to avoid leaking
     * any information about whether the username exists or what the account state is.
     */
    private void validatePassword(String password, User user) {
        if (!user.hasPassword()) {
            // OAuth-only account — don't reveal account state to unknown callers
            log.info("Credential login attempt on OAuth-only account: {}", user.getUsername());
            String provider = extractOauthProvider(user.getEmail());
            throw new OAuthOnlyAccountException(
                    "This account was created with " + provider +
                            ". Please use the same provider to sign in, or set a password in your account settings."
            );
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            // Keep the message identical whether the username doesn't exist, the password
            // is wrong, or any other pre-auth failure — no enumeration possible.
            throw new InvalidCredentialsException("Invalid username or password.");
        }
    }

    /**
     * Post-authentication brute-force check.
     * Called only after password has been validated, so:
     *  - ACCOUNT_LOCKED  → informative message with unlock time (safe: caller proved identity)
     *  - IP_BLOCKED      → defensive guard (should already be caught at entry; 429)
     *  - COMBINED_LOCKED → stay generic; too ambiguous to reveal details
     */
    private void validateBruteForceProtectionPostAuth(String username, String clientIpAddress) {
        LoginAttemptServiceImpl.BlockReason reason =
                loginAttemptService.getBlockReason(username, clientIpAddress);

        if (reason == LoginAttemptServiceImpl.BlockReason.NOT_BLOCKED) return;

        Instant unlockTime = loginAttemptService.getUnlockTime(username, clientIpAddress);

        switch (reason) {
            case ACCOUNT_LOCKED -> {
                // Password was correct → safe to give a helpful lockout message
                String message = buildLockoutMessage(unlockTime);
                throw new AccountTemporarilyLockedException(message, 423);
            }
            case IP_BLOCKED -> {
                // Defensive: already checked at entry point, but guard here too
                throw new RateLimitExceededException(
                        "Too many login attempts from your network. Please try again later.", unlockTime, 429);
            }
            case COMBINED_BLOCKED -> {
                // Both username and IP are blocked — stay vague
                throw new InvalidCredentialsException("Invalid username or password.");
            }
        }
    }

    /**
     * Validates account enabled / activation state.
     * Only called after password has been verified, so the detailed messages
     * are exposed solely to legitimate users who know their credentials.
     */
    private void validateAccountStatus(User user) {
        if (!user.isEnabled()) {
            if (!user.isEverActivated()) {
                throw new AccountActivationException("ACCOUNT_NOT_ACTIVATED");
            }

            // Self-deactivated → peut se réactiver
            if (user.isSelfDeactivated()) {
                DeactivationReason reason = user.getDeactivationReason();
                if (reason != null && reason.allowsReactivation()) {
                    throw new AccountDisabledException("ACCOUNT_SELF_DEACTIVATED");
                } else {
                    throw new AccountDisabledException("ACCOUNT_PERMANENTLY_DELETED"); // GDPR
                }
            }

            // Admin deactivated
            if (user.isAdminDeactivated()) {
                if (user.getAdminDeactivationReason().allowsReactivation()) {
                    throw new AccountDisabledException("ACCOUNT_ADMIN_DEACTIVATED_REACTIVABLE");
                } else {
                    throw new AccountDisabledException("ACCOUNT_ADMIN_DEACTIVATED_PERMANENT");
                }
            }

            throw new AccountDisabledException("ACCOUNT_DISABLED");
        }
    }

    /**
     * Validates the device is not blacklisted.
     * Publishes a security event to notify the user via email/push.
     */
    private void validateDeviceSecurity(User user, Device device) {
        if (device.isBlacklisted()) {
            eventPublisher.publishEvent(new DeviceSecurityEvent(
                    user, device, DeviceSecurityEvent.DeviceSecurityType.BLACKLISTED_DEVICE_ATTEMPT));
            log.warn("Login blocked — blacklisted device {} for user {}",
                    device.getId(), user.getUsername());
            throw new BlacklistedDeviceException(
                    "Access denied: this device has been blacklisted. " +
                            "Check your email for instructions on how to restore access."
            );
        }
    }


    // =========================================================================
    // Private — Failure handling
    // =========================================================================

    /**
     * Records a failed attempt for an unrecognised username.
     * We still call recordFailedAttempt on the IP to count cross-account spray attacks,
     * but we never surface any information about whether the account exists.
     */
    private void handleNonExistentUser(String username, String clientIpAddress) {
        // Record on IP only — no username-based counter for non-existent users
        loginAttemptService.recordFailedAttempt(username, clientIpAddress);
        log.debug("Login failed — unknown username: {} from IP: {}", username, clientIpAddress);
    }

    /**
     * Central failure handler for authenticated-user failures.
     *
     * Decision matrix (with new validation order):
     *
     *  AccountTemporarilyLockedException
     *      → password was correct, account was already locked at post-auth check
     *      → just emit LoginBlockedEvent; no additional recording
     *
     *  AccountActivationException / AccountDisabledException
     *      → password was correct (these are thrown post-password-validation)
     *      → do NOT penalise with a failed attempt (legitimate user)
     *      → emit UserLoginFailedEvent for audit
     *
     *  Everything else (wrong password, blacklisted device, combined lock, …)
     *      → record ONE failed attempt  (FIX #1: was recorded TWICE before)
     *      → check whether this attempt just crossed the lock threshold
     *      → emit appropriate events
     */
    private void handleAuthenticationFailure(User user, Device device, CoreProjectException e) {

        if (e instanceof AccountTemporarilyLockedException) {
            eventPublisher.publishEvent(new LoginBlockedEvent(user, device, e.getMessage()));
            return;
        }

        if (user == null || device == null) {
            log.warn("handleAuthenticationFailure called with null user or device — reason: {}",
                    e.getMessage());
            return;
        }

        if (e instanceof AccountDisabledException || e instanceof AccountActivationException) {
            // Password was correct → no failed-attempt penalty, just log
            eventPublisher.publishEvent(new UserLoginFailedEvent(user, device, e.getMessage()));
            return;
        }

        // FIX #1 — record exactly ONE failed attempt (previously was called twice)
        loginAttemptService.recordFailedAttempt(user.getUsername(), device.getLastIpAddress());

        // Publish generic failed-login event for activity logging
        eventPublisher.publishEvent(new UserLoginFailedEvent(user, device, e.getMessage()));

        // If this attempt just crossed the lock threshold, publish the lock event
        if (loginAttemptService.isBlocked(user.getUsername(), device.getLastIpAddress())) {
            Instant unlockTime = loginAttemptService.getUnlockTime(
                    user.getUsername(), device.getLastIpAddress());
            eventPublisher.publishEvent(new AccountLockedEvent(user, device, unlockTime));
        }

        log.warn("Login failed for user: {} from IP: {} — reason: {}",
                user.getUsername(), device.getLastIpAddress(), e.getMessage());
    }


    // =========================================================================
    // Private — Success helpers
    // =========================================================================

    private void publishSuccessEvents(User user, Device device) {
        eventPublisher.publishEvent(new UserLoggedInEvent(user, device));

        if (!device.isConfirmed()) {
            eventPublisher.publishEvent(new DeviceSecurityEvent(
                    user, device, DeviceSecurityEvent.DeviceSecurityType.UNCONFIRMED_DEVICE));
        }
    }


    // =========================================================================
    // Private — Device management
    // =========================================================================

    private void handleDeviceState(Device device) {
        if (device.isLoggedOut()) {
            device.setLoggedOut(false);
            deviceService.saveDevice(device);
            log.debug("Device {} reactivated after logout", device.getId());
        }
    }


    // =========================================================================
    // Private — Token generation
    // =========================================================================

    private LoginTokens generateTokens(User user, Device device) {
        String accessToken = authJwtService.generateAccessToken(user, device);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, device);
        return new LoginTokens(accessToken, refreshToken.getToken(), user.getId(), device.getId());
    }

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
                            log.debug("Refresh token {} revoked during logout", tokenId);
                        });
            }
        } catch (Exception e) {
            log.warn("Error revoking refresh token during logout: {}", e.getMessage());
        }
    }

    private void revokeDeviceConfirmationTokens(User user) {
        try {
            deviceConfirmationTokenService.revokeAllUserTokens(user);
        } catch (Exception e) {
            log.warn("Error revoking device confirmation tokens for user {}: {}",
                    user.getUsername(), e.getMessage());
        }
    }


    // =========================================================================
    // Private — 2FA helpers
    // =========================================================================

    private void sendVerificationCodeSync(User user, TwoFactorType type, String code,
                                          HttpServletRequest httpRequest)
            throws MailDeliveryException, SmsSendingException {
        switch (type) {
            case EMAIL -> authMailerService.sendTwoFactorCodeSync(user, code, httpRequest);
            case SMS   -> authSmsService.sendTwoFactorCodeSync(user, code);
            default    -> throw new IllegalArgumentException(
                    "Unsupported 2FA type for code sending: " + type);
        }
    }

    private List<TwoFactorType> getAvailableAlternatives(User user, TwoFactorType failedMethod) {
        return twoFactorFactory.getEnabledTwoFactorMethods(user).stream()
                .map(TwoFactorAuth::getType)
                .filter(type -> type != failedMethod)
                .toList();
    }

    private TwoFactorTokenClaims extractTwoFactorClaims(Claims claims) {
        return new TwoFactorTokenClaims(
                claims.get("publicId", String.class),
                claims.get("username", String.class),
                claims.get("email", String.class),
                TwoFactorType.valueOf(claims.get("twoFactorType", String.class)),
                claims.get("verificationCodeHash", String.class),
                claims.getIssuedAt().getTime(),
                claims.getExpiration().getTime()
        );
    }

    private String getMaskedTarget(User user, TwoFactorType type) {
        return switch (type) {
            case EMAIL        -> maskEmail(user.getEmail());
            case SMS          -> maskPhoneNumber(user.getPhoneNumber());
            case TOTP         -> "Authenticator App";
            case BACKUP_CODES -> "Backup Codes";
            case WEBAUTHN     -> "Security Key";
        };
    }


    // =========================================================================
    // Private — User loading
    // =========================================================================

    private User loadUser(String username) {
        return (User) loadUserByUsername(username);
    }


    // =========================================================================
    // Private — Utility
    // =========================================================================

    private String buildLockoutMessage(Instant unlockTime) {
        if (unlockTime == null) {
            return "Account temporarily locked due to too many failed login attempts. " +
                    "Please try again later.";
        }
        Instant now = Instant.now();
        if (unlockTime.isBefore(now)) {
            return "Account temporarily locked due to too many failed login attempts. " +
                    "Please try again.";
        }
        long minutes = ChronoUnit.MINUTES.between(now, unlockTime);
        if (minutes < 1) {
            long seconds = ChronoUnit.SECONDS.between(now, unlockTime);
            return String.format(
                    "Account temporarily locked. Try again in %d seconds.", seconds);
        }
        return String.format(
                "Account temporarily locked. Try again in %d minutes.", minutes);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***.***";
        String[] parts = email.split("@");
        String local = parts[0];
        String maskedLocal = local.length() > 2
                ? local.charAt(0) + "***" + local.charAt(local.length() - 1)
                : "***";
        return maskedLocal + "@" + parts[1];
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }

    /**
     * FIX (bonus) — original used split(".") which matches ANY character.
     * Must be split("\\.") to split on a literal dot.
     */
    private String extractOauthProvider(String email) {
        try {
            return email.split("@")[1].split("\\.")[0];
        } catch (Exception e) {
            return "your OAuth provider";
        }
    }
}