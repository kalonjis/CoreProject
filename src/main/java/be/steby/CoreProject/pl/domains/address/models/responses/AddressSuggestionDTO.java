package be.steby.CoreProject.pl.domains.address.models.responses;

import be.steby.CoreProject.dl.entities.Address;

/**
 * Lightweight DTO for address autocomplete/suggestion responses.
 *
 * <p>This DTO is specifically designed for autocomplete functionality,
 * providing all necessary fields to populate address form fields when
 * a user selects a suggestion.</p>
 *
 * <h4>Use Case:</h4>
 * <p>When a user types in an address field (street, city, postal code),
 * the system returns a list of matching addresses. Upon selection,
 * all form fields are populated with the complete address data.</p>
 *
 * <h4>Display Text:</h4>
 * <p>The {@code displayText} field provides a pre-formatted string suitable
 * for display in dropdown suggestions. It uses the address's
 * {@code formattedAddress} if available, otherwise constructs one from
 * individual fields.</p>
 *
 * <h4>JSON Example:</h4>
 * <pre>{@code
 * {
 *   "publicId": "550e8400-e29b-41d4-a716-446655440000",
 *   "displayText": "Rue de la Loi 16, 1000 Bruxelles, BE",
 *   "streetNumber": "16",
 *   "streetName": "Rue de la Loi",
 *   "complement": null,
 *   "postalCode": "1000",
 *   "city": "Bruxelles",
 *   "countryCode": "BE"
 * }
 * }</pre>
 *
 * @param publicId     the unique public identifier for the address
 * @param displayText  pre-formatted address string for display in suggestions
 * @param streetNumber the street/house number
 * @param streetName   the street name
 * @param complement   additional address info (apartment, floor, etc.)
 * @param postalCode   the postal/ZIP code
 * @param city         the city name
 * @param countryCode  the ISO 3166-1 alpha-2 country code
 *
 * @see Address
 */
public record AddressSuggestionDTO(
        String publicId,
        String displayText,
        String streetNumber,
        String streetName,
        String complement,
        String postalCode,
        String city,
        String countryCode
) {

    /**
     * Creates a suggestion DTO from an Address entity.
     *
     * <p>Uses the entity's {@code formattedAddress} if available,
     * otherwise builds a display text from individual fields.</p>
     *
     * @param address the address entity to convert
     * @return a new AddressSuggestionDTO instance
     */
    public static AddressSuggestionDTO fromEntity(Address address) {
        return new AddressSuggestionDTO(
                address.getPublicId(),
                buildDisplayText(address),
                address.getStreetNumber(),
                address.getStreetName(),
                address.getComplement(),
                address.getPostalCode(),
                address.getCity(),
                address.getCountryCode()
        );
    }

    /**
     * Builds a human-readable display text for the address.
     *
     * <p>If the address has a pre-formatted address string (typically from
     * geocoding), it is used directly. Otherwise, constructs a display
     * string from individual fields.</p>
     *
     * <h4>Format:</h4>
     * <ul>
     *   <li>With street number: "Street Name Number, PostalCode City, Country"</li>
     *   <li>Without street number: "Street Name, PostalCode City, Country"</li>
     * </ul>
     *
     * <h4>Examples:</h4>
     * <ul>
     *   <li>"Rue de la Loi 16, 1000 Bruxelles, BE"</li>
     *   <li>"Main Street 123, 10001 New York, US"</li>
     *   <li>"Avenue des Champs-Élysées, 75008 Paris, FR"</li>
     * </ul>
     *
     * @param address the address entity
     * @return formatted display text
     */
    private static String buildDisplayText(Address address) {
        // Use pre-formatted address if available
        if (address.getFormattedAddress() != null && !address.getFormattedAddress().isBlank()) {
            return address.getFormattedAddress();
        }

        // Build display text from components
        StringBuilder sb = new StringBuilder();

        // Street name and number
        if (address.getStreetName() != null && !address.getStreetName().isBlank()) {
            sb.append(address.getStreetName());
            if (address.getStreetNumber() != null && !address.getStreetNumber().isBlank()) {
                sb.append(" ").append(address.getStreetNumber());
            }
        }

        // Postal code and city
        if (address.getPostalCode() != null || address.getCity() != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
                sb.append(address.getPostalCode());
                if (address.getCity() != null && !address.getCity().isBlank()) {
                    sb.append(" ");
                }
            }
            if (address.getCity() != null && !address.getCity().isBlank()) {
                sb.append(address.getCity());
            }
        }

        // Country code
        if (address.getCountryCode() != null && !address.getCountryCode().isBlank()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(address.getCountryCode().toUpperCase());
        }

        return sb.toString();
    }
}