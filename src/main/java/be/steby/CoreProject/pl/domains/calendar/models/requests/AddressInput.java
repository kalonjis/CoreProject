package be.steby.CoreProject.pl.domains.calendar.models.requests;

// NOUVEAU FICHIER À CRÉER : AddressInput.java
// Location: src/main/java/be/steby/CoreProject/pl/domains/calendar/models/requests/

import be.steby.CoreProject.dl.entities.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for address data in event creation/update requests.
 *
 * <p>This is a lightweight address input model specifically designed for
 * embedding in other request DTOs (like {@link CreateEventRequest}).
 * It contains the essential fields needed to identify or create an address.</p>
 *
 * <h4>Required Fields:</h4>
 * <p>For address matching/creation to work, the following fields are required:</p>
 * <ul>
 *   <li>{@code streetName} - Street name</li>
 *   <li>{@code postalCode} - Postal/ZIP code</li>
 *   <li>{@code city} - City name</li>
 *   <li>{@code countryCode} - ISO country code</li>
 * </ul>
 *
 * <h4>Optional Fields:</h4>
 * <ul>
 *   <li>{@code streetNumber} - Building number</li>
 *   <li>{@code complement} - Apartment, floor, etc.</li>
 * </ul>
 *
 * <h4>Deduplication:</h4>
 * <p>When converted to an entity and saved via {@code AddressService.findOrCreate()},
 * the system checks for existing addresses with matching street number, street name,
 * postal code, city, and country code. If found, the existing address is reused.</p>
 *
 * <h4>JSON Example:</h4>
 * <pre>{@code
 * {
 *   "streetNumber": "16",
 *   "streetName": "Rue de la Loi",
 *   "complement": "3rd floor",
 *   "postalCode": "1000",
 *   "city": "Bruxelles",
 *   "countryCode": "BE"
 * }
 * }</pre>
 *
 * @param streetNumber building number (optional, max 20 chars)
 * @param streetName   street name (required, max 150 chars)
 * @param complement   additional info like apartment (optional, max 150 chars)
 * @param postalCode   postal/ZIP code (required, max 20 chars)
 * @param city         city name (required, max 100 chars)
 * @param countryCode  ISO 3166-1 alpha-2 country code (required, 2 chars)
 *
 * @see CreateEventRequest
 * @see Address
 */
public record AddressInput(

        @Size(max = 20, message = "Street number must not exceed 20 characters")
        String streetNumber,

        @NotBlank(message = "Street name is required")
        @Size(max = 150, message = "Street name must not exceed 150 characters")
        String streetName,

        @Size(max = 150, message = "Complement must not exceed 150 characters")
        String complement,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (ISO 3166-1 alpha-2)")
        String countryCode
) {

    /**
     * Converts this input DTO to an Address entity.
     *
     * <p>Creates a new Address entity with the provided fields.
     * The entity is not persisted - that is handled by the service layer
     * which may find an existing duplicate instead.</p>
     *
     * <p>Country code is normalized to uppercase.</p>
     *
     * @return new Address entity (not persisted)
     */
    public Address toEntity() {
        return Address.builder()
                .streetNumber(streetNumber)
                .streetName(streetName)
                .complement(complement)
                .postalCode(postalCode)
                .city(city)
                .countryCode(countryCode != null ? countryCode.toUpperCase() : null)
                .validated(false)
                .build();
    }
}