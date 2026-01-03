package be.steby.CoreProject.bll.domains.address.services;


import be.steby.CoreProject.dl.entities.Address;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link Address} entities.
 *
 * <p>This service handles operations on pure geographical address data,
 * independent of ownership relationships.</p>
 *
 * @see Address
 */
public interface AddressService {

    // ========================================
    // region Lookup
    // ========================================

    /**
     * Finds an address by its internal ID.
     *
     * @param id the address ID
     * @return the address
     * @throws be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException if not found
     */
    Address getById(Long id);

    /**
     * Finds an address by its public ID.
     *
     * @param publicId the public UUID
     * @return the address
     * @throws be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException if not found
     */
    Address getByPublicId(String publicId);

    /**
     * Finds an address by its public ID.
     *
     * @param publicId the public UUID
     * @return optional containing the address if found
     */
    Optional<Address> findByPublicId(String publicId);

    // endregion

    // ========================================
    // region Creation
    // ========================================

    /**
     * Creates a new address.
     *
     * @param address          the address to create
     * @param detectDuplicates whether to check for existing duplicates
     * @return the created or existing address
     */
    Address create(Address address, boolean detectDuplicates);

    /**
     * Creates a new address with duplicate detection enabled.
     *
     * @param address the address to create
     * @return the created or existing address
     */
    Address create(Address address);

    /**
     * Creates a new address, always creating a new record (no duplicate detection).
     *
     * @param address the address to create
     * @return the newly created address
     */
    Address createForced(Address address);

    // endregion

    // ========================================
    // region Update
    // ========================================

    /**
     * Updates an existing address.
     *
     * @param publicId the public ID of the address to update
     * @param updated  the updated address data
     * @return the updated address
     * @throws be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException if not found
     */
    Address update(String publicId, Address updated);

    // endregion

    // ========================================
    // region Duplicate Detection
    // ========================================

    /**
     * Finds a duplicate of the given address.
     *
     * @param address the address to check
     * @return the duplicate if found
     */
    Optional<Address> findDuplicate(Address address);

    /**
     * Checks if a duplicate exists for the given address.
     *
     * @param address the address to check
     * @return true if a duplicate exists
     */
    boolean hasDuplicate(Address address);

    /**
     * Finds or creates an address.
     *
     * @param address the address data
     * @return the existing or newly created address
     */
    Address findOrCreate(Address address);

    // endregion

    // ========================================
    // region Geographical Queries
    // ========================================

    /**
     * Finds all addresses in a specific city.
     *
     * @param city        the city name
     * @param countryCode the ISO country code
     * @return list of addresses
     */
    List<Address> findByCity(String city, String countryCode);

    /**
     * Finds all addresses with a specific postal code.
     *
     * @param postalCode  the postal code
     * @param countryCode the ISO country code
     * @return list of addresses
     */
    List<Address> findByPostalCode(String postalCode, String countryCode);

    /**
     * Finds all addresses in a specific country.
     *
     * @param countryCode the ISO country code
     * @return list of addresses
     */
    List<Address> findByCountry(String countryCode);

    // endregion

    // ========================================
    // region Validation
    // ========================================

    /**
     * Marks an address as validated.
     *
     * @param publicId         the address public ID
     * @param validationSource the source of validation
     * @return the updated address
     */
    Address markAsValidated(String publicId, String validationSource);

    /**
     * Sets geolocation coordinates for an address.
     *
     * @param publicId  the address public ID
     * @param latitude  the latitude
     * @param longitude the longitude
     * @return the updated address
     */
    Address setCoordinates(String publicId, Double latitude, Double longitude);

    /**
     * Sets the formatted address string.
     *
     * @param publicId         the address public ID
     * @param formattedAddress the formatted address string
     * @return the updated address
     */
    Address setFormattedAddress(String publicId, String formattedAddress);

    /**
     * Gets all addresses pending validation.
     *
     * @return list of unvalidated addresses
     */
    List<Address> getUnvalidatedAddresses();

    /**
     * Gets all addresses missing geolocation.
     *
     * @return list of addresses without coordinates
     */
    List<Address> getAddressesWithoutCoordinates();

    // endregion

    // ========================================
    // region Cleanup
    // ========================================

    /**
     * Finds all orphaned addresses (not linked to any entity).
     *
     * @return list of orphaned addresses
     */
    List<Address> findOrphanedAddresses();

    /**
     * Counts orphaned addresses.
     *
     * @return number of orphaned addresses
     */
    long countOrphanedAddresses();

    /**
     * Deletes all orphaned addresses.
     *
     * @return number of deleted addresses
     */
    int deleteOrphanedAddresses();

    /**
     * Deletes an address by public ID.
     *
     * @param publicId the address public ID
     */
    void delete(String publicId);

    // endregion
}