package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.password.AdminPasswordService;
import be.steby.CoreProject.pl.domains.admin.models.AdminPasswordResetRequest;
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
@PatchMapping("/{userId})")
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