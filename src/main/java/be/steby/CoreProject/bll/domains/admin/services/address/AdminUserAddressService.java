package be.steby.CoreProject.bll.domains.admin.services.address;

import be.steby.CoreProject.dl.entities.UserAddress;

import java.util.List;

/**
 * Service interface for administrative user address management operations.
 * Handles address-related administrative tasks with proper permission validation.
 *
 * <p>All methods require the authenticated user to have ADMIN privileges.
 * Permission validation is performed within each method implementation.</p>
 *
 * <h4>Delegation strategy:</h4>
 * <ul>
 *   <li>UserService → User lookup and authentication</li>
 *   <li>UserAddressService → Core address operations</li>
 *   <li>AdminPermissionValidator → Permission validation</li>
 * </ul>
 *
 * @see be.steby.CoreProject.bll.domains.profile.services.address.UserAddressService
 * @see be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator
 */
public interface AdminUserAddressService {

    /**
     * Retrieves all addresses associated with a specific user.
     *
     * <p>Returns all user addresses including inactive ones.
     * Admins need to see the complete address history for audit purposes.</p>
     *
     * <h4>Permission requirements:</h4>
     * <ul>
     *   <li>Actor must have ADMIN or SUPER_ADMIN role</li>
     *   <li>No hierarchy restrictions for read operations</li>
     * </ul>
     *
     * @param userPublicId User's public ID
     * @return List of all addresses belonging to the user (empty list if user has no addresses)
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException
     *         if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     */
    List<UserAddress> getUserAddresses(String userPublicId);

    /**
     * Updates the metadata of a user address link as an administrator.
     *
     * <p>This method allows admins to modify link-specific metadata without touching
     * the geographical address data. All fields in the request are optional;
     * only provided fields will be updated.</p>
     *
     * <h4>Updateable fields:</h4>
     * <ul>
     *   <li>label - User-friendly name for the address</li>
     *   <li>notes - Additional information</li>
     *   <li>addressType - RESIDENTIAL, BILLING, SHIPPING, WORK</li>
     *   <li>isPrimary - User's primary address (only one per user)</li>
     *   <li>isDefault - Default for its type (only one per type per user)</li>
     *   <li>billingEligible - Can be used for billing</li>
     *   <li>shippingEligible - Can be used for shipping</li>
     *   <li>verifiedByOwner - Verification status</li>
     * </ul>
     *
     * <h4>Business rules applied:</h4>
     * <ul>
     *   <li>Setting isPrimary=true removes isPrimary from other addresses</li>
     *   <li>Setting isDefault=true removes isDefault from other addresses of same type</li>
     *   <li>Empty strings for label/notes will clear the field</li>
     * </ul>
     *
     * <h4>Permission requirements:</h4>
     * <ul>
     *   <li>Actor must have ADMIN or SUPER_ADMIN role</li>
     *   <li>No hierarchy restrictions for metadata updates</li>
     * </ul>
     *
     * @param userPublicId User's public ID
     * @param linkPublicId UserAddress link public ID
     * @param request      Update request with optional fields
     * @return The updated user address link
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException
     *         if user doesn't exist
     * @throws be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException
     *         if address link doesn't exist or doesn't belong to user
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     * @throws IllegalArgumentException
     *         if request has no fields to update
     */
    UserAddress updateAddressMetadata(
            String userPublicId,
            String linkPublicId,
            be.steby.CoreProject.pl.domains.admin.models.requests.UpdateUserAddressMetadataRequest request
    );


    /**
     * Creates a new address for a specific user as an administrator.
     *
     * <p>Creates both the Address entity (geographical data) and the UserAddress link
     * (metadata) in a single operation. Uses duplicate detection to avoid creating
     * redundant address entries.</p>
     *
     * <h4>Business rules applied:</h4>
     * <ul>
     *   <li>Duplicate detection: reuses existing Address if found</li>
     *   <li>Setting isPrimary=true removes isPrimary from other addresses</li>
     *   <li>Setting isDefault=true removes isDefault from other addresses of same type</li>
     * </ul>
     *
     * <h4>Permission requirements:</h4>
     * <ul>
     *   <li>Actor must have ADMIN or SUPER_ADMIN role</li>
     *   <li>No hierarchy restrictions for address creation</li>
     * </ul>
     *
     * @param userPublicId User's public ID
     * @param request      Address creation request with geographical data and metadata
     * @return The created user address link
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException
     *         if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     * @throws be.steby.CoreProject.bll.domains.address.exceptions.AddressValidationException
     *         if address data is invalid
     */
    UserAddress createAddress(String userPublicId, be.steby.CoreProject.pl.domains.profile.address.models.requests.CreateUserAddressRequest request);
}