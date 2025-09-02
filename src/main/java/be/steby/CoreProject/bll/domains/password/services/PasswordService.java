package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.domains.password.models.PasswordChangeRequest;
import be.steby.CoreProject.bll.domains.password.models.PasswordResetRequest;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.domains.password.exceptions.PasswordTokenValidityException;
import be.steby.CoreProject.dl.entities.User;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Service interface for password management operations
 * Handles password changes, resets, and security validations with comprehensive logging
 */
public interface PasswordService {

    // ================== CORE PASSWORD OPERATIONS ==================

    /**
     * Reset password using a valid token (anonymous users only)
     *
     * @param request Contains the new password
     * @param token Valid password reset token
     * @param httpRequest HTTP request for context and device detection
     * @throws InvalidPasswordException if password doesn't meet policy requirements
     * @throws PasswordTokenValidityException if token is invalid or expired
     */
    void resetPassword(PasswordResetRequest request, String token, HttpServletRequest httpRequest);

    /**
     * Change password for authenticated user
     *
     * @param request Contains current password, new password, and confirmation
     * @param httpRequest HTTP request for context and device detection
     * @throws InvalidPasswordException if current password is incorrect or new password doesn't meet policy
     */
    void changePassword(PasswordChangeRequest request, HttpServletRequest httpRequest);

    // ================== PASSWORD RESET REQUEST OPERATIONS ==================

    /**
     * Request password reset by email (anonymous users only)
     *
     * @param email Email address for password reset
     * @param request HTTP request for context
     */
    void requestPasswordReset(String email, HttpServletRequest request);

    /**
     * Request new password reset token when current token is expired (anonymous users only)
     *
     * @param token Expired or invalid token
     * @param httpRequest HTTP request for context
     * @throws PasswordTokenValidityException if token is still valid
     */
    void requestPasswordToken(String token, HttpServletRequest httpRequest);

    // ================== ADMINISTRATIVE OPERATIONS ==================

    /**
     * Force password change by administrator
     * Sets mustChangePassword flag and logs administrative action
     *
     * @param targetUser User whose password change is being forced
     * @param adminUsername Username of the administrator performing the action
     * @param httpRequest HTTP request for context
     */
    void forcePasswordChange(User targetUser, String adminUsername, HttpServletRequest httpRequest);

    // ================== ANALYSIS AND MONITORING OPERATIONS ==================

    /**
     * Get password activity statistics for a user
     *
     * @param user User to analyze
     * @return Password statistics including changes, failures, and reset requests
     */
    PasswordActivityLogService.PasswordStats getPasswordStats(User user);

    /**
     * Check if user has suspicious password activity in the given time window
     *
     * @param user User to check
     * @param hoursWindow Time window in hours to analyze
     * @return true if suspicious activity detected
     */
    boolean hasSuspiciousPasswordActivity(User user, int hoursWindow);
}