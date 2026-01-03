package be.steby.CoreProject.pl.domains.profile.address.models.responses;

import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;

import java.time.Instant;

/**
 * DTO representing a user's address link with metadata.
 *
 * @param publicId         the public UUID of the link
 * @param address          the address data
 * @param addressType      the type of address (BILLING, SHIPPING, etc.)
 * @param label            the user-defined label
 * @param notes            optional notes
 * @param isDefault        whether this is the default for its type
 * @param isPrimary        whether this is the user's primary address
 * @param billingEligible  whether eligible for billing
 * @param shippingEligible whether eligible for shipping
 * @param active           whether the link is active
 * @param verifiedByOwner  whether verified by the owner
 * @param validFrom        start of validity period
 * @param validTo          end of validity period
 * @param createdAt        creation timestamp
 */
public record UserAddressDTO(
        String publicId,
        AddressDTO address,
        AddressType addressType,
        String label,
        String notes,
        boolean isDefault,
        boolean isPrimary,
        boolean billingEligible,
        boolean shippingEligible,
        boolean active,
        boolean verifiedByOwner,
        Instant validFrom,
        Instant validTo,
        Instant createdAt
) {
    /**
     * Creates a DTO from a UserAddress entity.
     *
     * @param userAddress the user address entity
     * @return the DTO
     */
    public static UserAddressDTO fromEntity(UserAddress userAddress) {
        return new UserAddressDTO(
                userAddress.getPublicId(),
                AddressDTO.fromEntity(userAddress.getAddress()),
                userAddress.getAddressType(),
                userAddress.getLabel(),
                userAddress.getNotes(),
                userAddress.isDefault(),
                userAddress.isPrimary(),
                userAddress.isBillingEligible(),
                userAddress.isShippingEligible(),
                userAddress.isActive(),
                userAddress.isVerifiedByOwner(),
                userAddress.getValidFrom(),
                userAddress.getValidTo(),
                userAddress.getCreatedAt()
        );
    }
}