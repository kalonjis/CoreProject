package be.steby.CoreProject.bll.domains.profile.services.address;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UpdateUserAddressRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UserAddressSearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link UserAddress} relationships.
 *
 * <p>This service handles the relationship between users and their addresses,
 * including support for address sharing and modification strategies.</p>
 *
 * @see UserAddress
 */
public interface UserAddressService {

    // ========================================
    // region Search
    // ========================================

    /**
     * Searches addresses with flexible filtering.
     *
     * @param user     the user
     * @param criteria the search criteria
     * @return list of matching addresses
     */
    List<UserAddress> search(User user, UserAddressSearchCriteria criteria);

    // endregion

    // ========================================
    // region Lookup
    // ========================================

    /**
     * Gets a user address link by its public ID.
     *
     * @param publicId the public UUID
     * @return the user address
     */
    UserAddress getByPublicId(String publicId);

    /**
     * Gets a user address link ensuring it belongs to the specified user.
     *
     * @param publicId the public UUID
     * @param user     the expected owner
     * @return the user address
     */
    UserAddress getByPublicIdAndUser(String publicId, User user);

    /**
     * Gets all addresses for a user.
     *
     * @param user the user
     * @return list of all user address links
     */
    List<UserAddress> getAllForUser(User user);

    /**
     * Gets all active addresses for a user.
     *
     * @param user the user
     * @return list of active user address links
     */
    List<UserAddress> getActiveForUser(User user);

    /**
     * Gets addresses of a specific type for a user.
     *
     * @param user        the user
     * @param addressType the address type
     * @return list of matching addresses
     */
    List<UserAddress> getByType(User user, AddressType addressType);

    // endregion

    // ========================================
    // region Default & Primary Address
    // ========================================

    /**
     * Gets the user's primary address.
     *
     * @param user the user
     * @return the primary address if set
     */
    Optional<UserAddress> getPrimaryAddress(User user);

    /**
     * Gets the default address for a specific type.
     *
     * @param user        the user
     * @param addressType the address type
     * @return the default address if set
     */
    Optional<UserAddress> getDefaultForType(User user, AddressType addressType);

    /**
     * Gets the user's default billing address.
     *
     * @param user the user
     * @return the default billing address if set
     */
    Optional<UserAddress> getDefaultBillingAddress(User user);

    /**
     * Gets the user's default shipping address.
     *
     * @param user the user
     * @return the default shipping address if set
     */
    Optional<UserAddress> getDefaultShippingAddress(User user);

    /**
     * Sets an address as the default for its type.
     *
     * @param publicId the user address public ID
     * @param user     the user (for ownership verification)
     * @return the updated user address
     */
    UserAddress setAsDefault(String publicId, User user);

    /**
     * Sets an address as the user's primary address.
     *
     * @param publicId the user address public ID
     * @param user     the user (for ownership verification)
     * @return the updated user address
     */
    UserAddress setAsPrimary(String publicId, User user);

    // endregion

    // ========================================
    // region Link Management
    // ========================================

    /**
     * Links an existing address to a user.
     *
     * @param user        the user
     * @param address     the address to link
     * @param addressType the type of address
     * @param label       optional label
     * @param isDefault   whether this should be the default for its type
     * @param isPrimary   whether this should be the primary address
     * @return the created user address link
     */
    UserAddress linkAddress(User user, Address address, AddressType addressType,
                            String label, boolean isDefault, boolean isPrimary);

    /**
     * Links an existing address to a user with minimal parameters.
     *
     * @param user        the user
     * @param address     the address to link
     * @param addressType the type of address
     * @return the created user address link
     */
    UserAddress linkAddress(User user, Address address, AddressType addressType);

    /**
     * Creates a new address and links it to the user in one operation.
     *
     * @param user        the user
     * @param address     the address data
     * @param addressType the type of address
     * @param label       optional label
     * @param isDefault   whether this should be the default for its type
     * @param isPrimary   whether this should be the primary address
     * @return the created user address link
     */
    UserAddress createAndLink(User user, Address address, AddressType addressType,
                              String label, boolean isDefault, boolean isPrimary);

