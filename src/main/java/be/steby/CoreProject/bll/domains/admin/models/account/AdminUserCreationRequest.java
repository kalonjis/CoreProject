package be.steby.CoreProject.bll.domains.admin.models.account;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.dl.enums.UserRole;

import java.util.Set;

/**
 * Request for creating a user by an administrator.
 * Contains only the essential information an admin must provide.
 * Username is auto-generated from email (consistent with self-signup).
 */
public record AdminUserCreationRequest(
        /**
         * User's email address (required).
         */
        String email,

        /**
         * User's first name (required for identification).
         */
        String firstname,

        /**
         * User's last name (required for identification).
         */
        String lastname,

        /**
         * User's phone number (optional but recommended for employees).
         */
        String phoneNumber,

        /**
         * Roles to assign to the user (required, minimum USER role).
         */
        Set<UserRole> userRoles
) {

    /**
     * Compact constructor with validation.
     */
    public AdminUserCreationRequest {
        if (email == null || email.isBlank()) {
            throw new AdminOperationException("Email cannot be empty");
        }
        if (firstname == null || firstname.isBlank()) {
            throw new AdminOperationException("First name cannot be empty");
        }
        if (lastname == null || lastname.isBlank()) {
            throw new AdminOperationException("Last name cannot be empty");
        }
        if (userRoles == null || userRoles.isEmpty()) {
            throw new AdminOperationException("At least one role must be assigned");
        }
        // Note: We don't enforce USER role presence - admin explicitly chooses roles
    }

    /**
     * Checks if this creation requires SUPER_ADMIN privileges.
     */
    public boolean requiresSuperAdminPrivileges() {
        return userRoles.contains(UserRole.SUPER_ADMIN);
    }

    /**
     * Checks if this creation requires admin privileges.
     */
    public boolean requiresAdminPrivileges() {
        return userRoles.stream().anyMatch(role ->
                role == UserRole.ADMIN ||
                        role == UserRole.SUPER_ADMIN ||
                        role == UserRole.MODERATOR
        );
    }

    /**
     * Factory method for creating a basic user with USER role only.
     */
    public static AdminUserCreationRequest forBasicUser(
            String email,
            String firstname,
            String lastname) {
        return new AdminUserCreationRequest(
                email,
                firstname,
                lastname,
                null, // No phone
                Set.of(UserRole.USER)
        );
    }
}