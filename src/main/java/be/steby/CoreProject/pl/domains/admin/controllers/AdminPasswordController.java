package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.password.AdminPasswordService;
import be.steby.CoreProject.pl.domains.admin.models.requests.AdminAlternativeChannelPasswordRequest;
import be.steby.CoreProject.pl.domains.admin.models.requests.password.AdminPasswordResetLinkRequest;
import be.steby.CoreProject.pl.domains.admin.models.requests.password.AdminPasswordResetRequest;
import be.steby.CoreProject.pl.domains.admin.models.requests.password.AdminTemporaryPasswordRequest;
import be.steby.CoreProject.pl.domains.admin.models.responses.AdminPasswordOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for admin password management operations.
 * Handles administrative password operations with multiple strategies
 * for different security scenarios.
 *
 * All endpoints require ADMIN privileges.
 * Business logic is delegated to AdminPasswordService.
 *
 * This controller is separate from AdminUserAccountController to maintain
 * single responsibility principle and align with the user-facing
 * PasswordController pattern.
 *
 * Endpoint pattern: /api/admin/users/{userId}/password/*
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/password-reset")
@Slf4j
public class AdminPasswordController {

    private final AdminPasswordService adminPasswordService;

// ===============================
// PASSWORD RESET OPERATIONS
// ===============================

/**
 * Forces a password reset for a user as an administrator.
 *
 *
 * This endpoint provides admin-controlled password reset with multiple strategies:
 *
 * 1. STANDARD_RESET:
 *    - Sends password reset email to user
 *    - Current password remains valid until user resets
 *    - Use for: User forgot password, needs admin help
 *
 * 2. SECURITY_BREACH:
 *    - IMMEDIATELY revokes current password (user locked out)
 *    - Invalidates ALL active sessions
 *    - Sends reset email with high-priority notification
 *    - Use for: Account compromised, security incident
 *
 * 3. TEMPORARY_PASSWORD:
 *    - Generates secure temporary password
 *    - Sends via alternative channel (SMS, alternative email)
 *    - Forces password change on first login
 *    - Use for: Primary email inaccessible, urgent recovery
 *
 * 4. FORCE_EXPIRE:
 *    - Marks password as expired
 *    - Current password works for ONE more login
 *    - User must change password after login
 *    - Use for: Compliance policies (90-day expiration)
 *
 * All operations are fully audited with reason tracking.
 *
 * @param userId User ID requiring password reset
 * @param request Reset request with strategy, reason, and options
 * @param httpRequest HTTP request for context capture (IP, user agent)
 * @return 200 OK with operation details
 */
@PatchMapping("/{userId}")
public ResponseEntity<Map<String, String>> forcePasswordReset(
        @PathVariable Long userId,
        @Valid @RequestBody AdminPasswordResetRequest request,
        HttpServletRequest httpRequest) {

    log.warn("Admin force password reset request - userId: {}, strategy: {}, reason: {}",
            userId, request.strategy(), request.reason());

    // Convert PL DTO to BLL DTO and delegate
    adminPasswordService.forcePasswordReset(
            userId,
            request.toBLL(),
            httpRequest
    );

    // Build response with operation details
    Map<String, String> response = new HashMap<>();
    response.put("message", "Password reset operation completed successfully");
    response.put("userId", userId.toString());
    response.put("strategy", request.strategy().name());
    response.put("strategyDescription", request.strategy().getDescription());
    response.put("reason", request.reason());
    response.put("sessionsInvalidated", String.valueOf(request.invalidateActiveSessions()));
    response.put("securityLevel", request.strategy().getSecurityLevel());

    // Add strategy-specific info
    if (request.strategy().generatesTemporaryPassword()) {
        response.put("temporaryPasswordGenerated", "true");
        response.put("deliveryMethod",
                request.alternativeDeliveryMethod() != null
                        ? request.alternativeDeliveryMethod()
                        : "EMAIL");
    }

    if (request.strategy().requiresImmediateRevocation()) {
        response.put("passwordRevoked", "true");
        response.put("accountLocked", "true");
    }

    log.info("Password reset completed successfully - userId: {}, strategy: {}",
            userId, request.strategy());

    return ResponseEntity.ok(response);
}


