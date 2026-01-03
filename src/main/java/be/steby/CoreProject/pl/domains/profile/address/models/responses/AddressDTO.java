package be.steby.CoreProject.pl.domains.profile.address.models.responses;


import be.steby.CoreProject.dl.entities.Address;

/**
 * DTO representing address geographical data.
 *
 * @param publicId         the public UUID
 * @param streetNumber     the street/house number
 * @param streetName       the street name
 * @param complement       additional info (apt, floor, etc.)
 * @param postalCode       the postal/ZIP code
 * @param city             the city name
 * @param stateProvince    the state or province
 * @param countryCode      the ISO 3166-1 alpha-2 country code
 * @param formattedAddress the formatted address string
 * @param latitude         the latitude coordinate
 * @param longitude        the longitude coordinate
 * @param validated        whether the address has been validated
 */
public record AddressDTO(
        String publicId,
        String streetNumber,
        String streetName,
        String complement,
        String postalCode,
        String city,
        String stateProvince,
        String countryCode,
        String formattedAddress,
        Double latitude,
        Double longitude,
        boolean validated
) {
    /**
     * Creates a DTO from an Address entity.
     *
     * @param address the address entity
     * @return the DTO
     */
    public static AddressDTO fromEntity(Address address) {
        return new AddressDTO(
                address.getPublicId(),
                address.getStreetNumber(),
                address.getStreetName(),
                address.getComplement(),
                address.getPostalCode(),
                address.getCity(),
                address.getStateProvince(),
                address.getCountryCode(),
                address.getFormattedAddress(),
                address.getLatitude(),
                address.getLongitude(),
                address.isValidated()
        );
    }
}