package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Address} entities.
 *
 * <p>This repository handles operations on the pure geographical address data,
 * independent of any ownership relationships. For user-specific address operations,
 * see {@link UserAddressRepository}.</p>
 *
 * <h4>Key responsibilities:</h4>
 * <ul>
 *   <li>CRUD operations on addresses</li>
 *   <li>Duplicate detection to avoid redundant address entries</li>
 *   <li>Geographical queries (by city, postal code, country)</li>
 *   <li>Specification-based searches for autocomplete functionality</li>
 *   <li>Orphaned address cleanup support</li>
 * </ul>
 *
 * <h4>Specification Support:</h4>
 * <p>Extends {@link JpaSpecificationExecutor} to enable dynamic query building
 * using {@link be.steby.CoreProject.dal.specifications.AddressSpecification}.
 * This is particularly useful for autocomplete/suggestion features.</p>
 *
 * @see Address
 * @see UserAddressRepository
 * @see be.steby.CoreProject.dal.specifications.AddressSpecification
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {

    // ========================================
    // region Public ID Lookup
    // ========================================

    /**
     * Finds an address by its public ID.
     *
     * @param publicId the public UUID of the address
     * @return the address if found
     */
    Optional<Address> findByPublicId(String publicId);

    // endregion

    // ========================================
    // region Duplicate Detection
    // ========================================

    /**
     * Finds addresses matching exact street, postal code, and city.
     *
     * <p>Used to detect potential duplicates before creating a new address.
     * Comparison is case-insensitive for better matching.</p>
     *
     * @param streetNumber the street number
     * @param streetName   the street name
     * @param postalCode   the postal code
     * @param city         the city
     * @param countryCode  the ISO country code
     * @return list of matching addresses
     */
    @Query("""
            SELECT a FROM Address a
            WHERE LOWER(a.streetNumber) = LOWER(:streetNumber)
              AND LOWER(a.streetName) = LOWER(:streetName)
              AND LOWER(a.postalCode) = LOWER(:postalCode)
              AND LOWER(a.city) = LOWER(:city)
              AND LOWER(a.countryCode) = LOWER(:countryCode)
            """)
    List<Address> findDuplicates(
            @Param("streetNumber") String streetNumber,
            @Param("streetName") String streetName,
            @Param("postalCode") String postalCode,
            @Param("city") String city,
            @Param("countryCode") String countryCode
    );

    /**
     * Checks if an exact address already exists.
     *
     * @param streetNumber the street number
     * @param streetName   the street name
     * @param postalCode   the postal code
     * @param city         the city
     * @param countryCode  the ISO country code
     * @return true if a matching address exists
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM Address a
            WHERE LOWER(a.streetNumber) = LOWER(:streetNumber)
              AND LOWER(a.streetName) = LOWER(:streetName)
              AND LOWER(a.postalCode) = LOWER(:postalCode)
              AND LOWER(a.city) = LOWER(:city)
              AND LOWER(a.countryCode) = LOWER(:countryCode)
            """)
    boolean existsDuplicate(
            @Param("streetNumber") String streetNumber,
            @Param("streetName") String streetName,
            @Param("postalCode") String postalCode,
            @Param("city") String city,
            @Param("countryCode") String countryCode
    );

    // endregion

    // ========================================
    // region Geographical Queries
    // ========================================

    /**
     * Finds all addresses in a specific city.
     *
     * @param city        the city name (case-insensitive)
     * @param countryCode the ISO country code
     * @return list of addresses in the specified city
     */
    @Query("""
            SELECT a FROM Address a
            WHERE LOWER(a.city) = LOWER(:city)
              AND LOWER(a.countryCode) = LOWER(:countryCode)
            ORDER BY a.streetName, a.streetNumber
            """)
    List<Address> findByCity(
            @Param("city") String city,
            @Param("countryCode") String countryCode
    );

    /**
     * Finds all addresses with a specific postal code.
     *
     * @param postalCode  the postal code
     * @param countryCode the ISO country code
     * @return list of addresses with the specified postal code
     */
    List<Address> findByPostalCodeAndCountryCodeIgnoreCase(String postalCode, String countryCode);

    /**
     * Finds all addresses in a specific country.
     *
     * @param countryCode the ISO country code
     * @return list of addresses in the specified country
     */
    List<Address> findByCountryCodeIgnoreCase(String countryCode);

    /**
     * Finds all addresses in a specific state/province.
     *
     * @param stateProvince the state or province name
     * @param countryCode   the ISO country code
     * @return list of addresses in the specified state/province
     */
    List<Address> findByStateProvinceAndCountryCodeIgnoreCase(String stateProvince, String countryCode);

    // endregion

    // ========================================
    // region Validation Status
    // ========================================

    /**
     * Finds all validated addresses.
     *
     * @return list of addresses that have been validated
     */
    List<Address> findByValidatedTrue();

    /**
     * Finds all unvalidated addresses.
     *
     * <p>Useful for batch validation jobs.</p>
     *
     * @return list of addresses pending validation
     */
    List<Address> findByValidatedFalse();

    /**
     * Counts unvalidated addresses.
     *
     * @return number of addresses pending validation
     */
    long countByValidatedFalse();

    // endregion

    // ========================================
    // region Orphan Detection
    // ========================================

    /**
     * Finds addresses that are not linked to any entity.
     *
     * <p>Orphaned addresses can occur when all links are deleted but the
     * address itself remains. This query helps identify addresses that
     * may be candidates for cleanup.</p>
     *
     * <p><b>Note:</b> This query checks for UserAddress links. When additional
     * link types are added (OrderAddress, CompanyAddress), this query should
     * be updated to check all link types.</p>
     *
     * @return list of addresses with no active links
     */
    @Query("""
            SELECT a FROM Address a
            WHERE NOT EXISTS (
                SELECT 1 FROM UserAddress ua WHERE ua.address = a
            )
            """)
    List<Address> findOrphanedAddresses();

    /**
     * Counts orphaned addresses.
     *
     * @return number of addresses with no active links
     */
    @Query("""
            SELECT COUNT(a) FROM Address a
            WHERE NOT EXISTS (
                SELECT 1 FROM UserAddress ua WHERE ua.address = a
            )
            """)
    long countOrphanedAddresses();

    // endregion

    // ========================================
    // region Geolocation
    // ========================================

    /**
     * Finds addresses without geolocation coordinates.
     *
     * <p>Useful for batch geocoding jobs.</p>
     *
     * @return list of addresses missing coordinates
     */
    @Query("SELECT a FROM Address a WHERE a.latitude IS NULL OR a.longitude IS NULL")
    List<Address> findWithoutCoordinates();

    /**
     * Finds addresses within a bounding box.
     *
     * <p>Simple rectangular search. For radius-based search,
     * consider using a spatial database extension.</p>
     *
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLon minimum longitude
     * @param maxLon maximum longitude
     * @return list of addresses within the bounding box
     */
    @Query("""
            SELECT a FROM Address a
            WHERE a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLon AND :maxLon
            """)
    List<Address> findWithinBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );

    // endregion
}