@PostMapping("/send-reset-link/{userPublicId}")
public ResponseEntity<AdminPasswordOperationResponse> sendPasswordResetLink(
        @PathVariable String userPublicId,
        @Valid @RequestBody AdminPasswordResetLinkRequest request) {

    adminPasswordService.sendPasswordResetLink(userPublicId, request.toBLL());

    return ResponseEntity.ok(
            AdminPasswordOperationResponse.resetLinkSent()
    );
}


    /**
     * Sends a temporary password to a user via email.
     *
     * This endpoint:
     * - Generates a secure temporary password
     * - Replaces the user's current password with the temporary one
     * - Sets mustChangePassword flag to true
     * - Revokes all active sessions (user must re-authenticate)
     * - Sends the temporary password via email
     *
     * Use cases:
     * - User locked out of account
     * - User needs immediate access
     * - Admin-assisted password recovery
     *
     * Security notes:
     * - User will be forced to change password on next login
     * - All active sessions are terminated
     * - Operation is fully audited
     *
     * @param userPublicId User's public ID requiring temporary password
     * @param request Request containing reason for audit trail
     * @return 200 OK with operation confirmation
     */
    @PostMapping("/send-temporary-password/{userPublicId}")
    public ResponseEntity<AdminPasswordOperationResponse> sendTemporaryPassword(
            @PathVariable String userPublicId,
            @Valid @RequestBody AdminTemporaryPasswordRequest request) {

        log.warn("Admin temporary password request - userPublicId: {}, reason: {}",
                userPublicId, request.reason());

        adminPasswordService.sendTemporaryPassword(userPublicId, request.toBLL());

        return ResponseEntity.ok(
                AdminPasswordOperationResponse.temporaryPasswordSent()
        );
    }


    // Dans AdminPasswordController.java

    /**
     * Sends temporary password via ALTERNATIVE CHANNEL (email or SMS).
     *
     * ⚠️ USE CASE: Primary email/phone is compromised or inaccessible.
     *
     * IMPORTANT SECURITY NOTES:
     * - This is a GENERIC implementation for flexibility
     * - In production, alternative channels should be PRE-VERIFIED from user profile
     * - Never accept arbitrary email/phone without verification process
     * - Consider implementing:
     *   - 30-day grace period after channel changes
     *   - Multi-factor verification using immutable data
     *   - Physical verification for high-security accounts
     *
     * This endpoint:
     * - Generates secure temporary password
     * - Revokes ALL active sessions
     * - Sets mustChangePassword flag
     * - Sends password via alternative channel(s)
     * - Fully audited with reason
     *
     * @param userPublicId User's public ID
     * @param request Request with reason and alternative channel(s)
     * @return 200 OK with operation confirmation
     */
    @PostMapping("/send-via-alternative-channel/{userPublicId}")
    public ResponseEntity<AdminPasswordOperationResponse> sendViaAlternativeChannel(
            @PathVariable String userPublicId,
            @Valid @RequestBody AdminAlternativeChannelPasswordRequest request) {

        log.warn("⚠️ Admin alternative channel password request - userPublicId: {}, reason: {}",
                userPublicId, request.reason());

        adminPasswordService.sendTemporaryPasswordViaAlternativeChannel(userPublicId, request.toBLL());

        return ResponseEntity.ok(
                AdminPasswordOperationResponse.alternativeChannelPasswordSent(
                        request.useAlternativeEmail(),
                        request.useAlternativePhone()
                )
        );
    }

    // ===============================
    // FUTURE: PASSWORD POLICY OPERATIONS
    // ===============================

    /**
     * TODO: Gets password history for a user (compliance tracking).
     * GET /api/admin/users/{userId}/password/history
     *
     * Returns list of password change events with timestamps.
     * Useful for compliance audits (e.g., ensuring no password reuse).
     *
     * @GetMapping("/history")
     * public ResponseEntity<List<PasswordHistoryDTO>> getPasswordHistory(
     *     @PathVariable Long userId,
     *     @RequestParam(defaultValue = "10") int limit)
     */

    /**
     * TODO: Checks if user's password meets current security policy.
     * GET /api/admin/users/{userId}/password/policy-compliance
     *
     * Returns compliance status with details:
     * - Password age
     * - Last changed date
     * - Meets current policy (length, complexity)
     * - Recommended action
     *
     * @GetMapping("/policy-compliance")
     * public ResponseEntity<PasswordPolicyStatusDTO> checkPolicyCompliance(
     *     @PathVariable Long userId)
     */

    /**
     * TODO: Forces immediate password expiration for a user.
     * PATCH /api/admin/users/{userId}/password/expire
     *
     * Similar to FORCE_EXPIRE strategy but without notification.
     * Used for bulk operations or automated policy enforcement.
     *
     * @PatchMapping("/expire")
     * public ResponseEntity<Map<String, String>> expirePassword(
     *     @PathVariable Long userId,
     *     @RequestBody PasswordExpireRequest request)
     */

    /**
     * TODO: Bulk password expiration for compliance.
     * POST /api/admin/password/bulk-expire
     *
     * Expires passwords for multiple users matching criteria:
     * - Users with passwords older than X days
     * - Users in specific roles
     * - Users matching custom filters
     *
     * @PostMapping("/bulk-expire")
     * public ResponseEntity<BulkOperationResult> bulkExpirePasswords(
     *     @RequestBody BulkPasswordExpireRequest request)
     */
}