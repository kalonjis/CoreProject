package be.steby.CoreProject.bll.domains.admin.events.account;

import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Event published when a user is created by an administrator.
 * Contains all information necessary for audit logging and email notification.
 * No confirmation token - user confirms account by first login with temporary password.
 */
public record AdminUserCreatedEvent(
        /**
         * The user that was just created.
         */
        User createdUser,

        /**
         * The administrator who performed the creation.
         */
        User adminUser,

        /**
         * The temporary password to include in the email.
         */
        String temporaryPassword,

        /**
         * Timestamp of the event.
         */
        Instant timestamp
) {

    /**
     * Constructor with automatic timestamp.
     *
     * @param createdUser The user that was created
     * @param adminUser The admin who created the user
     * @param temporaryPassword The temporary password
     */
    public AdminUserCreatedEvent(
            User createdUser,
            User adminUser,
            String temporaryPassword) {
        this(createdUser, adminUser, temporaryPassword, Instant.now());
    }

    /**
     * Factory method for creating admin user creation event.
     *
     * @param createdUser The user that was created
     * @param adminUser The admin who created the user
     * @param temporaryPassword The temporary password
     * @return A new AdminUserCreatedEvent instance
     */
    public static AdminUserCreatedEvent of(
            User createdUser,
            User adminUser,
            String temporaryPassword) {
        return new AdminUserCreatedEvent(createdUser, adminUser, temporaryPassword);
    }

    /**
     * Gets the created user's ID.
     *
     * @return The user ID
     */
    public Long getCreatedUserId() {
        return createdUser.getId();
    }

    /**
     * Gets the admin user's ID.
     *
     * @return The admin ID
     */
    public Long getAdminUserId() {
        return adminUser.getId();
    }

    /**
     * Gets the created user's username.
     *
     * @return The username
     */
    public String getCreatedUsername() {
        return createdUser.getUsername();
    }

    /**
     * Gets the admin user's username.
     *
     * @return The admin username
     */
    public String getAdminUsername() {
        return adminUser.getUsername();
    }

    /**
     * Gets the created user's email.
     *
     * @return The user email
     */
    public String getCreatedUserEmail() {
        return createdUser.getEmail();
    }
}