package be.steby.CoreProject.pl.domains.profile.address.models.requests;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new user address.
 *
 * @param streetNumber     the street/house number
 * @param streetName       the street name (required)
 * @param complement       additional info (apt, floor, etc.)
 * @param postalCode       the postal/ZIP code (required)
 * @param city             the city name (required)
 * @param stateProvince    the state or province
 * @param countryCode      the ISO 3166-1 alpha-2 country code (required)
 * @param addressType      the type of address (required)
 * @param label            optional user-defined label
 * @param notes            optional notes
 * @param isDefault        whether this should be the default for its type
 * @param isPrimary        whether this should be the primary address
 * @param billingEligible  whether eligible for billing (default true)
 * @param shippingEligible whether eligible for shipping (default true)
 */
public record CreateUserAddressRequest(

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

        @Size(max = 100, message = "State/Province must not exceed 100 characters")
        String stateProvince,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (ISO 3166-1 alpha-2)")
        String countryCode,

        @NotNull(message = "Address type is required")
        AddressType addressType,

        @Size(max = 50, message = "Label must not exceed 50 characters")
        String label,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes,

        boolean isDefault,

        boolean isPrimary,

        Boolean billingEligible,

        Boolean shippingEligible
) {
    /**
     * Converts this request to an Address entity.
     *
     * @return the Address entity
     */
    public Address toAddressEntity() {
        return Address.builder()
                .streetNumber(streetNumber)
                .streetName(streetName)
                .complement(complement)
                .postalCode(postalCode)
                .city(city)
                .stateProvince(stateProvince)
                .countryCode(countryCode != null ? countryCode.toUpperCase() : null)
                .build();
    }

    /**
     * Returns billing eligibility with default value.
     */
    public boolean isBillingEligible() {
        return billingEligible == null || billingEligible;
    }

    /**
     * Returns shipping eligibility with default value.
     */
    public boolean isShippingEligible() {
        return shippingEligible == null || shippingEligible;
    }
}