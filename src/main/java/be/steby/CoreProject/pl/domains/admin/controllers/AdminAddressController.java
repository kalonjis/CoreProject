package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.address.AdminAddressService;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.pl.domains.admin.models.responses.AddressUserLinkDTO;
import be.steby.CoreProject.pl.domains.admin.models.responses.AdminAddressDetailDTO;
import be.steby.CoreProject.pl.domains.profile.address.models.responses.AddressDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for administrative address management operations.
 * Provides a system-wide view of addresses and their usage.
 *
 * <p>All endpoints require ADMIN privileges.</p>
 *
 * <h4>Base path:</h4>
 * <pre>/api/admin/addresses</pre>
 *
 * <h4>Security:</h4>
 * <ul>
 *   <li>Controller-level: {@code @PreAuthorize("hasAuthority('ADMIN')")}</li>
 *   <li>Service-level: Additional permission validation in service layer</li>
 * </ul>
 *
 * <h4>Business logic delegation:</h4>
 * <p>All business logic is delegated to {@link AdminAddressService}.</p>
 *
 * @see AdminAddressService
 * @see AddressDTO
 * @see AdminAddressDetailDTO
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/addresses")
@Slf4j
public class AdminAddressController {

    private final AdminAddressService adminAddressService;

    // ========================================
    // region GET - Retrieval
    // ========================================

    /**
     * Retrieves all addresses in the system.
     *
     * <p>Returns a simple list of all addresses with their geographical data.
     * Use the detail endpoint to see which users are linked to specific addresses.</p>
     *
     * <h4>Example request:</h4>
     * <pre>GET /api/admin/addresses</pre>
     *
     * <h4>Response:</h4>
     * <pre>
     * [
     *   {
     *     "publicId": "addr-123",
     *     "streetNumber": "42",
     *     "streetName": "Avenue Louise",
     *     "city": "Bruxelles",
     *     "countryCode": "BE",
     *     "validated": true,
     *     ...
     *   }
     * ]
     * </pre>
     *
     * @return List of all addresses
     */
    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAllAddresses() {
        log.debug("Admin request to retrieve all addresses");

        List<Address> addresses = adminAddressService.getAllAddresses();

        List<AddressDTO> dtos = addresses.stream()
                .map(AddressDTO::fromEntity)
                .toList();

        log.debug("Returning {} address(es)", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Retrieves detailed information about a specific address.
     *
     * <p>Returns the address data along with all users who have it linked.
     * This is essential for understanding address sharing and impact before modifications.</p>
     *
     * <h4>Example request:</h4>
     * <pre>GET /api/admin/addresses/addr-123</pre>
     *
     * <h4>Response:</h4>
     * <pre>
     * {
     *   "address": {
     *     "publicId": "addr-123",
     *     "streetName": "Avenue des Sports",
     *     "city": "Louvain-la-Neuve",
     *     ...
     *   },
     *   "userCount": 2,
     *   "users": [
     *     {
     *       "userPublicId": "user-456",
     *       "username": "steby",
     *       "email": "kalonj1981@hotmail.com",
     *       "linkPublicId": "link-789",
     *       "addressType": "SHIPPING",
     *       "label": "Shared Apartment",
     *       "active": true,
     *       "isPrimary": false,
     *       ...
     *     },
     *     {
     *       "userPublicId": "user-789",
     *       "username": "quent",
     *       "email": "quentin@fake.com",
     *       "linkPublicId": "link-012",
     *       "addressType": "RESIDENTIAL",
     *       "label": "Roommate Apartment",
     *       "active": true,
     *       "isPrimary": true,
     *       ...
     *     }
     *   ]
     * }
     * </pre>
     *
     * @param publicId Address public ID
     * @return Address details with linked users
     */
    @GetMapping("/{publicId}")
    public ResponseEntity<AdminAddressDetailDTO> getAddressDetail(
            @PathVariable String publicId) {

        log.debug("Admin request to retrieve address detail: {}", publicId);

        Address address = adminAddressService.getAddressById(publicId);
        List<UserAddress> userAddresses = adminAddressService.getAddressUsers(publicId);

        AdminAddressDetailDTO dto = AdminAddressDetailDTO.fromEntity(address, userAddresses);

        log.debug("Returning address {} with {} user(s)", publicId, dto.userCount());
        return ResponseEntity.ok(dto);
    }

    /**
     * Retrieves all users linked to a specific address.
     *
     * <p>Returns only the user link information without the address data.
     * Useful when you already have the address details and just need the user list.</p>
     *
     * <h4>Example request:</h4>
     * <pre>GET /api/admin/addresses/addr-123/users</pre>
     *
     * <h4>Response:</h4>
     * <pre>
     * [
     *   {
     *     "userPublicId": "user-456",
     *     "username": "steby",
     *     "email": "kalonj1981@hotmail.com",
     *     "linkPublicId": "link-789",
     *     "addressType": "SHIPPING",
     *     "label": "Shared Apartment",
     *     "active": true,
     *     ...
     *   }
     * ]
     * </pre>
     *
     * @param publicId Address public ID
     * @return List of users linked to the address
     */
    @GetMapping("/{publicId}/users")
    public ResponseEntity<List<AddressUserLinkDTO>> getAddressUsers(
            @PathVariable String publicId) {

        log.debug("Admin request to retrieve users for address: {}", publicId);

        List<UserAddress> userAddresses = adminAddressService.getAddressUsers(publicId);

        List<AddressUserLinkDTO> dtos = userAddresses.stream()
                .map(AddressUserLinkDTO::fromEntity)
                .toList();

        log.debug("Returning {} user(s) for address {}", dtos.size(), publicId);
        return ResponseEntity.ok(dtos);
    }

    // endregion
}