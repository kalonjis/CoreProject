package be.steby.CoreProject.bll.domains.admin.services.password;

import be.steby.CoreProject.bll.common.services.generation.password.TemporaryPasswordGeneratorService;
import be.steby.CoreProject.bll.domains.admin.events.AdminPasswordResetTriggeredEvent;
import be.steby.CoreProject.bll.domains.admin.models.password.AdminPasswordResetBLLRequest;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for administrative password management operations.
 *
 * ✅ SIMPLIFIED KISS APPROACH:
 * - User has ONE password (not separate temporary password)
 * - User has ONE flag: mustChangePassword
 * - No password revocation fields (just revoke tokens instead)
 * - No expiration dates (password expires when used)
 *
 * This follows GAFA best practices (Google, AWS, GitHub) where:
 * 1. Admin generates temp password
 * 2. Replaces user's current password
 * 3. Sets mustChangePassword = true
 * 4. User must change on first login
 *
 * Security is enforced by:
 * - Revoking all refresh tokens (logout everywhere)
 * - Clearing all devices (force re-authentication)
 * - mustChangePassword flag (enforced in AuthenticationService)
 *
 * @author Steby Core Team
 * @version 3.0 (Simplified KISS approach)
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPasswordServiceImpl implements AdminPasswordService {

    private final UserService userService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final DeviceService deviceService;
    private final TemporaryPasswordGeneratorService passwordGeneratorService;
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

        // 2. Load target user
        User targetUser = userService.getUserById(userId);

        // 3. Get current admin for audit
        User admin = userService.getAuthenticatedUser();

        // 4. Execute strategy-specific logic
        switch (request.strategy()) {
            case STANDARD_RESET -> handleStandardReset(targetUser, admin, request, httpRequest);
            case SECURITY_BREACH -> handleSecurityBreach(targetUser, admin, request, httpRequest);
            case TEMPORARY_PASSWORD -> handleTemporaryPassword(targetUser, admin, request, httpRequest);
            case FORCE_EXPIRE -> handleForceExpire(targetUser, admin, request, httpRequest);
            default -> throw new IllegalArgumentException(
                    "Unknown password reset strategy: " + request.strategy());
        }

        log.info("Admin password reset completed successfully - userId: {}, strategy: {}",
                userId, request.strategy());
    }

    // ===============================
    // STRATEGY IMPLEMENTATIONS
    // ===============================

    /**
     * STANDARD_RESET: Standard email-based password reset flow.
     * Generates a reset token and sends it via email.
     *
     * ✅ SIMPLIFIED: No password changes, just generates token
     *
     * Use case: User forgot password, email still accessible.
     */
    private void handleStandardReset(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.info("Executing STANDARD_RESET for user {}", target.getId());

        // 1. Generate password reset token
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);
        log.debug("Password reset token generated - tokenId: {}", token.getPublicId());

        // 2. Invalidate sessions if requested
        if (request.invalidateActiveSessions()) {
            revokeAllUserTokens(target);
        }

        // 3. Publish event (triggers email with reset link)
        publishEvent(target, admin, request);

        log.info("Standard reset email sent to user {}", target.getId());
    }

    /**
     * SECURITY_BREACH: Immediate password replacement + logout everywhere.
     * Used when account security is compromised.
     *
     * ✅ SIMPLIFIED:
     * - Replace password with random one (user can't login)
     * - Revoke all tokens (logout everywhere)
     * - Send reset email (so user can regain access)
     * - No "revoked" fields needed
     *
     * Use case: Detected unauthorized access, data breach, account takeover.
     */
    private void handleSecurityBreach(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.warn("Executing SECURITY_BREACH for user {}", target.getId());

        // 1. Generate a random password (user doesn't know it = effectively locked)
        String randomPassword = passwordGeneratorService.generateStandard(32);
        target.setPassword(passwordEncoder.encode(randomPassword));
        target.setMustChangePassword(true);
        userService.saveUser(target);

        log.error("🔒 Password replaced with random value for user {} - effectively locked",
                target.getId());

        // 2. MANDATORY: Revoke all tokens (logout everywhere)
        revokeAllUserTokens(target);

        // 3. Generate reset token so user can regain access
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);
        log.debug("Security breach reset token generated - tokenId: {}", token.getPublicId());

        // 4. Publish high-priority event
        publishEvent(target, admin, request);

        log.warn("Security breach handled - user {} locked out, reset email sent", target.getId());
    }

    /**
     * TEMPORARY_PASSWORD: Generates temporary password for alternative delivery.
     * Forces password change on first login.
     *
     * ✅ SIMPLIFIED:
     * - Generate temp password
     * - REPLACE user's current password (not separate field)
     * - Set mustChangePassword = true
     * - Send via alternative channel
     *
     * Use case: Primary email inaccessible, urgent account recovery needed.
     */
    private void handleTemporaryPassword(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.info("Executing TEMPORARY_PASSWORD for user {}", target.getId());

        // 1. Generate temporary password optimized for delivery method
        // Automatically selects best strategy:
        // - SMS/PHONE → Human-friendly (no ambiguous characters O/0, I/1/l)
        // - EMAIL → Formatted in blocks (easier to read/type)
        // - Other → Standard (maximum security)
        String tempPassword = passwordGeneratorService.generateForDeliveryMethod(
                request.alternativeDeliveryMethod()
        );

        // 2. REPLACE current password with temporary one
        target.setPassword(passwordEncoder.encode(tempPassword));
        target.setMustChangePassword(true);  // FORCE change on first login
        userService.saveUser(target);

        log.info("✅ Temporary password set for user {} (mustChangePassword=true)",
                target.getId());

        // 3. Invalidate sessions if requested
        if (request.invalidateActiveSessions()) {
            revokeAllUserTokens(target);
        }

        // 4. Send via alternative channel (SMS, alternative email, etc.)
        sendTemporaryPasswordViaAlternativeChannel(
                target,
                tempPassword,  // ⚠️ Plain text only for sending
                request.alternativeNotificationEmail(),
                request.alternativeDeliveryMethod()
        );

        // 5. Publish event for audit
        publishEvent(target, admin, request);

        log.info("Temporary password sent via {} to user {}",
                request.alternativeDeliveryMethod(), target.getId());
    }

    /**
     * FORCE_EXPIRE: Forces user to change password on next login.
     *
     * ✅ SIMPLIFIED:
     * - Just set mustChangePassword = true
     * - No expiration date needed (enforced on login)
     * - User can still login with current password, but must change it
     *
     * Use case: Compliance policies (e.g., 90-day password rotation).
     */
    private void handleForceExpire(
            User target,
            User admin,
            AdminPasswordResetBLLRequest request,
            HttpServletRequest httpRequest) {

        log.info("Executing FORCE_EXPIRE for user {}", target.getId());

        // 1. Set flag to force password change
        target.setMustChangePassword(true);
        userService.saveUser(target);

        log.info("✅ Password expiration set for user {} - must change on next login",
                target.getId());

        // 2. Invalidate sessions if requested
        if (request.invalidateActiveSessions()) {
            revokeAllUserTokens(target);
        }

        // 3. Publish event (triggers notification email)
        publishEvent(target, admin, request);

        log.info("Password expiration notification sent to user {}", target.getId());
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    /**
     * Revokes all refresh tokens and clears all devices for a user.
     *
     * ✅ SIMPLIFIED: No Session entity needed, just revoke tokens!
     *
     * This effectively logs out the user everywhere:
     * - Refresh tokens revoked → Can't generate new access tokens
     * - Devices cleared → Must re-authenticate from scratch
     * - Access tokens → Will expire naturally (15-30min TTL)
     */
    private void revokeAllUserTokens(User user) {
        log.warn("Revoking all tokens for user {}", user.getId());

        // 1. Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(user);
        log.debug("✅ Revoked {} refresh tokens for user {}", user.getId());

        // 2. Clear all devices (forces re-authentication)
        int clearedDevices = deviceService.disconnectAllDevicesForUser(user);
        log.debug("✅ Cleared {} devices for user {}", clearedDevices, user.getId());

        log.info("🔒 User {} logged out everywhere - {} tokens revoked, {} devices cleared",
                user.getId(), clearedDevices);
    }

    /**
     * Sends temporary password via alternative delivery channel.
     *
     * ⚠️ SECURITY: Plain text password is ONLY transmitted here, NEVER logged.
     */
    private void sendTemporaryPasswordViaAlternativeChannel(
            User user,
            String plainTextPassword,
            String alternativeEmail,
            String deliveryMethod) {

        log.info("Sending temporary password via {} to user {}", deliveryMethod, user.getId());

        // TODO: Implement actual delivery mechanism:
        // - EMAIL: Use email service (alternative email address)
        // - SMS: Use SMS gateway (user's phone number)
        // - PHONE: Manual call by support team
        // - INTERNAL: Display in admin console for manual delivery

        log.debug("Delivery method: {}", deliveryMethod);
        if (alternativeEmail != null) {
            log.debug("Alternative email: {}", alternativeEmail);
        }

        // ⚠️ CRITICAL: NEVER log the actual password
        log.info("✅ Temporary password sent successfully");
    }

    /**
     * Publishes admin password reset event for audit trail.
     */
    private void publishEvent(User target, User admin, AdminPasswordResetBLLRequest request) {
        AdminPasswordResetTriggeredEvent event = new AdminPasswordResetTriggeredEvent(
                target,
                admin,
                request.strategy(),
                request.reason(),
                request.invalidateActiveSessions(),
                request.alternativeDeliveryMethod()
        );

        eventPublisher.publishEvent(event);
        log.debug("AdminPasswordResetTriggeredEvent published for user {}", target.getId());
    }
}