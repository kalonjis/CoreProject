package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.dl.entities.User;

/**
 * Result of admin user creation operation.
 * Contains the created user and temporary password.
 * No confirmation token needed - user confirms by first login.
 */
public record AdminUserCreationResult(
        /**
         * The created user entity.
         */
        User user,

        /**
         * The temporary password generated for the user.
         * This will be sent via email and must be changed on first login.
         */
        String temporaryPassword
) {
    /**
     * Factory method for successful admin user creation.
     *
     * @param user The created user
     * @param tempPassword The temporary password
     * @return A new AdminUserCreationResult instance
     */
    public static AdminUserCreationResult of(User user, String tempPassword) {
        return new AdminUserCreationResult(user, tempPassword);
    }
}