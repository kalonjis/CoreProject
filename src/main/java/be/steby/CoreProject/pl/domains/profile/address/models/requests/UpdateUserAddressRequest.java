package be.steby.CoreProject.pl.domains.profile.address.models.requests;

import be.steby.CoreProject.dl.entities.Address;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing user address.
 *
 * <p>All fields are optional. Only non-null fields will be updated.</p>
 *
 * @param streetNumber     the street/house number
 * @param streetName       the street name
 * @param complement       additional info (apt, floor, etc.)
 * @param postalCode       the postal/ZIP code
 * @param city             the city name
 * @param stateProvince    the state or province
 * @param countryCode      the ISO 3166-1 alpha-2 country code
 * @param label            user-defined label
 * @param notes            optional notes
 * @param billingEligible  whether eligible for billing
 * @param shippingEligible whether eligible for shipping
 */
public record UpdateUserAddressRequest(

        @Size(max = 20, message = "Street number must not exceed 20 characters")
        String streetNumber,

        @Size(max = 150, message = "Street name must not exceed 150 characters")
        String streetName,

        @Size(max = 150, message = "Complement must not exceed 150 characters")
        String complement,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "State/Province must not exceed 100 characters")
        String stateProvince,

        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (ISO 3166-1 alpha-2)")
        String countryCode,

        @Size(max = 50, message = "Label must not exceed 50 characters")
        String label,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes,

        Boolean billingEligible,

        Boolean shippingEligible
) {
    /**
     * Checks if any address field is provided for update.
     *
     * @return true if at least one address field is non-null
     */
    public boolean hasAddressChanges() {
        return streetNumber != null
                || streetName != null
                || complement != null
                || postalCode != null
                || city != null
                || stateProvince != null
                || countryCode != null;
    }

    /**
     * Checks if any metadata field is provided for update.
     *
     * @return true if at least one metadata field is non-null
     */
    public boolean hasMetadataChanges() {
        return label != null
                || notes != null
                || billingEligible != null
                || shippingEligible != null;
    }

    /**
     * Converts this request to an Address entity for update.
     *
     * @param existing the existing address to merge with
     * @return the merged Address entity
     */
    public Address toAddressEntity(Address existing) {
        return Address.builder()
                .streetNumber(streetNumber != null ? streetNumber : existing.getStreetNumber())
                .streetName(streetName != null ? streetName : existing.getStreetName())
                .complement(complement != null ? complement : existing.getComplement())
                .postalCode(postalCode != null ? postalCode : existing.getPostalCode())
                .city(city != null ? city : existing.getCity())
                .stateProvince(stateProvince != null ? stateProvince : existing.getStateProvince())
                .countryCode(countryCode != null ? countryCode.toUpperCase() : existing.getCountryCode())
                .build();
    }
}