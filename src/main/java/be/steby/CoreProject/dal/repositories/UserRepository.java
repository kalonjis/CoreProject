package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entity persistence operations.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic criteria queries
 * used by admin search and filtering features (see {@code UserSpecification}).
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Simple lookups → Spring Data derived method names (auto-generated SQL)</li>
 *   <li>Multi-field search → {@code @Query} JPQL (kept here for transparency)</li>
 *   <li>Dynamic filtering → {@link JpaSpecificationExecutor} + {@code UserSpecification}</li>
 * </ul>
 *
 * <h3>Search queries</h3>
 * <p>The inline {@code @Query} search methods ({@link #searchByMultipleFields},
 * {@link #searchByCriteria}) are kept as fallback options.
 * Prefer {@code userRepository.findAll(UserSpecification.xxx(), pageable)}
 * for complex or composable queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // =========================================================================
    // Lookup by identifier
    // =========================================================================

    /**
     * Finds a user by username (case-insensitive).
     *
     * @param username the username to look up
     * @return the matching user, or empty
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Finds a user by email address (case-insensitive).
     *
     * @param email the email to look up
     * @return the matching user, or empty
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Finds a user by their public UUID (used in URLs and API responses).
     *
     * @param publicId the public UUID string
     * @return the matching user, or empty
     */
    Optional<User> findByPublicId(String publicId);

    /**
     * Finds a user by username or email — used for login and reactivation flows
     * where the caller may provide either identifier.
     *
     * <p>A single parameter is matched against both fields (case-insensitive),
     * so the caller never needs to duplicate the value.
     *
     * @param identifier username or email address
     * @return the matching user, or empty
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:identifier) OR LOWER(u.email) = LOWER(:identifier)")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);


    // =========================================================================
    // Existence checks
    // =========================================================================

    /**
     * Returns whether a username is already taken (case-insensitive).
     *
     * @param username the username to check
     * @return {@code true} if taken
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Returns whether an email address is already registered (case-insensitive).
     *
     * @param email the email to check
     * @return {@code true} if registered
     */
    boolean existsByEmailIgnoreCase(String email);

    // =========================================================================
    // Role-based lookup
    // =========================================================================

    /**
     * Returns all active users holding a given role, ordered by lastname then firstname.
     *
     * @param role the role to filter by
     * @return list of matching users
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles r WHERE r = :role AND u.enabled = true ORDER BY u.lastname, u.firstname")
    List<User> findActiveByRole(@Param("role") UserRole role);

    // =========================================================================
    // Statistics
    // =========================================================================

    /**
     * Counts users by enabled status.
     *
     * @param enabled {@code true} for active users, {@code false} for deactivated
     * @return the count
     */
    Long countByEnabled(boolean enabled);

    /**
     * Counts users by email verification status.
     *
     * @param emailVerified {@code true} for verified, {@code false} for unverified
     * @return the count
     */
    Long countByEmailVerified(boolean emailVerified);

    /**
     * Counts users holding at least one administrative role (ADMIN or SUPER_ADMIN).
     *
     * @return the count of admin users
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.userRoles r WHERE r IN ('ADMIN', 'SUPER_ADMIN')")
    Long countUsersWithAdminRoles();

    // =========================================================================
    // Search queries
    // =========================================================================

    /**
     * Global full-text search across username, firstname, lastname, email, and phone.
     *
     * <p>Prefer {@code findAll(UserSpecification.searchInAllFields(query), pageable)}
     * for composable or dynamically built queries.
     *
     * @param query    the search term
     * @param pageable pagination and sorting
     * @return paginated matching users
     */
    @Query("""
            SELECT u FROM User u WHERE
            LOWER(u.username)    LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.firstname)   LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.lastname)    LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.email)       LIKE LOWER(CONCAT('%', :query, '%')) OR
            u.phoneNumber        LIKE CONCAT('%', :query, '%')
            """)
    Page<User> searchByMultipleFields(@Param("query") String query, Pageable pageable);

    /**
     * Filtered search with optional per-field criteria. Any {@code null} parameter is ignored.
     *
     * <p>Prefer {@code findAll(UserSpecification.searchByCriteria(...), pageable)}
     * for composable or dynamically built queries.
     *
     * @param username    partial username filter, or {@code null}
     * @param firstname   partial firstname filter, or {@code null}
     * @param lastname    partial lastname filter, or {@code null}
     * @param email       partial email filter, or {@code null}
     * @param phoneNumber partial phone number filter, or {@code null}
     * @param pageable    pagination and sorting
     * @return paginated matching users
     */
    @Query("""
            SELECT u FROM User u WHERE
            (:username    IS NULL OR LOWER(u.username)  LIKE LOWER(CONCAT('%', :username,    '%'))) AND
            (:firstname   IS NULL OR LOWER(u.firstname) LIKE LOWER(CONCAT('%', :firstname,   '%'))) AND
            (:lastname    IS NULL OR LOWER(u.lastname)  LIKE LOWER(CONCAT('%', :lastname,    '%'))) AND
            (:email       IS NULL OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :email,       '%'))) AND
            (:phoneNumber IS NULL OR u.phoneNumber      LIKE       CONCAT('%', :phoneNumber, '%'))
            """)
    Page<User> searchByCriteria(
            @Param("username")    String username,
            @Param("firstname")   String firstname,
            @Param("lastname")    String lastname,
            @Param("email")       String email,
            @Param("phoneNumber") String phoneNumber,
            Pageable pageable
    );
}