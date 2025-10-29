package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface UserService {

    // ===============================
    // SEARCH AND QUERY OPERATIONS
    // ===============================

    Page<User> searchUsers(String query, Pageable pageable);

    Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                     String email, String phoneNumber, Pageable pageable);

//    User getByOauthProviderAndOauthProviderId(String provider, String providerId);

    /**
     * Finds a user for OAuth reconciliation using a two-step strategy:
     * 1. By email (if valid email provided and not a temporary OAuth email)
     * 2. By OAuth provider credentials
     *
     * This method is used during OAuth login to determine if a user already exists
     * before creating a new account.
     *
     * @param provider OAuth provider name (e.g., GITHUB, GOOGLE)
     * @param providerId User ID from OAuth provider
     * @param email User email from OAuth provider (may be null or temporary)
     * @return Optional containing user if found by either strategy, empty otherwise
     */
    Optional<User> findUserForOAuthReconciliation(String provider, String providerId, String email);



    User getUserById(Long id);

    User getUserByUsername(String username);

    User getUserByPublicId(String public_id);

    User getUserByEmail(String email);

    User getUserByUsernameOrByEmail(String identifier);

    User saveUser(User user);

    Long getTotalUsers();

    // ===============================
    // USER ACTIVATION OPERATIONS
    // ===============================

    /**
     * Self-activation of user account (usually after email confirmation).
     */
    void activateUser(Long id);



    // ===============================
    // USER DEACTIVATION OPERATIONS
    // ===============================

    /**
     * Self-deactivation of user account.
     */
    void deactivateUser(Long id, DeactivationReason reason, String reasonDetails);


    // ===============================
    // USER REACTIVATION OPERATIONS
    // ===============================

    /**
     * Self-reactivation of previously deactivated user account.
     */
    void reactivateUser(User user);


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