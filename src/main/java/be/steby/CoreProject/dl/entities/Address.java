package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a physical postal address.
 *
 * <p>This entity contains only the geographical/postal data of an address,
 * without any ownership or relationship information. It is designed to be
 * reusable and shareable across multiple entities through {@link AddressLink}
 * subclasses.</p>
 *
 * <h4>Design principles:</h4>
 * <ul>
 *   <li><b>Pure data:</b> No business logic or ownership concept</li>
 *   <li><b>Shareable:</b> Multiple users/entities can reference the same address</li>
 *   <li><b>Normalized:</b> Avoids data duplication across the system</li>
 *   <li><b>Extensible:</b> Can be enhanced with geocoding, validation status, etc.</li>
 * </ul>
 *
 * <h4>Relationships:</h4>
 * <p>This entity does not directly reference its "owners". Instead, ownership is
 * managed through {@link AddressLink} subclasses (e.g., {@link UserAddress},
 * CompanyAddress) which hold the many-to-many relationships with metadata.</p>
 *
 * <h4>Address format:</h4>
 * <p>The structure supports international addresses with varying formats.
 * Not all fields are required, allowing flexibility for different countries'
 * addressing conventions.</p>
 *
 * @see AddressLink
 * @see UserAddress
 */
@Entity
@Table(name = "address",
        indexes = {
                @Index(name = "idx_address_postal_code", columnList = "postal_code"),
                @Index(name = "idx_address_city", columnList = "city"),
                @Index(name = "idx_address_country", columnList = "country_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Address extends BaseEntity<Long> {

    // ========================================
    // region Street Information
    // ========================================

    /**
     * Street name.
     *
     * <p>Contains only the street name without the number.
     * Example: "Rue de la Loi", "Main Street", "Avenue des Champs-Élysées"</p>
     */
    @Column(name = "street_name", length = 150)
    private String streetName;

    /**
     * Street/house number.
     *
     * <p>The building number on the street. Stored as String to support
     * formats like "12A", "100-102", "221B".</p>
     */
    @Column(name = "street_number", length = 20)
    private String streetNumber;

    /**
     * Additional address line for complement information.
     *
     * <p>Used for apartment numbers, building names, floor numbers,
     * entry codes, or any additional delivery instructions.</p>
     *
     * <p>Examples: "Apt 4B", "Building C, 3rd floor", "Gate code: 1234"</p>
     */
    @Column(name = "complement", length = 150)
    private String complement;

    // endregion

    // ========================================
    // region Locality Information
    // ========================================

    /**
     * Postal or ZIP code.
     *
     * <p>Format varies by country. Stored as String to support alphanumeric
     * codes (e.g., UK: "SW1A 1AA", Canada: "K1A 0B1").</p>
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * City or locality name.
     *
     * <p>The primary locality (city, town, village) of the address.</p>
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State, province, or region.
     *
     * <p>Administrative division below the country level.
     * Optional as not all countries use this level.</p>
     *
     * <p>Examples: "California", "Ontario", "Wallonia", "Île-de-France"</p>
     */
    @Column(name = "state_province", length = 100)
    private String stateProvince;

    /**
     * ISO 3166-1 alpha-2 country code.
     *
     * <p>Two-letter country code following the ISO standard.
     * Examples: "BE" (Belgium), "FR" (France), "US" (United States)</p>
     *
     * <p>Using ISO codes instead of full names ensures consistency
     * and facilitates internationalization.</p>
     */
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    // endregion

    // ========================================
    // region Geolocation (Optional)
    // ========================================

    /**
     * Latitude coordinate for geolocation.
     *
     * <p>Optional field that can be populated via geocoding services.
     * Useful for mapping, distance calculations, and location-based features.</p>
     *
     * <p>Range: -90.0 to +90.0</p>
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * Longitude coordinate for geolocation.
     *
     * <p>Optional field that can be populated via geocoding services.</p>
     *
     * <p>Range: -180.0 to +180.0</p>
     */
    @Column(name = "longitude")
    private Double longitude;

    // endregion

    // ========================================
    // region Validation & Metadata
    // ========================================

    /**
     * Indicates whether this address has been validated.
     *
     * <p>Validation can be performed via:
     * <ul>
     *   <li>External address verification API</li>
     *   <li>Postal service validation</li>
     *   <li>Manual verification</li>
     * </ul>
     * </p>
     *
     * <p>Unvalidated addresses may still be used but could be flagged
     * for review or excluded from certain operations.</p>
     */
    @Column(name = "validated", nullable = false)
    @Builder.Default
    private boolean validated = false;

    /**
     * Source or method of the last validation.
     *
     * <p>Examples: "GOOGLE_PLACES", "BPOST_API", "MANUAL", "USER_CONFIRMED"</p>
     */
    @Column(name = "validation_source", length = 50)
    private String validationSource;

    /**
     * Formatted address string for display purposes.
     *
     * <p>A pre-formatted, human-readable version of the complete address.
     * Can be populated by geocoding services or generated from components.</p>
     *
     * <p>Example: "Rue de la Loi 16, 1000 Bruxelles, Belgium"</p>
     */
    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    // endregion

    // ========================================
    // region Convenience Methods
    // ========================================

    /**
     * Checks if this address has geolocation coordinates.
     *
     * @return true if both latitude and longitude are set
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Checks if this address is complete (has minimum required fields).
     *
     * <p>Minimum requirements: street name, postal code, city, and country.</p>
     *
     * @return true if the address has all minimum required fields
     */
    public boolean isComplete() {
        return streetName != null && !streetName.isBlank()
                && postalCode != null && !postalCode.isBlank()
                && city != null && !city.isBlank()
                && countryCode != null && !countryCode.isBlank();
    }

    /**
     * Generates a single-line address string.
     *
     * <p>If a formatted address exists, returns it. Otherwise, builds
     * a basic representation from available components.</p>
     *
     * @return a human-readable address string
     */
    public String toSingleLine() {
        if (formattedAddress != null && !formattedAddress.isBlank()) {
            return formattedAddress;
        }

        StringBuilder sb = new StringBuilder();

        if (streetNumber != null && !streetNumber.isBlank()) {
            sb.append(streetNumber).append(" ");
        }
        if (streetName != null && !streetName.isBlank()) {
            sb.append(streetName);
        }
        if (complement != null && !complement.isBlank()) {
            sb.append(", ").append(complement);
        }
        if (postalCode != null && !postalCode.isBlank()) {
            sb.append(", ").append(postalCode);
        }
        if (city != null && !city.isBlank()) {
            sb.append(" ").append(city);
        }
        if (stateProvince != null && !stateProvince.isBlank()) {
            sb.append(", ").append(stateProvince);
        }
        if (countryCode != null && !countryCode.isBlank()) {
            sb.append(", ").append(countryCode);
        }

        return sb.toString().trim();
    }

    // endregion
}