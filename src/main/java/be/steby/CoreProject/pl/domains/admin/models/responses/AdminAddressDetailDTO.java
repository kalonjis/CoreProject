package be.steby.CoreProject.pl.domains.admin.models.responses;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.pl.domains.profile.address.models.responses.AddressDTO;

import java.util.List;

/**
 * DTO representing detailed address information with linked users.
 *
 * <p>Used in admin contexts to provide a comprehensive view of an address
 * including all users who have it linked. This is essential for understanding
 * address sharing and impact before modifications.</p>
 *
 * <h4>Use cases:</h4>
 * <ul>
 *   <li>Admin viewing address details before modification</li>
 *   <li>Identifying shared addresses (multiple users)</li>
 *   <li>Detecting orphaned addresses (zero users)</li>
 *   <li>Impact analysis for address updates or deletion</li>
 * </ul>
 *
 * @param address   The geographical address data
 * @param userCount Number of users linked to this address
 * @param users     List of users linked to this address with their link metadata
 */
public record AdminAddressDetailDTO(
        AddressDTO address,
        int userCount,
        List<AddressUserLinkDTO> users
) {
    /**
     * Creates a DTO from an Address entity and its linked users.
     *
     * @param address      the address entity
     * @param userAddresses list of UserAddress links for this address
     * @return the DTO with address details and user links
     */
    public static AdminAddressDetailDTO fromEntity(Address address, List<UserAddress> userAddresses) {
        List<AddressUserLinkDTO> userLinks = userAddresses.stream()
                .map(AddressUserLinkDTO::fromEntity)
                .toList();

        return new AdminAddressDetailDTO(
                AddressDTO.fromEntity(address),
                userAddresses.size(),
                userLinks
        );
    }
}