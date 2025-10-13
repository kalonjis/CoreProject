package be.steby.CoreProject.dal.repositories;


import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

}

