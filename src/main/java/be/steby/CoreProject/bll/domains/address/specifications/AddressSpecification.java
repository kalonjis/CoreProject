package be.steby.CoreProject.bll.domains.address.specifications;

import be.steby.CoreProject.dl.entities.Address;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for filtering {@link Address} entities.
 *
 * <p>Provides composable filter methods that can be combined using
 * {@code and()}, {@code or()}, and {@code not()} operations.</p>
 *
 * <h4>Primary Use Cases:</h4>
 * <ul>
 *   <li><b>Autocomplete/Suggestions:</b> Use {@code startsWith} methods for
 *       index-friendly prefix searches during user input</li>
 *   <li><b>Extended Search:</b> Use {@code contains} methods when prefix
 *       search yields insufficient results</li>
 *   <li><b>Filtering:</b> Use exact match methods for precise filtering</li>
 * </ul>
 *
 * <h4>Performance Notes:</h4>
 * <ul>
 *   <li>{@code startsWith} methods use {@code LIKE 'prefix%'} which can
 *       leverage B-tree indexes efficiently</li>
 *   <li>{@code contains} methods use {@code LIKE '%keyword%'} which requires
 *       full table scan unless trigram indexes are configured</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Simple autocomplete on street name
 * Specification<Address> spec = AddressSpecification.streetNameStartsWith("rue de la");
 * List<Address> results = addressRepository.findAll(spec, PageRequest.of(0, 10));
 *
 * // Combined search with country filter
 * Specification<Address> spec = AddressSpecification.streetNameStartsWith("main")
 *     .and(AddressSpecification.hasCountry("US"));
 * }</pre>
 *
 * @see Address
 * @see be.steby.CoreProject.dal.repositories.AddressRepository
 */
public class AddressSpecification {

    private AddressSpecification() {
        // Utility class - prevent instantiation
    }

    // ========================================
    // region Starts With (Autocomplete-optimized)
    // ========================================