    /**
     * Creates a new address and links it to the user with full configuration.
     *
     * <p>This method consolidates address creation and eligibility settings in a single operation.</p>
     *
     * @param user             the user
     * @param address          the address data
     * @param addressType      the type of address
     * @param label            optional label
     * @param isDefault        whether this should be the default for its type
     * @param isPrimary        whether this should be the primary address
     * @param billingEligible  whether eligible for billing
     * @param shippingEligible whether eligible for shipping
     * @return the created user address link
     */
    UserAddress createAndLink(User user, Address address, AddressType addressType,
                              String label, boolean isDefault, boolean isPrimary,
                              boolean billingEligible, boolean shippingEligible);

    /**
     * Creates a new address and links it with minimal parameters.
     *
     * @param user        the user
     * @param address     the address data
     * @param addressType the type of address
     * @return the created user address link
     */
    UserAddress createAndLink(User user, Address address, AddressType addressType);

    /**
     * Unlinks an address from a user (soft delete).
     *
     * @param publicId the user address public ID
     * @param user     the user (for ownership verification)
     * @return the deactivated user address
     */
    UserAddress unlinkAddress(String publicId, User user);

    /**
     * Permanently removes an address link.
     *
     * @param publicId the user address public ID
     * @param user     the user (for ownership verification)
     */
    void removeAddressLink(String publicId, User user);

    // endregion

    // ========================================
    // region Address Modification (with Strategy)
    // ========================================

    /**
     * Updates an address respecting the modification strategy.
     *
     * @param userAddressPublicId the user address link public ID
     * @param user                the user requesting the update
     * @param updatedAddress      the new address data
     * @return the updated user address link
     */
    UserAddress updateAddress(String userAddressPublicId, User user, Address updatedAddress);

    // endregion

    // ========================================
    // region Link Metadata Updates
    // ========================================

    /**
     * Updates the metadata of a user address link.
     *
     * @param publicId the user address public ID
     * @param user     the user
     * @param label    new label (null to keep existing)
     * @param notes    new notes (null to keep existing)
     * @return the updated user address
     */
    UserAddress updateLinkMetadata(String publicId, User user, String label, String notes);

    /**
     * Consolidated update operation for user addresses.
     *
     * <p>This method handles all update operations in a single transaction:</p>
     * <ul>
     *   <li>Address geographical data (if provided)</li>
     *   <li>Link metadata (label, notes)</li>
     *   <li>Eligibility settings (billing, shipping)</li>
     * </ul>
     *
     * @param publicId the user address public ID
     * @param user     the user
     * @param request  the update request with optional fields
     * @return the updated user address
     */
    UserAddress updateUserAddress(String publicId, User user, UpdateUserAddressRequest request);

    /**
     * Updates eligibility flags.
     *
     * @param publicId         the user address public ID
     * @param user             the user
     * @param billingEligible  whether eligible for billing
     * @param shippingEligible whether eligible for shipping
     * @return the updated user address
     */
    UserAddress updateEligibility(String publicId, User user,
                                  boolean billingEligible, boolean shippingEligible);

    /**
     * Changes the type of an address link.
     *
     * @param publicId     the user address public ID
     * @param user         the user
     * @param newType      the new address type
     * @param setAsDefault whether to set as default for the new type
     * @return the updated user address
     */
    UserAddress changeType(String publicId, User user, AddressType newType, boolean setAsDefault);

    // endregion

    // ========================================
    // region Sharing Info
    // ========================================

    /**
     * Gets all users sharing a specific address.
     *
     * @param addressPublicId the address public ID
     * @return list of user address links to this address
     */
    List<UserAddress> getUsersSharingAddress(String addressPublicId);

    /**
     * Checks if an address is shared between multiple users.
     *
     * @param addressPublicId the address public ID
     * @return true if shared
     */
    boolean isAddressShared(String addressPublicId);

    /**
     * Counts how many users share an address.
     *
     * @param addressPublicId the address public ID
     * @return number of users
     */
    long countUsersForAddress(String addressPublicId);

    // endregion

    // ========================================
    // region Eligibility Queries
    // ========================================

    /**
     * Gets all billing-eligible addresses for a user.
     *
     * @param user the user
     * @return list of billing-eligible addresses
     */
    List<UserAddress> getBillingEligibleAddresses(User user);

    /**
     * Gets all shipping-eligible addresses for a user.
     *
     * @param user the user
     * @return list of shipping-eligible addresses
     */
    List<UserAddress> getShippingEligibleAddresses(User user);

    // endregion
}