package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.events.*;
import be.steby.CoreProject.bll.domains.password.events.email.PasswordResetCodeEmailRequestedEvent;
import be.steby.CoreProject.bll.domains.password.events.sms.PasswordResetSmsRequestedEvent;
import be.steby.CoreProject.bll.domains.password.exceptions.*;
import be.steby.CoreProject.bll.domains.password.models.*;
import be.steby.CoreProject.bll.domains.password.services.jwt.PasswordResetJwtService;
import be.steby.CoreProject.bll.domains.password.services.tokens.verification_code.VerificationCodeTokenService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.password.services.tokens.email.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.bll.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.entities.tokens.VerificationCodeToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.dl.enums.PasswordResetType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Implementation of password management operations.
 *
 * <p>Handles all password-related business logic including:
 * <ul>
 *   <li>Password reset requests via EMAIL_LINK, EMAIL_CODE, or SMS_CODE</li>
 *   <li>Verification code validation for code-based flows</li>
 *   <li>Password reset completion with tokens or permission cookies</li>
 *   <li>Password changes for authenticated users</li>
 * </ul>
 *
 * @see PasswordResetType
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final DeviceService deviceService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetJwtService passwordResetJwtService;
    private final VerificationCodeTokenService verificationCodeTokenService;

    @Value("${url.front_server}")
    private String FRONT_URL;

    // =========================================================================
    // FORGOT PASSWORD - INITIATE RESET
    // =========================================================================

    @Override
    public CodePasswordResetResult requestPasswordReset(ForgotPasswordBLLRequest request) {
        log.debug("Processing password reset request for email: {} via {}",
                request.email(), request.resetType());

        // Business validation
        validatePasswordResetRequest(request);

        // Security check: must be anonymous
        checkIsAnonymous();

        try {
            // Find user by email (may throw exception if not found)
            User user = userService.getUserByEmail(request.email());

            // Route to appropriate handler based on reset type
            return switch (request.resetType()) {
                case EMAIL_LINK -> {
                    handleEmailLinkPasswordReset(user);
                    yield CodePasswordResetResult.linkBased();
                }
                case EMAIL_CODE -> handleEmailCodePasswordReset(user);
                case SMS_CODE -> handleSmsCodePasswordReset(user);
            };

        } catch (Exception e) {
            // For security: silently handle all exceptions
            log.debug("Password reset request silently handled for email: {} - reason: {}",
                    request.email(), e.getMessage());
            return CodePasswordResetResult.failure();
        }
    }

    // =========================================================================
    // CODE VERIFICATION
    // =========================================================================

    @Override
    public CodeVerificationResult verifyPasswordResetCode(VerifyCodeBLLRequest request) {
        log.debug("Processing password reset code verification");

        // Business validation
        validateCodeVerificationRequest(request);

        // Security check: must be anonymous
        checkIsAnonymous();

        try {
            // Validate JWT reference token and extract claims
            Claims claims = passwordResetJwtService.validateCodeReferenceToken(request.jwtToken());

            // Extract token reference from JWT
            String tokenReference = claims.get("tokenRef", String.class);

            log.debug("Code verification request with token ref: {}", tokenReference);

            // Find token by reference and get associated user
            Optional<VerificationCodeToken> tokenOpt = verificationCodeTokenService.findValidTokenByReference(tokenReference);
            if (tokenOpt.isEmpty()) {
                log.warn("No valid token found for reference: {}", tokenReference);
                return CodeVerificationResult.failure();
            }

            VerificationCodeToken verificationCodeToken = tokenOpt.get();
            User user = verificationCodeToken.getUser();

            // Validate the provided code against stored hash
            boolean isCodeValid = verificationCodeTokenService.validateVerificationCode(
                    user,
                    request.verificationCode()
            );

            if (!isCodeValid) {
                log.warn("Invalid verification code provided for user: {}", user.getUsername());
                return CodeVerificationResult.failure();
            }

            // Generate permission token for password reset access
            String permissionToken = passwordResetJwtService.generatePermissionToken(user.getEmail());

            log.info("Password reset code verified successfully for user: {}", user.getUsername());
            return CodeVerificationResult.success(permissionToken);

        } catch (InvalidPasswordResetTokenException e) {
            log.warn("Code verification failed - invalid token: {}", e.getMessage());
            return CodeVerificationResult.failure();
        } catch (Exception e) {
            log.error("Unexpected error during code verification: {}", e.getMessage(), e);
            return CodeVerificationResult.failure();
        }
    }

    // =========================================================================
    // RESET PASSWORD - COMPLETE RESET
    // =========================================================================


    @Transactional
    @Override
    public void resetPassword(PasswordResetRequest request, String token) {
        checkIsAnonymous();

        PasswordResetToken passwordResetToken;
        User user;

        try {
            passwordResetToken = passwordResetTokenService.getSecureValidToken(token, TokenType.PASSWORD_RESET);
            passwordResetTokenService.verifyTokenValidity(passwordResetToken);
            user = passwordResetToken.getUser();
        } catch (Exception e) {
            // ← AJOUTER: publier PasswordResetFailedEvent (user/device inconnus)
            eventPublisher.publishEvent(new PasswordResetFailedEvent(
                    null,
                    null,
                    e.getMessage()
            ));
            throw e;
        }

        Device device = deviceService.detectAndRegisterDevice(user);

        PasswordValidationResult result = passwordPolicyService.validatePassword(request.password());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }

        // Save password directly (don't use savePassword() to avoid PasswordChangedEvent)
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPasswordChangedAt(Instant.now());
        if (user.isMustChangePassword()) {
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);

        // ← MODIFIER: publier PasswordResetCompletedEvent (pas PasswordChangedEvent)
        eventPublisher.publishEvent(new PasswordResetCompletedEvent(user, device));

        // SECURITY: Logout from ALL devices
        log.info("Password reset for user {} - logging out ALL devices", user.getUsername());
        passwordResetTokenService.revokeToken(passwordResetToken);

        refreshTokenService.revokeAllUserTokens(user);
        deviceService.disconnectAllDevicesForUser(user);
    }

    @Override
    public void resetPasswordWithPermission(ResetPasswordWithPermissionBLLRequest request) {
        log.debug("Processing password reset with permission token");

        // Business validation
        validateResetPasswordWithPermissionRequest(request);

        // Security check: must be anonymous
        checkIsAnonymous();

        // Validate permission token and extract claims
        Claims claims = passwordResetJwtService.validatePermissionToken(request.permissionToken());
        String email = claims.get("email", String.class);

        log.debug("Password reset with permission for email: {}", email);

        // Find user by email
        User user = userService.getUserByEmail(email);

        // Validate password policy
        PasswordValidationResult result = passwordPolicyService.validatePassword(request.newPassword());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }

        // Save the new password
        Device device = deviceService.detectAndRegisterDevice(user);
        savePassword(request.newPassword(), user, device);

        // Security: Logout from ALL devices
        log.info("Password reset with permission for user {} - logging out ALL devices", user.getUsername());
        refreshTokenService.revokeAllUserTokens(user);
        deviceService.disconnectAllDevicesForUser(user);

        log.info("Password reset completed successfully for user: {}", user.getUsername());
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    @Override
    public void requestPasswordToken(String token) {
        checkIsAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getSecureToken(token);
        if (passwordResetToken.isValid()) {
            String url = FRONT_URL + "/api/password/reset-password?token=" + token;
            throw new TokenValidityException("This token is still valid. Please follow this link: " + url);
        }

        User user = passwordResetToken.getUser();
        PasswordResetToken newToken = passwordResetTokenService.createPasswordResetToken(user);

        // ← AJOUTER: détecter le device
        Device device = deviceService.detectAndRegisterDevice(user);

        eventPublisher.publishEvent(
                new RequestPasswordTokenEvent(user, newToken.getPublicId(), device)
        );

        passwordResetTokenService.revokeToken(passwordResetToken);
    }

    // =========================================================================
    // CHANGE PASSWORD (AUTHENTICATED)
    // =========================================================================

    @Override
    public void changePassword(PasswordChangeRequest request) {
        User authenticatedUser = userService.getAuthenticatedUser();
        Device currentDevice = deviceService.detectCurrentDevice();

        if (!authenticatedUser.hasPassword()) {
            throw new NoPasswordDefinedException(
                    "You don't have a password yet. Use the 'Define password' feature instead."
            );
        }

        if (!passwordEncoder.matches(request.currentPassword(), authenticatedUser.getPassword())) {
            eventPublisher.publishEvent(new PasswordChangeFailedEvent(
                    authenticatedUser,
                    currentDevice,
                    "Incorrect current password"
            ));
            throw new InvalidPasswordException("The current password is not correct", 400);
        }

        PasswordValidationResult result = passwordPolicyService.validatePassword(request.newPassword());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }

        savePassword(request.newPassword(), authenticatedUser, currentDevice); // ← passer device

        Long currentDeviceId = currentDevice != null ? currentDevice.getId() : null;

        if (currentDeviceId != null) {
            int revokedTokens = refreshTokenService.revokeAllUserTokensExceptDevice(authenticatedUser, currentDeviceId);
            int disconnectedDevices = deviceService.disconnectAllOtherDevices();

            log.info("Password changed for user {} - revoked {} tokens and disconnected {} devices (kept device {})",
                    authenticatedUser.getUsername(), revokedTokens, disconnectedDevices, currentDeviceId);
        } else {
            log.warn("Could not identify current device for user {} - logging out ALL devices",
                    authenticatedUser.getUsername());

            refreshTokenService.revokeAllUserTokens(authenticatedUser);
            deviceService.disconnectAllDevicesForUser(authenticatedUser);
        }
    }


    // =========================================================================
    // DEFINE PASSWORD (OAUTH USERS)
    // =========================================================================


    @Override
    @Transactional
    public void definePassword(String newPassword) {
        User authenticatedUser = userService.getAuthenticatedUser();
        Device currentDevice = deviceService.detectCurrentDevice(); // ← AJOUTER

        if (authenticatedUser.hasPassword()) {
            throw new PasswordAlreadyDefinedException(
                    "You already have a password defined. Use the change password feature instead."
            );
        }

        PasswordValidationResult result = passwordPolicyService.validatePassword(newPassword);
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }

        authenticatedUser.setPassword(passwordEncoder.encode(newPassword));
        authenticatedUser.setPasswordChangedAt(Instant.now());
        userService.saveUser(authenticatedUser);

        eventPublisher.publishEvent(new PasswordChangedEvent(authenticatedUser, currentDevice)); // ← ajouter device

        log.info("Password defined for OAuth user: {}", authenticatedUser.getUsername());
    }

    // =========================================================================
    // PRIVATE HANDLERS - RESET TYPE SPECIFIC
    // =========================================================================

    /**
     * Handles password reset via email link (EMAIL_LINK).
     *
     * <p>Creates a long-lived token and sends an email with a clickable reset link.
     *
     * @param user the user requesting password reset
     */
    private void handleEmailLinkPasswordReset(User user) {
        log.debug("Handling EMAIL_LINK password reset for user: {}", user.getUsername());

        PasswordResetToken passwordResetToken = passwordResetTokenService.createPasswordResetToken(user);

        // ← AJOUTER: détecter le device
        Device device = deviceService.detectAndRegisterDevice(user);

        eventPublisher.publishEvent(
                new RequestPasswordResetEvent(user, passwordResetToken.getPublicId(), device)
        );

        log.debug("Email link password reset initiated for user: {}", user.getUsername());
    }

    /**
     * Handles password reset via email code (EMAIL_CODE).
     *
     * <p>Creates a short verification code and sends it via email.
     * Returns JWT token for cookie storage.
     *
     * @param user the user requesting password reset
     * @return result containing JWT token for verification flow
     */
    private CodePasswordResetResult handleEmailCodePasswordReset(User user) {
        log.debug("Handling EMAIL_CODE password reset for user: {}", user.getUsername());

        try {
            // Create DB token with verification code
            VerificationCodeTokenService.CodeGenerationResult result =
                    verificationCodeTokenService.createSmsPasswordResetToken(user);

            // Create JWT reference token for cookie
            String tokenReference = result.token().getToken();
            String jwtReferenceToken = passwordResetJwtService.generateCodeReferenceToken(tokenReference);

            // Publish email event with the generated code
            eventPublisher.publishEvent(
                    new PasswordResetCodeEmailRequestedEvent(user, result.plainVerificationCode())
            );

            log.info("EMAIL_CODE password reset initiated successfully for user: {}", user.getUsername());
            return CodePasswordResetResult.success(jwtReferenceToken);

        } catch (MaxAttemptsReachedException e) {
            log.warn("EMAIL_CODE password reset failed for user {} - rate limit exceeded: {}",
                    user.getUsername(), e.getMessage());
            return CodePasswordResetResult.failure();
        } catch (Exception e) {
            log.error("Failed to initiate EMAIL_CODE password reset for user {} - error: {}",
                    user.getUsername(), e.getMessage(), e);
            return CodePasswordResetResult.failure();
        }
    }

    /**
     * Handles password reset via SMS code (SMS_CODE).
     *
     * <p>Creates a short verification code and sends it via SMS.
     * Requires the user to have a verified phone number.
     *
     * @param user the user requesting password reset
     * @return result containing JWT token for verification flow
     */
    private CodePasswordResetResult handleSmsCodePasswordReset(User user) {
        log.debug("Handling SMS_CODE password reset for user: {}", user.getUsername());

        try {
            // Validate SMS requirements first
            if (!canReceiveSms(user)) {
                log.debug("User {} cannot receive SMS - requirements not met", user.getUsername());
                return CodePasswordResetResult.failure();
            }

            // Create DB token with verification code
            VerificationCodeTokenService.CodeGenerationResult result =
                    verificationCodeTokenService.createSmsPasswordResetToken(user);

            // Create JWT reference token for cookie
            String tokenReference = result.token().getToken();
            String jwtReferenceToken = passwordResetJwtService.generateCodeReferenceToken(tokenReference);

            // Publish SMS event with the generated code
            eventPublisher.publishEvent(
                    new PasswordResetSmsRequestedEvent(user, result.plainVerificationCode())
            );

            log.info("SMS_CODE password reset initiated successfully for user: {}", user.getUsername());
            return CodePasswordResetResult.success(jwtReferenceToken);

        } catch (MaxAttemptsReachedException e) {
            log.warn("SMS_CODE password reset failed for user {} - rate limit exceeded: {}",
                    user.getUsername(), e.getMessage());
            return CodePasswordResetResult.failure();
        } catch (Exception e) {
            log.error("Failed to initiate SMS_CODE password reset for user {} - error: {}",
                    user.getUsername(), e.getMessage(), e);
            return CodePasswordResetResult.failure();
        }
    }

    // =========================================================================
    // PRIVATE VALIDATION METHODS
    // =========================================================================

    /**
     * Validates the password reset request data.
     */
    private void validatePasswordResetRequest(ForgotPasswordBLLRequest request) {
        if (request == null) {
            throw new PasswordRequestValidationException("Password reset request cannot be null");
        }

        if (request.email() == null || request.email().isBlank()) {
            throw new PasswordRequestValidationException("Email address cannot be null or blank");
        }

        if (request.resetType() == null) {
            throw new PasswordRequestValidationException("Reset type cannot be null");
        }

        // Validate email normalization (defensive programming)
        String normalizedEmail = request.email().toLowerCase().trim();
        if (!request.email().equals(normalizedEmail)) {
            throw new PasswordRequestValidationException("Email must be normalized (lowercase and trimmed)");
        }
    }

    /**
     * Validates the code verification request data.
     */
    private void validateCodeVerificationRequest(VerifyCodeBLLRequest request) {
        if (request == null) {
            throw new PasswordRequestValidationException("Code verification request cannot be null");
        }

        if (request.verificationCode() == null || request.verificationCode().isBlank()) {
            throw new PasswordRequestValidationException("Verification code cannot be null or blank");
        }

        if (request.jwtToken() == null || request.jwtToken().isBlank()) {
            throw new PasswordRequestValidationException("JWT token cannot be null or blank");
        }

        // Ensure code is 6 digits
        if (!request.verificationCode().matches("^[0-9]{6}$")) {
            throw new PasswordRequestValidationException("Verification code must be exactly 6 digits");
        }
    }

    /**
     * Validates the password reset with permission request data.
     */
    private void validateResetPasswordWithPermissionRequest(ResetPasswordWithPermissionBLLRequest request) {
        if (request == null) {
            throw new PasswordRequestValidationException("Password reset request cannot be null");
        }

        if (request.permissionToken() == null || request.permissionToken().isBlank()) {
            throw new PasswordRequestValidationException("Permission token cannot be null or blank");
        }

        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new PasswordRequestValidationException("New password cannot be null or blank");
        }
    }

    // =========================================================================
    // PRIVATE UTILITY METHODS
    // =========================================================================

    /**
     * Saves the new password for a user.
     */
    private void savePassword(String password, User user, Device device) {
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChangedAt(Instant.now());
        if (user.isMustChangePassword()) {
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);

        eventPublisher.publishEvent(new PasswordChangedEvent(user, device));
    }

    /**
     * Checks if the current request is from an anonymous user.
     */
    private void checkIsAnonymous() {
        if (!userService.isAnonymous()) {
            String url = FRONT_URL + "/password/change";
            String message = "You are logged in. Please use the change password feature instead: ";
            throw new UserAuthenticationStateException(message + url, 403);
        }
    }

    /**
     * Checks if a user can receive SMS messages.
     */
    private boolean canReceiveSms(User user) {
        return user.getPhoneNumber() != null
                && !user.getPhoneNumber().isBlank()
                && user.isPhoneNumberVerified();
    }

    /**
     * Masks phone number for secure logging.
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "***";
        }
        return phoneNumber.substring(0, 3) + "*****" + phoneNumber.substring(phoneNumber.length() - 3);
    }
}