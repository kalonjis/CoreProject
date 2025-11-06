package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordTokenEvent;
import be.steby.CoreProject.bll.domains.password.events.sms.PasswordResetSmsRequestedEvent;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import be.steby.CoreProject.bll.domains.password.exceptions.PasswordRequestValidationException;
import be.steby.CoreProject.bll.domains.password.models.*;
import be.steby.CoreProject.bll.domains.password.services.tokens.SmsTokenServiceImpl;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.bll.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.entities.tokens.SmsToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    private final JwtUtil jwtUtil;
    private final SmsTokenServiceImpl smsTokenService;

    @Value("${url.front_server}")
    private String FRONT_URL;



    @Transactional
    @Override
    public void resetPassword(PasswordResetRequest request, String token, HttpServletRequest httpRequest) {
        checkIsAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getSecureValidToken(token, TokenType.PASSWORD_RESET);
        passwordResetTokenService.verifyTokenValidity(passwordResetToken);

        User user = passwordResetToken.getUser();

        PasswordValidationResult result = passwordPolicyService.validatePassword(request.password());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }


        savePassword(request.password(), user);

        // ✅ SECURITY: Logout from ALL devices (not authenticated)
        log.info("Password reset for user {} - logging out ALL devices", user.getUsername());
        passwordResetTokenService.revokeToken(passwordResetToken);

        refreshTokenService.revokeAllUserTokens(user);
        deviceService.disconnectAllDevicesForUser(user);
    }


    @Override
    public void changePassword(PasswordChangeRequest request, HttpServletRequest httpRequest) {
        User authenticatedUser = userService.getAuthenticatedUser();

        if(!passwordEncoder.matches(request.currentPassword(), authenticatedUser.getPassword())){
            throw new InvalidPasswordException("The current password is not correct", 400);
        }
        PasswordValidationResult result = passwordPolicyService.validatePassword(request.newPassword());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }


        savePassword(request.newPassword(), authenticatedUser);

        Long currentDeviceId = deviceService.detectCurrentDevice(httpRequest).getId();

        if(currentDeviceId != null){
            int revokedTokens = refreshTokenService.revokeAllUserTokensExceptDevice(authenticatedUser, currentDeviceId);

            int disconnectedDevices = deviceService.disconnectAllDevicesExceptCurrent(authenticatedUser, currentDeviceId);

            log.info("Password changed for user {} - revoked {} tokens and disconnected {} devices (kept device {})",
                    authenticatedUser.getUsername(), revokedTokens, disconnectedDevices, currentDeviceId);

        } else {
            // Fallback : if can't identify current device, logout everywhere
            log.warn("Could not identify current device for user {} - logging out ALL devices",
                    authenticatedUser.getUsername());

            refreshTokenService.revokeAllUserTokens(authenticatedUser);
            deviceService.disconnectAllDevicesForUser(authenticatedUser);
        }

    }



    @Override
    public SmsPasswordResetResult requestPasswordReset(ForgotPasswordBLLRequest request, HttpServletRequest httpRequest) {
        log.debug("Processing password reset request for email: {} via {}",
                request.email(), request.notificationType());

        // Business validation
        validatePasswordResetRequest(request);

        // Security check: must be anonymous
        checkIsAnonymous();

        try {
            // Find user by email (may throw exception if not found)
            User user = userService.getUserByEmail(request.email());

            // Route to appropriate handler based on notification type
            return switch (request.notificationType()) {
                case EMAIL -> {
                    handleEmailPasswordReset(user);
                    yield SmsPasswordResetResult.failure(); // No JWT token for email
                }
                case SMS -> handleSmsPasswordReset(user); // Returns SmsPasswordResetResult
                default -> {
                    log.warn("Unsupported notification type: {}", request.notificationType());
                    yield SmsPasswordResetResult.failure(); // Silently fail for security
                }
            };

        } catch (Exception e) {
            // For security: silently handle all exceptions
            log.debug("Password reset request silently handled for email: {} - reason: {}",
                    request.email(), e.getMessage());
            return SmsPasswordResetResult.failure(); // Always return failure for security
        }

        // Note: Success logging moved to individual handlers
    }


    @Override
    public SmsVerificationResult verifySmsPasswordResetCode(VerifySmsPasswordResetBLLRequest request) {
        log.debug("Processing SMS password reset code verification");

        // Business validation
        validateSmsVerificationRequest(request);

        // Security check: must be anonymous (not authenticated)
        checkIsAnonymous();

        try {
            // ✨ NOUVEAU: Validate JWT reference token and extract claims
            Claims claims = jwtUtil.validatePasswordResetSmsReferenceToken(request.jwtToken());

            // Extract token reference from JWT
            String tokenReference = claims.get("tokenRef", String.class);

            log.debug("SMS verification request with token ref: {}", tokenReference);

            // ✨ NOUVEAU: Find SMS token by reference and get associated user
            Optional<SmsToken> tokenOpt = smsTokenService.findValidTokenByReference(tokenReference);
            if (tokenOpt.isEmpty()) {
                log.warn("No valid SMS token found for reference: {}", tokenReference);
                return SmsVerificationResult.failure();
            }

            SmsToken smsToken = tokenOpt.get();
            User user = smsToken.getUser();

            // ✨ NOUVEAU: Validate the provided code using DB token approach
            boolean isCodeValid = smsTokenService.validateVerificationCode(
                    user,
                    request.verificationCode()
            );

            if (!isCodeValid) {
                log.warn("Invalid SMS verification code provided for user: {}", user.getUsername());
                return SmsVerificationResult.failure();
            }

            // Generate permission token for password reset access (keep JWT for this)
            String permissionToken = jwtUtil.generatePasswordResetPermissionToken(user.getEmail());

            log.info("SMS password reset code verified successfully for user: {}", user.getUsername());
            return SmsVerificationResult.success(permissionToken);

        } catch (InvalidPasswordResetTokenException e) {
            log.warn("SMS verification failed - invalid token: {}", e.getMessage());
            return SmsVerificationResult.failure();
        } catch (Exception e) {
            log.error("Unexpected error during SMS code verification: {}", e.getMessage(), e);
            return SmsVerificationResult.failure();
        }
    }

    /**
     * Validates the SMS verification request data.
     *
     * @param request the SMS verification request
     * @throws PasswordRequestValidationException if validation fails
     */
    private void validateSmsVerificationRequest(VerifySmsPasswordResetBLLRequest request) {
        if (request == null) {
            throw new PasswordRequestValidationException("SMS verification request cannot be null");
        }

        if (request.verificationCode() == null || request.verificationCode().isBlank()) {
            throw new PasswordRequestValidationException("Verification code cannot be null or blank");
        }

        if (request.jwtToken() == null || request.jwtToken().isBlank()) {
            throw new PasswordRequestValidationException("JWT token cannot be null or blank");
        }

        // Additional validation: ensure code is 6 digits (defensive programming)
        if (!request.verificationCode().matches("^[0-9]{6}$")) {
            throw new PasswordRequestValidationException("Verification code must be exactly 6 digits");
        }
    }

    /**
     * Validates the business rules for password reset request.
     *
     * @param request the password reset request
     * @throws PasswordRequestValidationException if validation fails
     */
    private void validatePasswordResetRequest(ForgotPasswordBLLRequest request) {
        if (request == null) {
            throw new PasswordRequestValidationException("Password reset request cannot be null");
        }

        if (request.email() == null || request.email().isBlank()) {
            throw new PasswordRequestValidationException("Email address cannot be null or blank");
        }

        if (request.notificationType() == null) {
            throw new PasswordRequestValidationException("Notification type cannot be null");
        }

        // Validate email normalization (defensive programming)
        String normalizedEmail = request.email().toLowerCase().trim();
        if (!request.email().equals(normalizedEmail)) {
            throw new PasswordRequestValidationException("Email must be normalized (lowercase and trimmed)");
        }
    }

    /**
     * Handles password reset via email (traditional method).
     *
     * @param user the user requesting password reset
     */
    private void handleEmailPasswordReset(User user) {
        log.debug("Handling EMAIL password reset for user: {}", user.getUsername());

        // Create password reset token
        PasswordResetToken passwordResetToken = passwordResetTokenService.createPasswordResetToken(user);

        // Publish event to send email
        eventPublisher.publishEvent(
                new RequestPasswordResetEvent(user, passwordResetToken.getPublicId())
        );

        log.debug("Email password reset initiated for user: {}", user.getUsername());
    }

    /**
     * Handles password reset via SMS (requires verified phone number).
     *
     * <p>This method generates a short 6-digit verification code and sends it
     * via SMS to the user's verified phone number. The code has a shorter
     * lifespan than email tokens and is intended for immediate use.
     *
     * @param user the user requesting password reset
     */
    private SmsPasswordResetResult handleSmsPasswordReset(User user) {
        log.debug("Handling SMS password reset for user: {}", user.getUsername());

        try {
            // Validate SMS requirements first
            if (!canReceiveSms(user)) {
                log.debug("User {} cannot receive SMS - requirements not met", user.getUsername());
                return SmsPasswordResetResult.failure(); // Return failure for security
            }

            // ✨ NOUVEAU: Create DB token with verification code
            SmsTokenServiceImpl.SmsCodeGenerationResult result =
                    smsTokenService.createSmsPasswordResetToken(user);

            // ✨ NOUVEAU: Create JWT reference token for cookie (lightweight session)
            String tokenReference = result.token().getToken();
            String jwtReferenceToken = jwtUtil.generatePasswordResetSmsReferenceToken(tokenReference);

            // Publish SMS event with the generated code
            eventPublisher.publishEvent(
                    new PasswordResetSmsRequestedEvent(user, result.plainVerificationCode())
            );

            log.info("SMS password reset initiated successfully for user: {}", user.getUsername());
            return SmsPasswordResetResult.success(jwtReferenceToken);

        } catch (MaxAttemptsReachedException e) {
            log.warn("SMS password reset failed for user {} - rate limit exceeded: {}",
                    user.getUsername(), e.getMessage());
            return SmsPasswordResetResult.failure();
        } catch (Exception e) {
            log.error("Failed to initiate SMS password reset for user {} - error: {}",
                    user.getUsername(), e.getMessage(), e);
            return SmsPasswordResetResult.failure(); // Return failure instead of throwing
        }
    }


    @Override
    public void resetPasswordWithPermission(ResetPasswordWithPermissionBLLRequest request) {
        log.debug("Processing password reset with permission token");

        // Business validation
        validateResetPasswordWithPermissionRequest(request);

        // Security check: must be anonymous (not authenticated)
        checkIsAnonymous();

        // Validate permission token and extract claims
        Claims claims = jwtUtil.validatePasswordResetPermissionToken(request.permissionToken());
        String email = claims.get("email", String.class);

        log.debug("Password reset with permission for email: {}", email);

        // Find user by email to ensure they exist
        User user = userService.getUserByEmail(email);

        // Validate password policy
        PasswordValidationResult result = passwordPolicyService.validatePassword(request.newPassword());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }

        // Save the new password
        savePassword(request.newPassword(), user);

        // Security: Logout from ALL devices for security (password was reset)
        log.info("Password reset with permission for user {} - logging out ALL devices", user.getUsername());
        refreshTokenService.revokeAllUserTokens(user);
        deviceService.disconnectAllDevicesForUser(user);

        log.info("Password reset completed successfully for user: {}", user.getUsername());
    }

    /**
     * Validates the password reset with permission request data.
     *
     * @param request the password reset request
     * @throws PasswordRequestValidationException if validation fails
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


//    @Override
//    public void resetPasswordWithPermission(ResetPasswordBLLRequest request, String permissionToken, HttpServletRequest httpRequest) {
//        log.debug("Processing password reset with permission token");
//
//        // Business validation
//        validateResetPasswordRequest(request);
//
//        // Security check: must be anonymous (not authenticated)
//        checkIsAnonymous();
//
//        try {
//            // Validate permission token and extract claims (logique métier ici)
//            Claims claims = jwtUtil.validatePasswordResetPermissionToken(permissionToken);
//            String email = claims.get("email", String.class);
//
//            log.debug("Password reset with permission for email: {}", email);
//
//            // Find user by email
//            User user = userService.getUserByEmail(email);
//
//            // Apply password policies and update password
//            passwordPolicyService.validatePassword(request.newPassword(), user);
//
//            // Update password
//            user.setPassword(passwordEncoder.encode(request.newPassword()));
//            userRepository.save(user);
//
//            // Audit logging
//           // auditService.logPasswordReset(user, httpRequest, "SMS_PERMISSION");
//
//            log.info("Password reset completed successfully for user: {} using permission token", user.getUsername());
//
//        } catch (InvalidPasswordResetTokenException e) {
//            log.warn("Invalid permission token provided: {}", e.getMessage());
//            throw PasswordDomainException("Invalid or expired permission token");
//        } catch (Exception e) {
//            log.error("Unexpected error during permission-based password reset: {}", e.getMessage(), e);
//            throw PasswordDomainException("Password reset failed");
//        }
//    }

    /**
     * Masks phone number for display in email notifications.
     * Shows format like "+32*****890" for security.
     *
     * @param phoneNumber the phone number to mask
     * @return masked phone number for display
     */
    private String maskPhoneNumberForDisplay(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "****";
        }

        // Show country code + first few digits, mask middle, show last 3
        if (phoneNumber.startsWith("+")) {
            int length = phoneNumber.length();
            if (length >= 8) {
                String prefix = phoneNumber.substring(0, 4); // "+32X"
                String suffix = phoneNumber.substring(length - 3); // "890"
                int maskedLength = length - 7; // total - prefix - suffix
                return prefix + "*".repeat(maskedLength) + suffix;
            }
        }

        // Fallback masking
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }

    /**
     * Checks if user can receive SMS for password reset.
     *
     * @param user the user to check
     * @return true if user has verified phone number and SMS is enabled
     */
    private boolean canReceiveSms(User user) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            log.debug("User {} has no phone number configured", user.getUsername());
            return false;
        }

        if (!user.isPhoneNumberVerified()) {
            log.debug("User {} has unverified phone number", user.getUsername());
            return false;
        }

        // TODO: Add SMS service availability check
        // if (!smsService.isAvailable()) return false;

        return true;
    }


    @Override
    public void requestPasswordToken(String token, HttpServletRequest httpRequest){
        checkIsAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getSecureToken(token);
        if(passwordResetToken.isValid()) {
            String url = FRONT_URL + "/api/password/reset-password?token=" + token ;
            throw new TokenValidityException("This token, is still valid. Please follow this link: " + url);
        }
        User user = passwordResetToken.getUser();
        PasswordResetToken newToken = passwordResetTokenService.createPasswordResetToken(user);

        eventPublisher.publishEvent(
                new RequestPasswordTokenEvent(
                    user,
                    newToken.getPublicId()
                )
        );

        passwordResetTokenService.revokeToken(passwordResetToken);
    }


    private void savePassword(String password, User user){
        user.setPassword( passwordEncoder.encode(password) );
        if(user.isMustChangePassword()){
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);

        eventPublisher.publishEvent(new PasswordChangedEvent(user));
    }

    private void checkIsAnonymous(){
        if(!userService.isAnonymous()) {
            String url = FRONT_URL + "/password/change-password";
            String message = "You are logged in. Please use the change password feature instead: ";
            throw new UserAuthenticationStateException(message + url, 403);
        }
    }

}
