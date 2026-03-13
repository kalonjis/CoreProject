package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link UserAddress} entities.
 *
 * <p>This repository handles the relationship between users and their addresses,
 * including metadata like address type, default status, and validity periods.</p>
 *
 * <h4>Key responsibilities:</h4>
 * <ul>
 *   <li>Retrieve all addresses for a user</li>
 *   <li>Find default/primary addresses</li>
 *   <li>Manage address sharing between users</li>
 *   <li>Handle address type filtering</li>
 * </ul>
 *
 * @see UserAddress
 * @see AddressRepository
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long>, JpaSpecificationExecutor<UserAddress> {

    // ========================================
    // region Public ID Lookup
    // ========================================

    /**
     * Finds a user address link by its public ID.
     *
     * @param publicId the public UUID of the link
     * @return the user address if found
     */
    Optional<UserAddress> findByPublicId(String publicId);

    /**
     * Finds a user address link by public ID and user.
     *
     * <p>Ensures the link belongs to the specified user (security check).</p>
     *
     * @param publicId the public UUID of the link
     * @param user     the owner user
     * @return the user address if found and owned by user
     */
    Optional<UserAddress> findByPublicIdAndUser(String publicId, User user);

    // endregion

    // ========================================
    // region User Address Queries
    // ========================================

    /**
     * Finds all address links for a user.
     *
     * @param user the user
     * @return list of all user's address links
     */
    List<UserAddress> findByUser(User user);

    /**
     * Finds all address links for a user by user ID.
     *
     * @param userId the user's ID
     * @return list of all user's address links
     */
    List<UserAddress> findByUserId(Long userId);

    /**
     * Finds all active address links for a user.
     *
     * <p>Excludes deactivated and expired addresses.</p>
     *
     * @param user the user
     * @return list of active address links
     */
    @Query("""
            SELECT ua FROM UserAddress ua
            WHERE ua.user = :user
              AND ua.active = true
              AND (ua.validFrom IS NULL OR ua.validFrom <= CURRENT_TIMESTAMP)
              AND (ua.validTo IS NULL OR ua.validTo > CURRENT_TIMESTAMP)
            ORDER BY ua.isPrimary DESC, ua.isDefault DESC, ua.createdAt DESC
            """)
    List<UserAddress> findActiveByUser(@Param("user") User user);

    /**
     * Finds all address links of a specific type for a user.
     *
     * @param user        the user
     * @param addressType the type of address
     * @return list of matching address links
     */
    List<UserAddress> findByUserAndAddressType(User user, AddressType addressType);

    /**
     * Finds active addresses of a specific type for a user.
     *
     * @param user        the user
     * @param addressType the type of address
     * @return list of active matching address links
     */
    @Query("""
            SELECT ua FROM UserAddress ua
            WHERE ua.user = :user
              AND ua.addressType = :addressType
              AND ua.active = true
              AND (ua.validFrom IS NULL OR ua.validFrom <= CURRENT_TIMESTAMP)
              AND (ua.validTo IS NULL OR ua.validTo > CURRENT_TIMESTAMP)
            ORDER BY ua.isDefault DESC, ua.createdAt DESC
            """)
    List<UserAddress> findActiveByUserAndType(
            @Param("user") User user,
            @Param("addressType") AddressType addressType
    );

    // endregion

    // ========================================
    // region Default & Primary Address
    // ========================================

    /**
     * Finds the user's primary address.
     *
     * @param user the user
     * @return the primary address if set
     */
    Optional<UserAddress> findByUserAndIsPrimaryTrue(User user);

    /**
     * Finds the default address for a specific type.
     *
     * @param user        the user
     * @param addressType the type of address
     * @return the default address for the type if set
     */
    Optional<UserAddress> findByUserAndAddressTypeAndIsDefaultTrue(User user, AddressType addressType);

    /**
     * Finds the user's default billing address.
     *
     * <p>Convenience method for common use case.</p>
     *
     * @param user the user
     * @return the default billing address if set
     */
    default Optional<UserAddress> findDefaultBillingAddress(User user) {
        return findByUserAndAddressTypeAndIsDefaultTrue(user, AddressType.BILLING);
    }

    /**
     * Finds the user's default shipping address.
     *
     * <p>Convenience method for common use case.</p>
     *
     * @param user the user
     * @return the default shipping address if set
     */
    default Optional<UserAddress> findDefaultShippingAddress(User user) {
        return findByUserAndAddressTypeAndIsDefaultTrue(user, AddressType.SHIPPING);
    }

    /**
     * Clears the default flag for all addresses of a type for a user.
     *
     * <p>Used before setting a new default to ensure only one default exists.</p>
     *
     * @param user        the user
     * @param addressType the type of address
     */
    @Modifying
    @Query("""
            UPDATE UserAddress ua
            SET ua.isDefault = false
            WHERE ua.user = :user
              AND ua.addressType = :addressType
              AND ua.isDefault = true
            """)
    void clearDefaultForType(@Param("user") User user, @Param("addressType") AddressType addressType);

    /**
     * Clears the primary flag for all addresses of a user.
     *
     * <p>Used before setting a new primary to ensure only one primary exists.</p>
     *
     * @param user the user
     */
    @Modifying
    @Query("""
            UPDATE UserAddress ua
            SET ua.isPrimary = false
            WHERE ua.user = :user
              AND ua.isPrimary = true
            """)
    void clearPrimaryForUser(@Param("user") User user);

    // endregion

    // ========================================
    // region Eligibility Queries
    // ========================================

    /**
     * Finds all billing-eligible addresses for a user.
     *
     * @param user the user
     * @return list of addresses that can be used for billing
     */
    @Query("""
            SELECT ua FROM UserAddress ua
            WHERE ua.user = :user
              AND ua.billingEligible = true
              AND ua.active = true
              AND (ua.validFrom IS NULL OR ua.validFrom <= CURRENT_TIMESTAMP)
              AND (ua.validTo IS NULL OR ua.validTo > CURRENT_TIMESTAMP)
            ORDER BY ua.isDefault DESC, ua.createdAt DESC
            """)
    List<UserAddress> findBillingEligible(@Param("user") User user);

    /**
     * Finds all shipping-eligible addresses for a user.
     *
     * @param user the user
     * @return list of addresses that can be used for shipping
     */
    @Query("""
            SELECT ua FROM UserAddress ua
            WHERE ua.user = :user
              AND ua.shippingEligible = true
              AND ua.active = true
              AND (ua.validFrom IS NULL OR ua.validFrom <= CURRENT_TIMESTAMP)
              AND (ua.validTo IS NULL OR ua.validTo > CURRENT_TIMESTAMP)
            ORDER BY ua.isDefault DESC, ua.createdAt DESC
            """)
    List<UserAddress> findShippingEligible(@Param("user") User user);

    // endregion

    // ========================================
    // region Address Sharing
    // ========================================

    /**
     * Finds all users who share a specific address.
     *
     * @param address the shared address
     * @return list of user address links to this address
     */
    List<UserAddress> findByAddress(Address address);

    /**
     * Finds all users who share a specific address by address ID.
     *
     * @param addressId the address ID
     * @return list of user address links
     */
    List<UserAddress> findByAddressId(Long addressId);

    /**
     * Counts how many users share a specific address.
     *
     * <p>Useful for determining modification strategy behavior.</p>
     *
     * @param address the address
     * @return number of users linked to this address
     */
    long countByAddress(Address address);

    /**
     * Checks if an address is shared (linked to multiple users).
     *
     * @param address the address
     * @return true if more than one user is linked
     */
    default boolean isAddressShared(Address address) {
        return countByAddress(address) > 1;
    }

    /**
     * Checks if a user already has a link to a specific address.
     *
     * @param user    the user
     * @param address the address
     * @return true if a link exists
     */
    boolean existsByUserAndAddress(User user, Address address);

    /**
     * Checks if a user has a link to an address with a specific type.
     *
     * @param user        the user
     * @param address     the address
     * @param addressType the address type
     * @return true if such a link exists
     */
    boolean existsByUserAndAddressAndAddressType(User user, Address address, AddressType addressType);

    // endregion

    // ========================================
    // region Counting
    // ========================================

    /**
     * Counts all addresses for a user.
     *
     * @param user the user
     * @return total number of address links
     */
    long countByUser(User user);

    /**
     * Counts active addresses for a user.
     *
     * @param user the user
     * @return number of active address links
     */
    long countByUserAndActiveTrue(User user);

    /**
     * Counts addresses by type for a user.
     *
     * @param user        the user
     * @param addressType the address type
     * @return number of addresses of that type
     */
    long countByUserAndAddressType(User user, AddressType addressType);

    // endregion

    // ========================================
    // region Cleanup
    // ========================================

    /**
     * Deletes all address links for a user.
     *
     * <p><b>Warning:</b> This does not delete the underlying addresses,
     * only the links. Use with caution.</p>
     *
     * @param user the user
     */
    void deleteByUser(User user);

    /**
     * Deactivates all address links for a user.
     *
     * <p>Soft-delete alternative that preserves history.</p>
     *
     * @param user the user
     */
    @Modifying
    @Query("""
            UPDATE UserAddress ua
            SET ua.active = false, ua.validTo = :now
            WHERE ua.user = :user
              AND ua.active = true
            """)
    void deactivateAllForUser(@Param("user") User user, @Param("now") Instant now);

    // endregion
}