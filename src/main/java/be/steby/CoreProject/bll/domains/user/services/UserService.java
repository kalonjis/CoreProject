package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Core service contract for user entity operations.
 *
 * <p>This service is the <strong>sole owner of user state mutations</strong>.
 * No other service should call setters on a {@link User} entity directly —
 * all state changes must go through this contract.
 *
 * <h3>Separation of Concerns</h3>
 * <ul>
 *   <li><strong>UserService</strong> — mutates user state, persists, invalidates cache.</li>
 *   <li><strong>AccountService</strong> — self-service lifecycle orchestration (tokens, emails, events).</li>
 *   <li><strong>AdminUserAccountService</strong> — admin orchestration (permission checks, policy validation, events).</li>
 * </ul>
 *
 * <h3>Method Naming Convention</h3>
 * <ul>
 *   <li>Methods without prefix → self-service operations (user acts on their own account).</li>
 *   <li>Methods prefixed with {@code admin} → administrative operations (admin acts on another user).</li>
 * </ul>
 *
 * <h3>Permission Model</h3>
 * <p>This service carries <strong>no permission guards</strong> except for
 * {@link #deleteUser(Long)}, which is an irreversible destructive operation.
 * All other callers (AccountService, AdminUserAccountService) are responsible
 * for enforcing access control before invoking these methods.
 */
@Service
public interface UserService {

    // =========================================================================
    // SEARCH & QUERY
    // =========================================================================

    /**
     * Searches users across multiple fields (username, firstname, lastname, email, phone).
     *
     * @param query    global search term, or {@code null} to return all users
     * @param pageable pagination and sorting settings
     * @return paginated list of matching users
     */
    Page<User> searchUsers(String query, Pageable pageable);

    /**
     * Searches users by individual field criteria. All parameters are optional.
     *
     * @param username    partial username filter (case-insensitive), or {@code null}
     * @param firstname   partial firstname filter (case-insensitive), or {@code null}
     * @param lastname    partial lastname filter (case-insensitive), or {@code null}
     * @param email       partial email filter (case-insensitive), or {@code null}
     * @param phoneNumber partial phone number filter, or {@code null}
     * @param pageable    pagination and sorting settings
     * @return paginated list of matching users
     */
    Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                     String email, String phoneNumber, Pageable pageable);

    /**
     * Retrieves a user by their internal database ID.
     *
     * @param id the internal database ID
     * @return the matching user entity
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user exists with that ID
     */
    User getUserById(Long id);

    /**
     * Retrieves a user by their username.
     *
     * @param username the username to look up
     * @return the matching user entity
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user exists with that username
     */
    User getUserByUsername(String username);

    /**
     * Retrieves a user by their public UUID (used in URLs and API responses).
     *
     * @param publicId the public UUID string
     * @return the matching user entity
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user exists with that publicId
     */
    User getUserByPublicId(String publicId);

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address to look up
     * @return the matching user entity
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user exists with that email
     */
    User getUserByEmail(String email);

    /**
     * Retrieves a user by username or email (login identifier).
     *
     * @param identifier username or email address
     * @return the matching user entity
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user is found
     */
    User getUserByUsernameOrByEmail(String identifier);

    /**
     * Returns the total number of registered users.
     *
     * @return total user count
     */
    Long getTotalUsers();

    // =========================================================================
    // PERSISTENCE
    // =========================================================================

    /**
     * Persists a user entity and publishes a {@code UserPersistedEvent} for cache invalidation.
     *
     * <p>This is the <strong>only</strong> method that should be used to save a user.
     * Direct repository calls bypass cache management and must be avoided.
     *
     * @param user the user entity to save
     * @return the saved user entity (with generated ID if newly created)
     */
    User saveUser(User user);

    // =========================================================================
    // SELF — Activation
    // Precondition: everActivated == false
    // =========================================================================

    /**
     * Activates a user account following self-signup email confirmation.
     *
     * <p>Mutated fields:
     * <ul>
     *   <li>{@code enabled} → {@code true}</li>
     *   <li>{@code everActivated} → {@code true}</li>
     *   <li>{@code activatedAt} → now</li>
     * </ul>
     *
     * <p>Note: {@code emailVerified} is handled separately via {@link #setUserMailVerified(User)}.
     *
     * @param id the internal database ID of the user to activate
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user exists with that ID
     * @throws be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException if the user is already active
     */
    void activateUser(Long id);

    // =========================================================================
    // SELF — Deactivation
    // =========================================================================

    /**
     * Deactivates a user account at the user's own request.
     *
     * <p>Mutated fields:
     * <ul>
     *   <li>{@code enabled} → {@code false}</li>
     *   <li>{@code deactivatedAt} → now</li>
     *   <li>{@code deactivationReason} → provided reason</li>
     *   <li>{@code deactivationDetails} → provided details</li>
     * </ul>
     *
     * <p>Note: does NOT touch admin deactivation fields ({@code adminDeactivationReason}, etc.).
     *
     * @param id            the internal database ID of the user to deactivate
     * @param reason        the self-deactivation reason
     * @param reasonDetails optional free-text details provided by the user
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user exists with that ID
     * @throws be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException if the user is already deactivated
     */
    void deactivateUser(Long id, DeactivationReason reason, String reasonDetails);

    // =========================================================================
    // SELF — Reactivation
    // Precondition: everActivated == true && enabled == false
    // =========================================================================

    /**
     * Reactivates a self-deactivated user account.
     *
     * <p>Mutated fields:
     * <ul>
     *   <li>{@code enabled} → {@code true}</li>
     *   <li>{@code reactivatedAt} → now</li>
     *   <li>{@code reactivatedBy} → the user themselves</li>
     *   <li>{@code reactivationPolicy} → determined by {@code ReactivationPolicyService}</li>
     *   <li>{@code deactivationReason}, {@code deactivationDetails}, {@code deactivatedAt} → cleared</li>
     * </ul>
     *
     * <p>Eligibility is verified internally via {@code ReactivationPolicyService} before any mutation.
     *
     * @param user the user to reactivate
     * @throws be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException if the user is already active
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if reactivation is not allowed by policy
     */
    void reactivateUser(User user);

    // =========================================================================
    // SELF — GDPR Deletion
    // =========================================================================

    /**
     * Anonymizes the user's personal data under the GDPR right-to-erasure flow.
     *
     * <p>Mutated fields:
     * <ul>
     *   <li>{@code email} → {@code deleted_<id>@anonymized.local}</li>
     *   <li>{@code firstname} → {@code User}</li>
     *   <li>{@code lastname} → {@code Deleted}</li>
     *   <li>{@code phoneNumber} → {@code null}</li>
     *   <li>{@code enabled} → {@code false}</li>
     *   <li>{@code gdprDeletedAt} → now</li>
     * </ul>
     *
     * <p>This method carries <strong>no permission guard</strong>.
     * Callers are responsible for enforcing access control before invoking this method.
     *
     * @param user the user entity to anonymize
     */
    void anonymizeUser(User user);

    // =========================================================================
    // ADMIN — Activation (first-time only)
    // Precondition: everActivated == false
    // =========================================================================

    /**
     * Activates a user account created by an administrator (never logged in before).
     *
     * <p>Differs from {@link #activateUser(Long)} in that the admin is recorded
     * as the activating actor and email is marked verified immediately
     * (the admin verified the user's identity during creation).
     *
     * <p>Mutated fields:
     * <ul>
     *   <li>{@code enabled} → {@code true}</li>
     *   <li>{@code everActivated} → {@code true}</li>
     *   <li>{@code activatedAt} → now</li>
     *   <li>{@code activatedBy} → admin actor</li>
     *   <li>{@code emailVerified} → {@code true}</li>
     * </ul>
     *
     * @param target the user to activate
     * @param admin  the administrator performing the activation
     * @throws be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException if the user is already active
     * @throws IllegalStateException if the user was already activated before (use {@link #adminReactivateUser} instead)
     */
    void adminActivateUser(User target, User admin);

    // =========================================================================
    // ADMIN — Deactivation
    // =========================================================================

    /**
     * Deactivates a user account by administrative decision.
     *
     * <p>Distinct from self-deactivation: uses a separate set of fields
     * ({@code adminDeactivation*}) so the audit trail is never mixed with self-deactivation data.
     *
     * <p>Mutated fields:
     * <ul>
     *   <li>{@code enabled} → {@code false}</li>
     *   <li>{@code adminDeactivatedAt} → now</li>
     *   <li>{@code adminDeactivationReason} → provided category</li>
     *   <li>{@code adminDeactivationDetails} → provided details</li>
     *   <li>{@code adminDeactivatedBy} → admin actor</li>
     * </ul>
     *
     * <p>Note: does NOT touch self-deactivation fields ({@code deactivationReason}, etc.).
     *
     * @param target   the user to deactivate
     * @param admin    the administrator performing the deactivation
     * @param category the administrative deactivation category
     * @param details  mandatory free-text justification
     * @throws be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException if the user is already deactivated
     */
    void adminDeactivateUser(User target, User admin,
                             AdminDeactivationCategory category,
                             String details);

    // =========================================================================
    // ADMIN — Reactivation
    // Precondition: everActivated == true && enabled == false
    // =========================================================================

    /**
     * Reactivates a user account previously deactivated (by admin or by self).
     *
     * <p>Compared to {@link #reactivateUser(User)}, this method additionally clears
     * admin deactivation fields when the account was admin-deactivated,
     * and applies admin-specific reactivation policies.
     *
     * <p>Mutated fields:
     * <ul>
     *   <li>{@code enabled} → {@code true}</li>
     *   <li>{@code reactivatedAt} → now</li>
     *   <li>{@code reactivatedBy} → admin actor</li>
     *   <li>{@code reactivationPolicy} → determined by {@code ReactivationPolicyService}</li>
     *   <li>{@code deactivationReason}, {@code deactivationDetails}, {@code deactivatedAt} → cleared</li>
     *   <li>If admin-deactivated: {@code adminDeactivationReason}, {@code adminDeactivationDetails},
     *       {@code adminDeactivatedAt}, {@code adminDeactivatedBy} → cleared</li>
     * </ul>
     *
     * @param target the user to reactivate
     * @param admin  the administrator performing the reactivation
     * @throws be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException if the user is already active
     * @throws IllegalStateException if the user was never activated before (use {@link #adminActivateUser} instead)
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if reactivation is blocked by policy
     */
    void adminReactivateUser(User target, User admin);

    // =========================================================================
    // ADMIN — Hard Deletion (SUPER_ADMIN only)
    // =========================================================================

    /**
     * Permanently removes a user record and all associated data from the database.
     *
     * <p>This is an irreversible, destructive operation. For most cases,
     * deactivation should be preferred. Use this only in exceptional circumstances
     * (e.g., test accounts, duplicate entries, legal obligation).
     *
     * <p>This method carries <strong>no permission guard</strong>.
     * The caller (AdminUserAccountService) is solely responsible for
     * validating SUPER_ADMIN authority before invoking this method.
     *
     * @param id the internal database ID of the user to delete
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if no user exists with that ID
     */
    void deleteUser(Long id);

    // =========================================================================
    // VALIDATION & CHECKS
    // =========================================================================

    /**
     * Marks the user's email address as verified.
     *
     * @param user the user whose email to mark as verified
     */
    void setUserMailVerified(User user);

    /**
     * Checks whether a user already exists in the system (by email or username).
     * Throws if the user already exists.
     *
     * @param user the user entity to check
     */
    void checkIfUserExists(User user);

    /**
     * Returns whether a username is already taken.
     *
     * @param username the username to check
     * @return {@code true} if the username exists
     */
    boolean existsByUsername(String username);

    /**
     * Returns whether an email address is already registered.
     *
     * @param email the email to check
     * @return {@code true} if the email exists
     */
    boolean existsByEmail(String email);

    // =========================================================================
    // AUTHENTICATION & SECURITY
    // =========================================================================

    /**
     * Returns the currently authenticated user from the security context.
     *
     * @return the authenticated user entity
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if no authenticated user is found
     */
    User getAuthenticatedUser();

    /**
     * Returns whether the current security context is anonymous (unauthenticated).
     *
     * @return {@code true} if the current user is anonymous
     */
    boolean isAnonymous();

    /**
     * Returns whether the authenticated user holds the given role.
     *
     * @param role the role to check
     * @return {@code true} if the authenticated user has the role
     */
    boolean authenticatedHasRole(UserRole role);

}