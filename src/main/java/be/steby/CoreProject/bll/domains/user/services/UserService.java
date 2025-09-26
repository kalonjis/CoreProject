package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    // ===============================
    // SEARCH AND QUERY OPERATIONS
    // ===============================

    Page<User> searchUsers(String query, Pageable pageable);

    Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                     String email, String phoneNumber, Pageable pageable);

    User getUserById(Long id);

    User getUserByUsername(String username);

    User getUserByEmail(String email);

    User saveUser(User user);

    Long getTotalUsers();

    // ===============================
    // USER ACTIVATION OPERATIONS
    // ===============================

    /**
     * Self-activation of user account (usually after email confirmation).
     */
    void activateUser(Long id);

    /**
     * Admin activation of user account (first-time activation only).
     * Use this when user was never activated before.
     */
    void adminActivateUser(User target, User admin);

    // ===============================
    // USER DEACTIVATION OPERATIONS
    // ===============================

    /**
     * Self-deactivation of user account.
     */
    void deactivateUser(Long id, DeactivationReason reason, String reasonDetails);

    /**
     * Admin deactivation of user account.
     */
    void adminDeactivateUser(User target, User admin, AdminDeactivationCategory deactivationCategory,
                             String adminDeactivationDetails);

    // ===============================
    // USER REACTIVATION OPERATIONS
    // ===============================

    /**
     * Self-reactivation of previously deactivated user account.
     */
    void reactivateUser(User user);

    /**
     * Admin reactivation of previously deactivated user account.
     * Use this when user was deactivated and needs to be reactivated.
     */
    void adminReactivateUser(User target, User admin);

    // ===============================
    // USER DELETION OPERATIONS
    // ===============================

    /**
     * Permanently delete user account (super admin only).
     */
    void deleteUser(Long id);

    /**
     * GDPR compliant user deletion with data anonymization (super admin only).
     */
    void gdprUserDelete(User user);

    // ===============================
    // ROLE MANAGEMENT OPERATIONS
    // ===============================

    void grantUserRole(Long id, UserRole role);

    void revokeUserRole(Long id, UserRole role);

    // ===============================
    // USER VALIDATION AND CHECKS
    // ===============================

    void setUserMailVerified(User user);

    void checkIfUserExists(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // ===============================
    // AUTHENTICATION AND AUTHORIZATION
    // ===============================

    User getAuthenticatedUser();

    boolean isAnonymous();

    boolean authenticatedHasRole(UserRole role);

    void requireAdminPermissions();

    void requireSuperAdminPermissions();
}