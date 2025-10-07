package be.steby.CoreProject.bll.domains.admin.services.password;

import be.steby.CoreProject.bll.domains.admin.models.password.AdminPasswordResetBLLRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for administrative password management operations.
 *
 * This service handles password-related admin operations with multiple strategies:
 * - Standard reset: Email link for user to reset password
 * - Security breach: Immediate password revocation and session invalidation
 * - Temporary password: Generate temporary password for alternative delivery
 * - Force expire: Mark password as expired for compliance
 *
 * All operations require ADMIN privileges and are fully audited.
 *
 * Security considerations:
 * - All operations are logged for compliance and audit
 * - Security-related resets trigger high-priority alerts
 * - Temporary passwords are never logged in production
 * - Session invalidation is atomic with password changes
 */
public interface AdminPasswordService {

    /**
     * Forces a password reset for a user as an administrator.
     * The strategy in the request determines the exact behavior.
     *
     * Operation flow:
     * 1. Validates admin permissions
     * 2. Executes strategy-specific logic:
     *    - STANDARD_RESET: Generate reset token, send email
     *    - SECURITY_BREACH: Revoke password, invalidate sessions, send reset email
     *    - TEMPORARY_PASSWORD: Generate temp password, send via alternative channel
     *    - FORCE_EXPIRE: Mark password expired, send notification
     * 3. Publishes audit event with full context
     * 4. Logs operation for compliance
     *
     * @param userId ID of the user requiring password reset
     * @param request Reset request with strategy, reason, and options
     * @param httpRequest HTTP request for context capture (IP, user agent, etc.)
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks admin privileges
     * @throws IllegalArgumentException if request contains invalid data or violates business rules
     */
    void forcePasswordReset(Long userId,
                            AdminPasswordResetBLLRequest request,
                            HttpServletRequest httpRequest);
}