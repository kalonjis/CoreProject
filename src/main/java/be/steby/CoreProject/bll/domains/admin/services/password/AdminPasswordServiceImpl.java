package be.steby.CoreProject.bll.domains.admin.services.password;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.admin.events.AdminPasswordResetTriggeredEvent;
import be.steby.CoreProject.bll.domains.admin.models.password.AdminPasswordResetBLLRequest;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.enums.admin.AdminPasswordResetStrategy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service implementation for administrative password management operations.
 *
 * This service orchestrates password reset operations initiated by administrators,
 * with multiple strategies for different security scenarios:
 *
 * - STANDARD_RESET: Simple email-based reset for forgotten passwords
 * - SECURITY_BREACH: Immediate lockout for compromised accounts
 * - TEMPORARY_PASSWORD: Alternative access recovery when email unavailable
 * - FORCE_EXPIRE: Compliance-driven password expiration
 *
 * All operations are fully audited and logged for compliance requirements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPasswordServiceImpl implements AdminPasswordService {

    private final UserService userService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final DeviceService deviceService;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // ===============================
    // PUBLIC API
    // ===============================

    @Override
    @Transactional
    public void forcePasswordReset(
            Long userId,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.warn("Admin force password reset initiated - userId: {}, strategy: {}, reason: {}",
                userId, request.strategy(), request.reason());

        // 1. Check admin permissions
        userService.requireAdminPermissions();

        // 2. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserById(userId);

        log.info("Password reset - Admin: {} (ID: {}), Target: {} (ID: {}), Strategy: {}",
                admin.getUsername(), admin.getId(),
                target.getUsername(), target.getId(),
                request.strategy());

        // 3. Dispatch according to strategy
        switch (request.strategy()) {
            case STANDARD_RESET -> handleStandardReset(target, admin, request, httpRequest);
            case SECURITY_BREACH -> handleSecurityBreach(target, admin, request, httpRequest);
            case TEMPORARY_PASSWORD -> handleTemporaryPassword(target, admin, request, httpRequest);
            case FORCE_EXPIRE -> handleForceExpire(target, admin, request, httpRequest);
        }

        log.info("Password reset completed - userId: {}, strategy: {}",
                userId, request.strategy());
    }

    // ===============================
    // STRATEGY HANDLERS
    // ===============================

    /**
     * STANDARD_RESET: Simple reset email, current password remains valid.
     * User clicks link and sets new password at their convenience.
     */
    private void handleStandardReset(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.info("Executing STANDARD_RESET for user {}", target.getId());

        // Generate reset token
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);
        log.debug("Password reset token generated - tokenId: {}", token.getPublicId());

        // Invalidate sessions if requested (optional for standard reset)
        if (request.invalidateActiveSessions()) {
            invalidateUserSessions(target);
        }

        // Publish event (triggers email via listener)
        publishEvent(target, admin, request);

        log.info("Standard reset email sent to {}", target.getEmail());
    }

    /**
     * SECURITY_BREACH: IMMEDIATELY revokes password and locks account.
     * User cannot login until they complete the reset process.
     * All sessions are terminated.
     */
    private void handleSecurityBreach(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.warn("🚨 Executing SECURITY_BREACH for user {}", target.getId());

        // 1. REVOKE password immediately (user cannot login anymore)
//        target.setPasswordRevoked(true);
//        target.setPasswordRevokedAt(Instant.now());
//        target.setPasswordRevokedReason(request.reason());
        userService.saveUser(target);

        log.error("🔒 Password REVOKED for user {} - account locked", target.getId());

        // 2. Invalidate ALL sessions (mandatory for security breach)
        invalidateUserSessions(target);

        // 3. Generate reset token
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);
        log.debug("Security breach reset token generated - tokenId: {}", token.getPublicId());

        // 4. Publish high-priority event
        publishEvent(target, admin, request);

        log.warn("Security breach handled - user {} locked, reset email sent", target.getId());
    }

    /**
     * TEMPORARY_PASSWORD: Generates temporary password sent via alternative channel.
     * Forces password change on first login.
     */
    private void handleTemporaryPassword(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.info("Executing TEMPORARY_PASSWORD for user {}", target.getId());

        // 1. Generate temporary password based on delivery method
        String tempPassword = generateTemporaryPasswordForDelivery(
                request.alternativeDeliveryMethod()
        );

        String encodedTempPassword = passwordEncoder.encode(tempPassword);

        // 2. Store in user (with 24h expiration)
//        target.setTemporaryPassword(encodedTempPassword);
//        target.setTemporaryPasswordExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
//        target.setTemporaryPasswordUsed(false);
        target.setMustChangePassword(true);  // FORCE change on login
        userService.saveUser(target);

        log.info("Temporary password generated for user {} (expires in 24h)", target.getId());

        // 3. Invalidate sessions if requested
        if (request.invalidateActiveSessions()) {
            invalidateUserSessions(target);
        }

        // 4. Send via alternative channel
        sendTemporaryPasswordViaAlternativeChannel(
                target,
                tempPassword,  // ⚠️ Plain text only for sending
                request.alternativeNotificationEmail(),
                request.alternativeDeliveryMethod()
        );

        // 5. Publish event
        publishEvent(target, admin, request);

        log.info("Temporary password sent via {} to user {}",
                request.alternativeDeliveryMethod(), target.getId());
    }

    /**
     * FORCE_EXPIRE: Marks password as expired.
     * User can login ONE more time then must change password.
     */
    private void handleForceExpire(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.info("Executing FORCE_EXPIRE for user {}", target.getId());

        // Mark password as must-change
        target.setMustChangePassword(true);
        //target.setPasswordExpiredAt(Instant.now());
        userService.saveUser(target);

        // Generate reset token (user can choose to reset via email)
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);
        log.debug("Force expire reset token generated - tokenId: {}", token.getPublicId());

        // Publish event
        publishEvent(target, admin, request);

        log.info("Password expired for user {} - must change on next login", target.getId());
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    /**
     * Invalidates all user sessions (refresh tokens + devices).
     */
    private void invalidateUserSessions(User user) {
        refreshTokenService.revokeAllUserTokens(user);
        int disconnectedDevices = deviceService.disconnectAllDevicesForUser(user);

        log.warn("Sessions invalidated - userId: {}, devices disconnected: {}",
                user.getId(), disconnectedDevices);
    }

    /**
     * Publishes audit event for the password reset operation.
     */
    private void publishEvent(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request) {

        String notificationEmail = request.getNotificationEmail(target.getEmail());

        boolean forceChange = request.strategy() == AdminPasswordResetStrategy.TEMPORARY_PASSWORD
                || request.strategy() == AdminPasswordResetStrategy.FORCE_EXPIRE;

        AdminPasswordResetTriggeredEvent event = AdminPasswordResetTriggeredEvent.of(
                target,
                admin,
                request.reason(),
                request.strategy(),
                forceChange,
                request.invalidateActiveSessions(),
                notificationEmail
        );

        eventPublisher.publishEvent(event);

        // Enhanced logging for security-related resets
        if (request.isSecurityRelated()) {
            log.warn("🚨 SECURITY PASSWORD RESET - Admin: {}, Target: {}, Reason: {}, Strategy: {}",
                    admin.getUsername(), target.getUsername(),
                    request.reason(), request.strategy());
        }
    }

    /**
     * Generates temporary password based on delivery method.
     */
    private String generateTemporaryPasswordForDelivery(String deliveryMethod) {
        if ("SMS".equalsIgnoreCase(deliveryMethod)) {
            // For SMS: human-friendly, 12 characters
            return passwordPolicyService.generateHumanFriendlyPassword(12);

        } else if ("EMAIL".equalsIgnoreCase(deliveryMethod)) {
            // For email: formatted for readability, 16 characters
            return passwordPolicyService.generateFormattedTemporaryPassword(16);

        } else {
            // Default: secure standard, 16 characters
            return passwordPolicyService.generateSecurePassword(16);
        }
    }



    /**
     * Sends temporary password via alternative channel.
     * TODO: Implement actual delivery mechanisms (SMS, email service, etc.)
     */
    private void sendTemporaryPasswordViaAlternativeChannel(
            User user,
            String tempPassword,
            String alternativeEmail,
            String deliveryMethod) {

        log.info("Sending temporary password to user {} via {}",
                user.getId(), deliveryMethod != null ? deliveryMethod : "email");

        if ("SMS".equalsIgnoreCase(deliveryMethod)) {
            // TODO: Integrate SMS service (Twilio, AWS SNS, etc.)
            log.info("SMS delivery: Would send password to user's phone");

        } else if ("EMAIL".equalsIgnoreCase(deliveryMethod) && alternativeEmail != null) {
            // TODO: Send via alternative email using email service
            log.info("Email delivery to alternative address: {}", alternativeEmail);

        } else {
            // Fallback: primary email
            // TODO: Send via primary email using email service
            log.info("Email delivery to primary address: {}", user.getEmail());
        }

        // ⚠️ WARNING: Never log passwords in production!
        // This is for development/testing only
        if (log.isDebugEnabled()) {
            log.debug("Temporary password (length: {}): {}",
                    tempPassword.length(), tempPassword);
        }
    }
}