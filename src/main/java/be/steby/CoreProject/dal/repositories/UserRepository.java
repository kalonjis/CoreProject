package be.steby.CoreProject.dal.repositories;


import be.steby.CoreProject.dl.entities.User;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository interface for managing User entities.
 * Extends JpaRepository to inherit basic CRUD operations.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Retrieves a User by their username, ignoring case.
     *
     * @param username The username of the User.
     * @return An optional containing the User if found, or empty otherwise.
     */
    Optional<User> findByUsernameIgnoreCase(String username);


    /**
     * Checks if a username exists, ignoring case.
     *
     * @param username The username to check.
     * @return true if the username exists, false otherwise.
     */
    boolean existsByUsernameIgnoreCase(String username);


    /**
     * Retrieves a User by their email address, ignoring case.
     *
     * @param email The email address of the User.
     * @return An optional containing the User if found, or empty otherwise.
     */
    Optional<User> findByEmailIgnoreCase(String email);


    /**
     * check if username exist
     * @param email of User
     */
    boolean existsByEmailIgnoreCase(String email);


    /**
     * Finds user by email or username.
     * Useful for login/reactivation where user can provide either.
     *
     * @param identifier Email or username
     * @return Optional containing user if found
     */
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(String identifier);


    /**
     * Finds a User by its public_id.
     * @param publicId the public_id from URL
     * @return Optional containing the user if found
     */
    @Query("SELECT u FROM User u WHERE u.publicId = :publicId")
    Optional<User> findByPublicId(String publicId);

    /**
     * Counts users by enabled status.
     *
     * @param enabled Enabled status (true for active, false for deactivated)
     * @return Number of users with the specified status
     */
    Long countByEnabled(boolean enabled);

    /**
     * Counts users by email verification status.
     *
     * @param emailVerified Email verification status
     * @return Number of users with the specified verification status
     */
    Long countByEmailVerified(boolean emailVerified);

    /**
     * Counts users who have administrative roles (ADMIN or SUPER_ADMIN).
     * Uses a join query to check the user_roles table.
     *
     * @return Number of users with admin roles
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.userRoles r " +
            "WHERE r IN ('ADMIN', 'SUPER_ADMIN')")
    Long countUsersWithAdminRoles();

    /**
     * Global search across multiple user fields.
     * Searches in username, firstname, lastname, email, and phone number.
     *
     * @param query Search query
     * @param pageable Pagination settings
     * @return Paginated list of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.firstname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "u.phoneNumber LIKE CONCAT('%', :query, '%')")
    Page<User> searchByMultipleFields(@Param("query") String query, Pageable pageable);

    /**
     * Searches users by specific criteria with individual field filters.
     * All parameters are optional (null means don't filter by that field).
     *
     * @param username Username filter (partial match)
     * @param firstname First name filter (partial match)
     * @param lastname Last name filter (partial match)
     * @param email Email filter (partial match)
     * @param phoneNumber Phone number filter (partial match)
     * @param pageable Pagination settings
     * @return Paginated list of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "(:firstname IS NULL OR LOWER(u.firstname) LIKE LOWER(CONCAT('%', :firstname, '%'))) AND " +
            "(:lastname IS NULL OR LOWER(u.lastname) LIKE LOWER(CONCAT('%', :lastname, '%'))) AND " +
            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:phoneNumber IS NULL OR u.phoneNumber LIKE CONCAT('%', :phoneNumber, '%'))")
    Page<User> searchByCriteria(
            @Param("username") String username,
            @Param("firstname") String firstname,
            @Param("lastname") String lastname,
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber,
            Pageable pageable);
}



