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
    // region Suggestions/Autocomplete
    // ========================================

    /**
     * Searches addresses for autocomplete suggestions based on a specific field.
     *
     * <p>This method is optimized for autocomplete/typeahead functionality,
     * returning addresses that match the given query in the specified field.</p>
     *
     * <h4>Supported Fields:</h4>
     * <ul>
     *   <li>{@code streetName} - Searches in street name field</li>
     *   <li>{@code city} - Searches in city field</li>
     *   <li>{@code postalCode} - Searches in postal code field</li>
     * </ul>
     *
     * <h4>Search Strategy:</h4>
     * <p>Uses prefix matching ({@code LIKE 'query%'}) for optimal index usage.
     * Results are ordered alphabetically by the searched field.</p>
     *
     * <h4>Example Usage:</h4>
     * <pre>{@code
     * // User types "rue de la" in street name field
     * List<Address> suggestions = addressService.searchSuggestions("streetName", "rue de la", 10);
     *
     * // User types "1000" in postal code field
     * List<Address> suggestions = addressService.searchSuggestions("postalCode", "1000", 10);
     * }</pre>
     *
     * @param field the field to search in ("streetName", "city", or "postalCode")
     * @param query the search query (minimum 2 characters recommended)
     * @param limit maximum number of results to return (1-20)
     * @return list of matching addresses, ordered alphabetically
     * @throws IllegalArgumentException if field is not supported or query is blank
     */
    List<Address> searchSuggestions(String field, String query, int limit);

    /**
     * Searches addresses for autocomplete suggestions with default limit.
     *
     * <p>Convenience method that uses a default limit of 10 results.</p>
     *
     * @param field the field to search in ("streetName", "city", or "postalCode")
     * @param query the search query (minimum 2 characters recommended)
     * @return list of matching addresses, ordered alphabetically (max 10)
     * @throws IllegalArgumentException if field is not supported or query is blank
     * @see #searchSuggestions(String, String, int)
     */
    default List<Address> searchSuggestions(String field, String query) {
        return searchSuggestions(field, query, 10);
    }

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
    Address markAsValidated(Address address, String validationSource);

    /**
     * Sets geolocation coordinates for an address.
     *
     * @param publicId  the address public ID
     * @param latitude  the latitude
     * @param longitude the longitude
     * @return the updated address
     */
    Address setCoordinates(Address address, Double latitude, Double longitude);

    /**
     * Sets the formatted address string.
     *
     * @param publicId         the address public ID
     * @param formattedAddress the formatted address string
     * @return the updated address
     */
    Address setFormattedAddress(Address address, String formattedAddress);

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