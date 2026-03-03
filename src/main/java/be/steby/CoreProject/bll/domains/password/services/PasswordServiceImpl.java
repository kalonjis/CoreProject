package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.events.*;
import be.steby.CoreProject.bll.domains.password.exceptions.*;
import be.steby.CoreProject.bll.domains.password.models.*;
import be.steby.CoreProject.bll.domains.password.services.jwt.PasswordResetJwtService;
import be.steby.CoreProject.bll.domains.password.services.tokens.email.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.bll.domains.password.services.tokens.verification_code.VerificationCodeTokenService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.common.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.common.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.entities.tokens.VerificationCodeToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
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
 * Core password operations: verification, reset completion, change, define.
 *
 * <p>Reset initiation is handled by dedicated services:
 * {@link EmailLinkPasswordResetService}, {@link EmailCodePasswordResetService},
 * {@link SmsPasswordResetService}.
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
    private final PasswordHistoryService passwordHistoryService;

    @Value("${url.front_server}")
    private String frontUrl;

    // =========================================================================
    // CODE VERIFICATION (EMAIL_CODE + SMS_CODE)
    // =========================================================================

    @Override
    public CodeVerificationResult verifyPasswordResetCode(VerifyCodeBLLRequest request) {
        log.debug("Processing password reset code verification");

        validateCodeVerificationRequest(request);
        checkIsAnonymous();

        try {
            Claims claims = passwordResetJwtService.validateCodeReferenceToken(request.jwtToken());
            String tokenReference = claims.get("tokenRef", String.class);

            Optional<VerificationCodeToken> tokenOpt =
                    verificationCodeTokenService.findValidTokenByReference(tokenReference);

            if (tokenOpt.isEmpty()) {
                log.warn("No valid token found for reference: {}", tokenReference);
                return CodeVerificationResult.failure();
            }

            VerificationCodeToken verificationCodeToken = tokenOpt.get();
            User user = verificationCodeToken.getUser();

            boolean isCodeValid = verificationCodeTokenService.validateVerificationCode(
                    user, request.verificationCode()
            );

            if (!isCodeValid) {
                log.warn("Invalid verification code for user: {}", user.getUsername());
                return CodeVerificationResult.failure();
            }

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
            eventPublisher.publishEvent(new PasswordResetFailedEvent(null, null, e.getMessage()));
            throw e;
        }

        Device device = deviceService.detectAndRegisterDevice(user);

        validatePassword(request.password(), user);

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPasswordChangedAt(Instant.now());
        if (user.isMustChangePassword()) {
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);

        eventPublisher.publishEvent(new PasswordResetCompletedEvent(user, device));

        passwordResetTokenService.revokeToken(passwordResetToken);
        refreshTokenService.revokeAllUserTokens(user);
        deviceService.disconnectAllDevicesForUser(user);

        log.info("Password reset completed for user: {} - all devices logged out", user.getUsername());
    }

    @Override
    public void resetPasswordWithPermission(ResetPasswordWithPermissionBLLRequest request) {
        log.debug("Processing password reset with permission token");

        validateResetPasswordWithPermissionRequest(request);
        checkIsAnonymous();

        Claims claims = passwordResetJwtService.validatePermissionToken(request.permissionToken());
        String email = claims.get("email", String.class);

        User user = userService.getUserByEmail(email);

        validatePassword(request.newPassword(), user);

        Device device = deviceService.detectAndRegisterDevice(user);
        savePassword(request.newPassword(), user, device);

        refreshTokenService.revokeAllUserTokens(user);
        deviceService.disconnectAllDevicesForUser(user);

        log.info("Password reset with permission completed for user: {} - all devices logged out",
                user.getUsername());
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    @Override
    public void requestPasswordToken(String token) {
        checkIsAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getSecureToken(token);
        if (passwordResetToken.isValid()) {
            String url = frontUrl + "/api/password/reset-password?token=" + token;
            throw new TokenValidityException("This token is still valid. Please follow this link: " + url);
        }

        User user = passwordResetToken.getUser();
        PasswordResetToken newToken = passwordResetTokenService.createPasswordResetToken(user);
        Device device = deviceService.detectAndRegisterDevice(user);

        eventPublisher.publishEvent(new RequestPasswordTokenEvent(user, newToken.getPublicId(), device));

        passwordResetTokenService.revokeToken(passwordResetToken);
    }

    // =========================================================================
    // CHANGE PASSWORD (AUTHENTICATED)
    // =========================================================================

    @Override
    public void changePassword(PasswordChangeRequest request) {
        User user = userService.getAuthenticatedUser();
        Device currentDevice = deviceService.detectCurrentDevice();

        if (!user.hasPassword()) {
            throw new NoPasswordDefinedException(
                    "You don't have a password yet. Use the 'Define password' feature instead."
            );
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            eventPublisher.publishEvent(new PasswordChangeFailedEvent(
                    user, currentDevice, "Incorrect current password"
            ));
            throw new InvalidPasswordException("The current password is not correct", 400);
        }

        validatePassword(request.newPassword(), user);

        savePassword(request.newPassword(), user, currentDevice);

        Long currentDeviceId = currentDevice != null ? currentDevice.getId() : null;

        if (currentDeviceId != null) {
            int revokedTokens = refreshTokenService.revokeAllUserTokensExceptDevice(user, currentDeviceId);
            int disconnectedDevices = deviceService.disconnectAllOtherDevices();
            log.info("Password changed for user {} - revoked {} tokens, disconnected {} devices (kept device {})",
                    user.getUsername(), revokedTokens, disconnectedDevices, currentDeviceId);
        } else {
            log.warn("Could not identify current device for user {} - logging out ALL devices",
                    user.getUsername());
            refreshTokenService.revokeAllUserTokens(user);
            deviceService.disconnectAllDevicesForUser(user);
        }
    }

    // =========================================================================
    // DEFINE PASSWORD (OAUTH USERS)
    // =========================================================================

    @Transactional
    @Override
    public void definePassword(String newPassword) {
        User user = userService.getAuthenticatedUser();
        Device currentDevice = deviceService.detectCurrentDevice();

        if (user.hasPassword()) {
            throw new PasswordAlreadyDefinedException(
                    "You already have a password defined. Use the change password feature instead."
            );
        }

        validatePassword(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userService.saveUser(user);

        eventPublisher.publishEvent(new PasswordChangedEvent(user, currentDevice));

        log.info("Password defined for OAuth user: {}", user.getUsername());
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private void savePassword(String password, User user, Device device) {
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChangedAt(Instant.now());
        if (user.isMustChangePassword()) {
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);
        passwordHistoryService.record(user, user.getPassword());
        eventPublisher.publishEvent(new PasswordChangedEvent(user, device));
    }

    private void validatePassword(String password) {
        PasswordValidationResult result = passwordPolicyService.validatePassword(password);
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }
    }

    private void validatePassword(String password, User user) {
        validatePassword(password);
        passwordHistoryService.checkNotRecentlyUsed(user, password);
    }

    private void checkIsAnonymous() {
        if (!userService.isAnonymous()) {
            throw new UserAuthenticationStateException(
                    "You are logged in. Please use the change password feature instead: "
                            + frontUrl + "/password/change", 403
            );
        }
    }

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
        if (!request.verificationCode().matches("^[0-9]{6}$")) {
            throw new PasswordRequestValidationException("Verification code must be exactly 6 digits");
        }
    }

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
}