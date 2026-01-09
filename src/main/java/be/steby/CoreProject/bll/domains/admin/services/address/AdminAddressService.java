package be.steby.CoreProject.bll.domains.admin.services.address;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.UserAddress;

import java.util.List;

/**
 * Service interface for administrative address management operations.
 * Handles global address viewing and management with proper permission validation.
 *
 * <p>This service provides a system-wide view of addresses, allowing admins to:</p>
 * <ul>
 *   <li>View all addresses in the system</li>
 *   <li>See which users are linked to specific addresses</li>
 *   <li>Understand address sharing patterns</li>
 *   <li>Identify orphaned addresses</li>
 * </ul>
 *
 * <h4>Permission requirements:</h4>
 * <p>All methods require the authenticated user to have ADMIN or SUPER_ADMIN role.
 * Permission validation is performed within each method implementation.</p>
 *
 * <h4>Delegation strategy:</h4>
 * <ul>
 *   <li>AddressService → Core address operations</li>
 *   <li>UserAddressRepository → Link queries</li>
 *   <li>AdminPermissionValidator → Permission validation</li>
 * </ul>
 *
 * @see be.steby.CoreProject.bll.domains.address.services.AddressService
 * @see be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator
 */
public interface AdminAddressService {

    /**
     * Retrieves all addresses in the system.
     *
     * <p>Returns all Address entities regardless of whether they are linked to users.
     * Useful for getting a complete overview of the address data.</p>
     *
     * <h4>Note:</h4>
     * <p>This method returns all addresses without pagination. For large systems,
     * consider adding pagination support in future versions.</p>
     *
     * @return List of all addresses in the system
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     */
    List<Address> getAllAddresses();

    /**
     * Retrieves a specific address by its public ID.
     *
     * <p>Returns the Address entity with geographical data only.
     * To see linked users, use {@link #getAddressUsers(String)}.</p>
     *
     * @param publicId Address public ID
     * @return The address entity
     * @throws be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException
     *         if address doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     */
    Address getAddressById(String publicId);

    /**
     * Retrieves all users linked to a specific address.
     *
     * <p>Returns all UserAddress links for the given address, including inactive ones.
     * This allows admins to see the complete history and current usage of an address.</p>
     *
     * <h4>Use cases:</h4>
     * <ul>
     *   <li>Understanding address sharing (roommates, family members)</li>
     *   <li>Impact analysis before address modification</li>
     *   <li>Identifying orphaned addresses (empty list)</li>
     * </ul>
     *
     * @param addressPublicId Address public ID
     * @return List of UserAddress links (empty if no users linked)
     * @throws be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException
     *         if address doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     */
    List<UserAddress> getAddressUsers(String addressPublicId);
}