package be.steby.CoreProject.bll.domains.admin.events.account;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Event published when a user is deleted by an administrator.
 * Contains all information necessary for audit logging.
 *
 * <p>Note: Since the user is deleted, we capture identifying info at event time
 * rather than keeping a reference to the (soon to be deleted) entity.</p>
 */
public record AdminUserDeletedEvent(
        /**
         * The ID of the deleted user.
         */
        Long deletedUserId,

        /**
         * The username of the deleted user.
         */
        String deletedUsername,

        /**
         * The email of the deleted user.
         */
        String deletedEmail,

        /**
         * The administrator who performed the deletion.
         */
        User adminUser,

        /**
         * The device used to perform the deletion.
         */
        Device device,

        /**
         * Whether this was a GDPR deletion (anonymization) or hard delete.
         */
        boolean gdprDeletion,

        /**
         * Timestamp of the event.
         */
        Instant timestamp
) {

    /**
     * Constructor with automatic timestamp.
     */
    public AdminUserDeletedEvent(
            Long deletedUserId,
            String deletedUsername,
            String deletedEmail,
            User adminUser,
            Device device,
            boolean gdprDeletion) {
        this(deletedUserId, deletedUsername, deletedEmail, adminUser, device, gdprDeletion, Instant.now());
    }

    /**
     * Creates a deletion event from the user entity (call before deletion).
     *
     * @param deletedUser The user being deleted
     * @param adminUser   The admin performing the deletion
     * @param gdprDeletion Whether this is a GDPR deletion
     * @return New event instance
     */
    public static AdminUserDeletedEvent of(User deletedUser, User adminUser, Device device, boolean gdprDeletion) {
        return new AdminUserDeletedEvent(
                deletedUser.getId(),
                deletedUser.getUsername(),
                deletedUser.getEmail(),
                adminUser,
                device,
                gdprDeletion
        );
    }

    /**
     * Creates a hard deletion event.
     */
    public static AdminUserDeletedEvent hardDelete(User deletedUser, User adminUser, Device device) {
        return of(deletedUser, adminUser, device, false);
    }

    /**
     * Creates a GDPR deletion event.
     */
    public static AdminUserDeletedEvent gdprDelete(User deletedUser, User adminUser, Device device) {
        return of(deletedUser, adminUser, device, true);
    }

    /**
     * Gets the admin username.
     */
    public String getAdminUsername() {
        return adminUser.getUsername();
    }

    /**
     * Gets the admin ID.
     */
    public Long getAdminUserId() {
        return adminUser.getId();
    }
}