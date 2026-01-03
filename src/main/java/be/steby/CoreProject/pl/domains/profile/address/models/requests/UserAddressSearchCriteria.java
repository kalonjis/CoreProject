package be.steby.CoreProject.pl.domains.profile.address.models.requests;

import be.steby.CoreProject.dl.enums.AddressType;

/**
 * Search criteria for filtering user addresses.
 *
 * <p>All fields are optional. Only non-null fields are used as filters.</p>
 *
 * @param type             filter by address type
 * @param active           filter by active status
 * @param isDefault        filter by default status
 * @param isPrimary        filter by primary status
 * @param billingEligible  filter by billing eligibility
 * @param shippingEligible filter by shipping eligibility
 * @param verified         filter by owner verification status
 * @param countryCode      filter by country (ISO code)
 * @param city             filter by city name
 * @param postalCode       filter by postal code
 * @param label            filter by label (contains, case-insensitive)
 * @param validOnly        if true, only currently valid addresses
 * @param hasCoordinates   if true, only addresses with geolocation
 */
public record UserAddressSearchCriteria(
        AddressType type,
        Boolean active,
        Boolean isDefault,
        Boolean isPrimary,
        Boolean billingEligible,
        Boolean shippingEligible,
        Boolean verified,
        String countryCode,
        String city,
        String postalCode,
        String label,
        Boolean validOnly,
        Boolean hasCoordinates
) {
    /**
     * Checks if any filter is set.
     *
     * @return true if at least one filter is non-null
     */
    public boolean hasFilters() {
        return type != null
                || active != null
                || isDefault != null
                || isPrimary != null
                || billingEligible != null
                || shippingEligible != null
                || verified != null
                || countryCode != null
                || city != null
                || postalCode != null
                || label != null
                || validOnly != null
                || hasCoordinates != null;
    }
}