    /**
     * Filters addresses where street name starts with the given prefix.
     *
     * <p>Case-insensitive search using {@code LIKE 'prefix%'}.
     * This pattern is optimized for B-tree index usage.</p>
     *
     * <p><b>Use case:</b> Autocomplete when user types in street name field.</p>
     *
     * @param prefix the prefix to search for (case-insensitive)
     * @return specification filtering by street name prefix
     */
    public static Specification<Address> streetNameStartsWith(String prefix) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("streetName")),
                prefix.toLowerCase() + "%"
        );
    }

    /**
     * Filters addresses where city starts with the given prefix.
     *
     * <p>Case-insensitive search using {@code LIKE 'prefix%'}.
     * This pattern is optimized for B-tree index usage.</p>
     *
     * <p><b>Use case:</b> Autocomplete when user types in city field.</p>
     *
     * @param prefix the prefix to search for (case-insensitive)
     * @return specification filtering by city prefix
     */
    public static Specification<Address> cityStartsWith(String prefix) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("city")),
                prefix.toLowerCase() + "%"
        );
    }

    /**
     * Filters addresses where postal code starts with the given prefix.
     *
     * <p>Case-insensitive search using {@code LIKE 'prefix%'}.
     * This pattern is optimized for B-tree index usage.</p>
     *
     * <p><b>Use case:</b> Autocomplete when user types in postal code field.</p>
     *
     * @param prefix the prefix to search for (case-insensitive)
     * @return specification filtering by postal code prefix
     */
    public static Specification<Address> postalCodeStartsWith(String prefix) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("postalCode")),
                prefix.toLowerCase() + "%"
        );
    }

    // endregion

    // ========================================
    // region Contains (Extended Search)
    // ========================================

    /**
     * Filters addresses where street name contains the given keyword.
     *
     * <p>Case-insensitive search using {@code LIKE '%keyword%'}.
     * Note: This pattern cannot use standard B-tree indexes efficiently.</p>
     *
     * <p><b>Use case:</b> Extended search when prefix search yields no results.</p>
     *
     * @param keyword the keyword to search for (case-insensitive)
     * @return specification filtering by street name containing keyword
     */
    public static Specification<Address> streetNameContains(String keyword) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("streetName")),
                "%" + keyword.toLowerCase() + "%"
        );
    }

    /**
     * Filters addresses where city contains the given keyword.
     *
     * <p>Case-insensitive search using {@code LIKE '%keyword%'}.
     * Note: This pattern cannot use standard B-tree indexes efficiently.</p>
     *
     * <p><b>Use case:</b> Extended search when prefix search yields no results.</p>
     *
     * @param keyword the keyword to search for (case-insensitive)
     * @return specification filtering by city containing keyword
     */
    public static Specification<Address> cityContains(String keyword) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("city")),
                "%" + keyword.toLowerCase() + "%"
        );
    }

    /**
     * Filters addresses where formatted address contains the given keyword.
     *
     * <p>Case-insensitive search using {@code LIKE '%keyword%'}.
     * Searches in the pre-formatted address string if available.</p>
     *
     * <p><b>Use case:</b> Full-text style search across the complete address.</p>
     *
     * @param keyword the keyword to search for (case-insensitive)
     * @return specification filtering by formatted address containing keyword
     */
    public static Specification<Address> formattedAddressContains(String keyword) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("formattedAddress")),
                "%" + keyword.toLowerCase() + "%"
        );
    }

    // endregion

    // ========================================
    // region Exact Match (Filtering)
    // ========================================

    /**
     * Filters addresses by exact city match.
     *
     * <p>Case-insensitive exact match.</p>
     *
     * @param city the city name to match
     * @return specification filtering by exact city
     */
    public static Specification<Address> hasCity(String city) {
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("city")),
                city.toLowerCase()
        );
    }

    /**
     * Filters addresses by exact postal code match.
     *
     * <p>Case-insensitive exact match.</p>
     *
     * @param postalCode the postal code to match
     * @return specification filtering by exact postal code
     */
    public static Specification<Address> hasPostalCode(String postalCode) {
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("postalCode")),
                postalCode.toLowerCase()
        );
    }

    /**
     * Filters addresses by exact country code match.
     *
     * <p>Case-insensitive exact match using ISO 3166-1 alpha-2 codes.</p>
     *
     * @param countryCode the ISO country code to match (e.g., "BE", "FR", "US")
     * @return specification filtering by exact country code
     */
    public static Specification<Address> hasCountry(String countryCode) {
        return (root, query, cb) -> cb.equal(
                cb.upper(root.get("countryCode")),
                countryCode.toUpperCase()
        );
    }

    /**
     * Filters addresses by exact street name match.
     *
     * <p>Case-insensitive exact match.</p>
     *
     * @param streetName the street name to match
     * @return specification filtering by exact street name
     */
    public static Specification<Address> hasStreetName(String streetName) {
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("streetName")),
                streetName.toLowerCase()
        );
    }

    // endregion

    // ========================================
    // region Validation & Geolocation Filters
    // ========================================

    /**
     * Filters only validated addresses.
     *
     * @return specification filtering validated addresses
     */
    public static Specification<Address> isValidated() {
        return (root, query, cb) -> cb.isTrue(root.get("validated"));
    }

    /**
     * Filters only non-validated addresses.
     *
     * @return specification filtering non-validated addresses
     */
    public static Specification<Address> isNotValidated() {
        return (root, query, cb) -> cb.isFalse(root.get("validated"));
    }

    /**
     * Filters addresses that have geolocation coordinates.
     *
     * @return specification filtering addresses with coordinates
     */
    public static Specification<Address> hasCoordinates() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("latitude")),
                cb.isNotNull(root.get("longitude"))
        );
    }

    /**
     * Filters addresses that do not have geolocation coordinates.
     *
     * @return specification filtering addresses without coordinates
     */
    public static Specification<Address> hasNoCoordinates() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("latitude")),
                cb.isNull(root.get("longitude"))
        );
    }

    // endregion

    // ========================================
    // region Null Safety Helpers
    // ========================================

    /**
     * Filters addresses where street name is not null or empty.
     *
     * @return specification filtering addresses with street name
     */
    public static Specification<Address> hasStreetNameNotEmpty() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("streetName")),
                cb.notEqual(cb.trim(root.get("streetName")), "")
        );
    }

    /**
     * Filters addresses where city is not null or empty.
     *
     * @return specification filtering addresses with city
     */
    public static Specification<Address> hasCityNotEmpty() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("city")),
                cb.notEqual(cb.trim(root.get("city")), "")
        );
    }

    // endregion
}