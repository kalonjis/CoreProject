package be.steby.CoreProject.bll.domains.admin.services.search;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for admin search and query operations on users.
 * Handles user searches, retrieval, and statistics with proper permission validation.
 *
 * This service focuses exclusively on read-only query operations.
 * For user account modifications, see AdminUserAccountService.
 * For role management, see AdminRoleService.
 *
 * All methods require admin privileges and perform permission checks.
 */
public interface AdminSearchService {

    // ===============================
    // USER SEARCH OPERATIONS
    // ===============================

    /**
     * Searches users with a global query across multiple fields.
     * Searches in: username, firstname, lastname, email, phone number.
     *
     * @param query Search query (searches across all user fields)
     * @param pageable Pagination and sorting settings
     * @return Paginated list of users matching the query
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks admin privileges
     */
    Page<User> searchUsers(String query, Pageable pageable);

    /**
     * Searches users by specific criteria with individual field filters.
     * All criteria are optional (null values are ignored).
     *
     * @param username Username filter (partial match, case-insensitive)
     * @param firstname First name filter (partial match, case-insensitive)
     * @param lastname Last name filter (partial match, case-insensitive)
     * @param email Email filter (partial match, case-insensitive)
     * @param phoneNumber Phone number filter (partial match)
     * @param pageable Pagination and sorting settings
     * @return Paginated list of users matching the criteria
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks admin privileges
     */
    Page<User> searchUsersByCriteria(
            String username,
            String firstname,
            String lastname,
            String email,
            String phoneNumber,
            Pageable pageable
    );

    // ===============================
    // USER RETRIEVAL OPERATIONS
    // ===============================

    /**
     * Retrieves a user by their ID.
     *
     * @param userId User ID
     * @return User entity
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks admin privileges
     */
    User getUserById(Long userId);

    /**
     * Retrieves all devices associated with a user.
     *
     * @param userId User ID
     * @return List of user's devices
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks admin privileges
     */
    List<Device> getUserDevices(Long userId);

    // ===============================
    // STATISTICS OPERATIONS
    // ===============================

    /**
     * Gets the total number of users in the system.
     *
     * @return Total user count
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks admin privileges
     */
    Long getTotalUsers();
}