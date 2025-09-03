package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordTokenEvent;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.domains.password.models.PasswordChangeRequest;
import be.steby.CoreProject.bll.domains.password.models.PasswordResetRequest;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password service implementation with KISS helpers for event publishing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final RequestContextService requestContextService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordActivityLogService passwordActivityLogService;

    @Value("${url.front_server}")
    private String FRONT_URL;

    // ================== PASSWORD RESET METHODS ==================

    @Transactional
    @Override
    public void resetPassword(PasswordResetRequest request, String token, HttpServletRequest httpRequest) {
        checkIsAnonymous();

        User user = null;
        Device device = null;
        RequestContext requestContext = null;

        try {
            PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(token);
            passwordResetTokenService.verifyTokenValidity(passwordResetToken);
            user = passwordResetToken.getUser();

            requestContext = requestContextService.captureRequestContext(httpRequest);
            device = deviceService.detectAndRegisterDevice(httpRequest, user);

            // Validate password policy
            PasswordValidationResult result = passwordPolicyService.validatePassword(request.password());
            if (!result.isValid()) {
                passwordActivityLogService.logPasswordPolicyViolation(user, device, result.errors(), requestContext);
                throw new InvalidPasswordException("Password doesn't meet security requirements: "
                        + String.join(", ", result.errors()));
            }

            // Save password
            savePassword(request.password(), user);

            // ✅ DUAL APPROACH with HELPERS:
            publishPasswordChangedEvent(user, requestContext);                    // → Email
            passwordActivityLogService.logPasswordReset(user, device, requestContext); // → Database

            passwordResetTokenService.revokeToken(passwordResetToken);

            log.info("Password reset successfully for user: {}", user.getUsername());

        } catch (InvalidPasswordException e) {
            throw e;
        } catch (Exception e) {
            if (user != null && requestContext != null) {
                if (device == null) device = deviceService.detectCurrentDevice(httpRequest);
                passwordActivityLogService.logPasswordResetFailed(user, device, e.getMessage(), requestContext);
            }
            throw e;
        }
    }

    // ================== PASSWORD CHANGE METHODS ==================

    @Override
    @Transactional
    public void changePassword(PasswordChangeRequest request, HttpServletRequest httpRequest) {
        User authenticatedUser = userService.getAuthenticatedUser();
        Device device = null;
        RequestContext requestContext = null;

        try {
            device = deviceService.detectCurrentDevice(httpRequest);
            requestContext = requestContextService.captureRequestContext(httpRequest);

            // Verify current password
            if (!passwordEncoder.matches(request.currentPassword(), authenticatedUser.getPassword())) {
                passwordActivityLogService.logCurrentPasswordIncorrect(authenticatedUser, device, requestContext);
                throw new InvalidPasswordException("The current password is not correct", 400);
            }

            // Validate new password policy
            PasswordValidationResult result = passwordPolicyService.validatePassword(request.newPassword());
            if (!result.isValid()) {
                passwordActivityLogService.logPasswordPolicyViolation(authenticatedUser, device, result.errors(), requestContext);
                throw new InvalidPasswordException("Password doesn't meet security requirements: "
                        + String.join(", ", result.errors()));
            }

            // Save password
            savePassword(request.newPassword(), authenticatedUser);

            // ✅ DUAL APPROACH with HELPERS:
            publishPasswordChangedEvent(authenticatedUser, requestContext);                    // → Email
            passwordActivityLogService.logPasswordChanged(authenticatedUser, device, requestContext); // → Database

            log.info("Password changed successfully for user: {}", authenticatedUser.getUsername());

        } catch (InvalidPasswordException e) {
            throw e;
        } catch (Exception e) {
            if (requestContext == null) requestContext = requestContextService.captureRequestContext(httpRequest);
            if (device == null) device = deviceService.detectCurrentDevice(httpRequest);
            passwordActivityLogService.logPasswordChangeFailed(authenticatedUser, device, e.getMessage(), requestContext);
            throw e;
        }
    }

    // ================== PASSWORD RESET REQUEST METHODS ==================

    @Override
    public void requestPasswordReset(String email, HttpServletRequest request) {
//        checkIsAnonymous();

        try {
            User user = userService.getUserByEmail(email);
            log.info("User = " + user.getUsername());
            PasswordResetToken passwordResetToken = passwordResetTokenService.createPasswordResetToken(user);
            RequestContext requestContext = requestContextService.captureRequestContext(request);
            log.info("context = " + requestContext.getRequestId());

            Device device = deviceService.detectAndRegisterDevice(request, user);
            log.info("device name = " + device.getName());


            // ✅ DUAL APPROACH with HELPERS:
            publishPasswordResetRequestEvent(user, passwordResetToken.getToken(), requestContext); // → Email
            passwordActivityLogService.logPasswordResetRequested(user, device, email, requestContext); // → Database

            log.info("Password reset requested for user: {}", user.getUsername());

        } catch (Exception e) {
            RequestContext requestContext = requestContextService.captureRequestContext(request);
            Device device = deviceService.detectCurrentDevice(request);
            passwordActivityLogService.logPasswordResetRequestedByEmail(email, device, requestContext);
            log.warn("Password reset requested for email: {} - {}", email, e.getMessage());
        }
    }

    @Override
    public void requestPasswordToken(String token, HttpServletRequest httpRequest) {
        checkIsAnonymous();

        User user = null;
        Device device = null;
        RequestContext requestContext = null;

        try {
            PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(token);
            user = passwordResetToken.getUser();
            device = deviceService.detectAndRegisterDevice(httpRequest, user);
            requestContext = requestContextService.captureRequestContext(httpRequest);

            if (passwordResetToken.isValid()) {
                String url = FRONT_URL + "/api/password/reset-password?token=" + token;
                throw new TokenValidityException("This token is still valid. Please follow this link: " + url);
            }

            PasswordResetToken newToken = passwordResetTokenService.createPasswordResetToken(user);

            // ✅ DUAL APPROACH with HELPERS:
            publishPasswordTokenRequestEvent(user, newToken.getToken(), requestContext); // → Email
            passwordActivityLogService.logPasswordResetTokenRequested(user, device, requestContext); // → Database

            passwordResetTokenService.revokeToken(passwordResetToken);

            log.info("New password reset token requested for user: {}", user.getUsername());

        } catch (Exception e) {
            if (user != null && requestContext != null) {
                passwordActivityLogService.logPasswordResetFailed(user, device, e.getMessage(), requestContext);
            }
            throw e;
        }
    }

    @Override
    public void forcePasswordChange(User targetUser, String adminUsername, HttpServletRequest httpRequest) {

    }

    // ================== KISS EVENT PUBLISHING HELPERS ==================

    /**
     * Helper to publish password changed event for email notification
     */
    private void publishPasswordChangedEvent(User user, RequestContext requestContext) {
        eventPublisher.publishEvent(new PasswordChangedEvent(user, requestContext));
    }

    /**
     * Helper to publish password reset request event for email notification
     */
    private void publishPasswordResetRequestEvent(User user, String token, RequestContext requestContext) {
        eventPublisher.publishEvent(new RequestPasswordResetEvent(user, token, requestContext));
    }

    /**
     * Helper to publish password token request event for email notification
     */
    private void publishPasswordTokenRequestEvent(User user, String token, RequestContext requestContext) {
        eventPublisher.publishEvent(new RequestPasswordTokenEvent(user, token, requestContext));
    }

    // ================== PRIVATE HELPER METHODS ==================

    private void savePassword(String password, User user) {
        user.setPassword(passwordEncoder.encode(password));
        if (user.isMustChangePassword()) {
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);
    }

    private void checkIsAnonymous() {
        if (!userService.isAnonymous()) {
            String url = FRONT_URL + "/password/change-password";
            String message = "You are logged in. Please use the change password feature instead: ";
            throw new UserAuthenticationStateException(message + url, 403);
        }
    }

    // ================== ANALYSIS METHODS ==================

    @Transactional(readOnly = true)
    public PasswordActivityLogService.PasswordStats getPasswordStats(User user) {
        return passwordActivityLogService.getPasswordStats(user);
    }

    public boolean hasSuspiciousPasswordActivity(User user, int hoursWindow) {
        return !passwordActivityLogService.findSuspiciousPasswordActivities(user, hoursWindow).isEmpty();
    }
}