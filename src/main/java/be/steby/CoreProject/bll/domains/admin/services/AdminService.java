package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Admin service interface for user management operations.
 * This service orchestrates admin operations, captures request context,
 * and publishes events while delegating business logic to UserService.
 */
public interface AdminService {

    // ===============================
    // USER MANAGEMENT
    // ===============================

    /**
     * Activates or reactivates a user account as an admin.
     * Automatically determines whether it's first activation or reactivation.
     *
     * @param id User ID to activate
     * @param request HTTP request for context capture
     */
    //void activateUser(Long id, HttpServletRequest request);

    /**
     * Deactivates a user account as an admin.
     *
     * @param id User ID to deactivate
     * @param deactivationCategory Admin deactivation category
     * @param adminDeactivationDetails Admin deactivation details
     * @param request HTTP request for context capture
     */
    //void deactivateUser(Long id, AdminDeactivationCategory deactivationCategory,
                        //String adminDeactivationDetails, HttpServletRequest request);

    /**
     * Reactivates a previously deactivated user account as an admin.
     *
     * @param id User ID to reactivate
     * @param request HTTP request for context capture
     */
    //void reactivateUser(Long id, HttpServletRequest request);

    /**
     * Permanently deletes a user account (super admin only).
     *
     * @param id User ID to delete
     */
    //void deleteUser(Long id);

    /**
     * GDPR compliant user deletion with data anonymization (super admin only).
     *
     * @param user User to delete
     */
   // void gdprUserDelete(User user);

    // ===============================
    // ROLE MANAGEMENT
    // ===============================

    /**
     * Grants a role to a user as an admin.
     *
     * @param id User ID
     * @param role Role to grant
     */
   // void grantUserRole(Long id, UserRole role);

    /**
     * Revokes a role from a user as an admin.
     *
     * @param id User ID
     * @param role Role to revoke
     */
    //void revokeUserRole(Long id, UserRole role);

    // ===============================
    // PASSWORD MANAGEMENT
    // ===============================

    /**
     * Triggers a password reset for a user as an admin.
     *
     * @param id User ID
     */
    void triggerPasswordReset(Long id);

    // ===============================
    // QUERY OPERATIONS
    // ===============================

    /**
     * Searches users with a global query.
     *
     * @param query Search query
     * @param pageable Pagination settings
     * @return Page of users
     */
    Page<User> searchUsers(String query, Pageable pageable);

    /**
     * Searches users by specific criteria.
     *
     * @param username Username filter
     * @param firstname Firstname filter
     * @param lastname Lastname filter
     * @param email Email filter
     * @param phoneNumber Phone number filter
     * @param pageable Pagination settings
     * @return Page of users
     */
    Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                     String email, String phoneNumber, Pageable pageable);

    /**
     * Gets a user by ID (admin access required).
     *
     * @param id User ID
     * @return User entity
     */
    User getUserById(Long id);

    /**
     * Gets all devices for a user (admin access required).
     *
     * @param id User ID
     * @return List of user devices
     */
    List<Device> getUserDevices(Long id);

    /**
     * Gets total number of users in the system.
     *
     * @return Total user count
     */
    Long getTotalUsers();
}