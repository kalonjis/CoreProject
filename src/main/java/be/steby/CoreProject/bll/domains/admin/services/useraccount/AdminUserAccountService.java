package be.steby.CoreProject.bll.domains.admin.services.useraccount;

import be.steby.CoreProject.bll.domains.admin.models.account.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationRequest;
import be.steby.CoreProject.bll.common.exceptions.UserPermissionException;
import be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;

/**
 * Service for admin operations on user accounts.
 * Handles user lifecycle management: creation, activation, deactivation, reactivation, and deletion.
 *
 * This service focuses exclusively on account state management.
 * For role management, see AdminRoleService.
 * For search operations, see AdminSearchService.
 */
public interface AdminUserAccountService {

    // ===============================
    // USER CREATION
    // ===============================

    /**
     * Creates a new user account as an administrator.
     *
     * The created user will have:
     * - Email verified (admin verified identity)
     * - Account disabled until first login
     * - Temporary password (must be changed on first login)
     * - Complete profile (firstname/lastname provided by admin)
     *
     * @param request Admin user creation request with all required data
     * @return The created user entity
     * @throws AdminOperationException if validation fails
     * @throws UserPermissionException if actor lacks permission to assign requested roles
     */
    User createUser(AdminUserCreationRequest request);

    // ===============================
    // USER ACTIVATION
    // ===============================

    /**
     * Activates or reactivates a user account as an administrator.
     * Automatically determines whether it's a first activation or reactivation
     * based on the user's activation history.
     *
     * First activation: User created by admin, never logged in before
     * Reactivation: User was previously activated and then deactivated
     *
     * @param publicId publicId of the user to activate
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks permission
     * @throws AttributeUnchangedException if user is already active
     */
    void activateUser(String publicId);

    // ===============================
    // USER DEACTIVATION
    // ===============================

    /**
     * Deactivates a user account as an administrator.
     *
     * Admin deactivation is distinct from user self-deactivation:
     * - Requires administrative privileges
     * - Includes categorization and detailed reason
     * - May have different reactivation policies
     * - Logged separately for audit purposes
     *
     * @param publicId publicId of the user to deactivate
     * @param deactivationCategory Admin deactivation category (policy violation, security, etc.)
     * @param adminDeactivationDetails Detailed reason for deactivation (required)
     * @throws UserNotFoundException if user doesn't exist
     * @throws UserPermissionException if actor lacks permission
     * @throws AttributeUnchangedException if user is already deactivated
     */
    void deactivateUser(String publicId,
                        AdminDeactivationCategory deactivationCategory,
                        String adminDeactivationDetails
    );

    /**
     * Deactivates a user account with a structured deactivation request.
     * Convenience method that accepts a request object instead of individual parameters.
     *
     * @param publicId publicId of the user to deactivate
     * @param request Deactivation request with category and details
     * @throws UserNotFoundException if user doesn't exist
     * @throws UserPermissionException if actor lacks permission
     * @throws AttributeUnchangedException if user is already deactivated
     */
    void deactivateUser(String publicId,
                        AdminDeactivationRequest request);

    // ===============================
    // USER REACTIVATION
    // ===============================

    /**
     * Reactivates a previously deactivated user account as an administrator.
     *
     * This method is specifically for reactivating users who were deactivated.
     * For first-time activation of newly created accounts, use activateUser() instead.
     *
     * Reactivation policies may vary based on:
     * - How the account was deactivated (admin vs self-deactivation)
     * - Deactivation category and reason
     * - Time elapsed since deactivation
     *
     * @param publicId publicId of the user to reactivate
     * @throws UserNotFoundException if user doesn't exist
     * @throws UserPermissionException if actor lacks permission
     * @throws AttributeUnchangedException if user is already active
     * @throws IllegalStateException if user was never activated before
     */
    //void reactivateUser(String publicId);

    // ===============================
    // USER DELETION
    // ===============================

    /**
     * Permanently deletes a user account (super admin only).
     *
     * This is a destructive operation that:
     * - Removes the user entity from the database
     * - Cascade deletes associated data (tokens, sessions, etc.)
     * - Cannot be undone
     *
     * WARNING: This should only be used in exceptional circumstances.
     * For most cases, deactivation is preferred over deletion.
     *
     * @param publicId publicId of the user to delete
     * @throws UserNotFoundException if user doesn't exist
     * @throws UserPermissionException if actor is not a super admin
     */
    void deleteUser(String publicId);

    /**
     * GDPR compliant user deletion with data anonymization.
     *
     * Unlike permanent deletion, this method:
     * - Anonymizes personal data (email, phone, name → "Deleted")
     * - Preserves user record for referential integrity
     * - Marks account as deactivated with GDPR_REQUEST reason
     * - Maintains audit trail while removing PII
     *
     * This is the preferred deletion method for GDPR compliance.
     *
     * @param user User entity to anonymize and delete
     * @throws UserPermissionException if actor is not a super admin
     */
    void gdprUserDelete(User user);

    // ===============================
    // PASSWORD MANAGEMENT
    // ===============================

    /**
     * Triggers a password reset for a user as an administrator.
     *
     * This generates a password reset token and sends it to the user's email.
     * The user can then reset their password using the token.
     *
     * Use cases:
     * - User forgot password and requested admin help
     * - Security measure after suspicious activity
     * - Forced password change for compliance
     *
     * @param publicId publicId of the user requiring password reset
     * @throws UserNotFoundException if user doesn't exist
     * @throws UserPermissionException if actor lacks admin privileges
     */
    void triggerPasswordReset(String publicId);
}