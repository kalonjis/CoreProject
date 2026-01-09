package be.steby.CoreProject.pl.domains.admin.models.responses;

import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;

/**
 * DTO representing a user linked to an address with minimal information.
 *
 * <p>Used in admin contexts to show which users are associated with a specific address
 * without exposing full user details. Contains essential user identification
 * and the metadata of the address link.</p>
 *
 * @param userPublicId     User's public ID
 * @param username         Username for display
 * @param email            User's email address
 * @param linkPublicId     Public ID of the UserAddress link
 * @param addressType      Type of address (RESIDENTIAL, WORK, etc.)
 * @param label            User-defined label for the address
 * @param active           Whether the link is active
 * @param isPrimary        Whether this is the user's primary address
 * @param isDefault        Whether this is the default for its type
 * @param verifiedByOwner  Whether the user has verified this address
 */
public record AddressUserLinkDTO(
        String userPublicId,
        String username,
        String email,
        String linkPublicId,
        AddressType addressType,
        String label,
        boolean active,
        boolean isPrimary,
        boolean isDefault,
        boolean verifiedByOwner
) {
    /**
     * Creates a DTO from a UserAddress entity.
     *
     * <p>Extracts user information and link metadata while keeping
     * the response lightweight for admin list views.</p>
     *
     * @param userAddress the user address link entity
     * @return the DTO
     */
    public static AddressUserLinkDTO fromEntity(UserAddress userAddress) {
        return new AddressUserLinkDTO(
                userAddress.getUser().getPublicId(),
                userAddress.getUser().getUsername(),
                userAddress.getUser().getEmail(),
                userAddress.getPublicId(),
                userAddress.getAddressType(),
                userAddress.getLabel(),
                userAddress.isActive(),
                userAddress.isPrimary(),
                userAddress.isDefault(),
                userAddress.isVerifiedByOwner()
        );
    }
}