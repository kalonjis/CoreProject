package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.address.AdminUserAddressService;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.pl.domains.admin.models.requests.UpdateUserAddressMetadataRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.CreateUserAddressRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.responses.UserAddressDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for administrative user address management operations.
 * Handles address viewing and management for any user in the system.
 *
 * <p>All endpoints require ADMIN privileges.</p>
 *
 * <h4>Base path:</h4>
 * <pre>/api/admin/users/{userPublicId}/addresses</pre>
 *
 * <h4>Security:</h4>
 * <ul>
 *   <li>Controller-level: {@code @PreAuthorize("hasAuthority('ADMIN')")}</li>
 *   <li>Service-level: Additional permission validation in service layer</li>
 * </ul>
 *
 * <h4>Business logic delegation:</h4>
 * <p>All business logic is delegated to {@link AdminUserAddressService}.</p>
 *
 * @see AdminUserAddressService
 * @see UserAddressDTO
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/users/{userPublicId}/addresses")
@Slf4j
public class AdminUserAddressController {

    private final AdminUserAddressService adminUserAddressService;

    // ========================================
    // region GET - Retrieval
    // ========================================

    /**
     * Retrieves all addresses for a specific user.
     *
     * <p>Returns all user addresses including inactive ones.
     * Admins can see the complete address history.</p>
     *
     * <h4>Example request:</h4>
     * <pre>GET /api/admin/users/abc-123/addresses</pre>
     *
     * <h4>Response:</h4>
     * <pre>
     * [
     *   {
     *     "publicId": "addr-123",
     *     "address": {...},
     *     "addressType": "RESIDENTIAL",
     *     "label": "Home",
     *     "isDefault": true,
     *     "active": true,
     *     ...
     *   }
     * ]
     * </pre>
     *
     * @param userPublicId User's public ID
     * @return List of user addresses
     */
    @GetMapping
    public ResponseEntity<List<UserAddressDTO>> getUserAddresses(
            @PathVariable String userPublicId) {

        log.debug("Admin request to retrieve addresses for user: {}", userPublicId);

        List<UserAddress> addresses = adminUserAddressService.getUserAddresses(userPublicId);

        List<UserAddressDTO> dtos = addresses.stream()
                .map(UserAddressDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // endregion


    // ========================================
// region POST - Create
// ========================================

    /**
     * Creates a new address for a specific user.
     *
     * <p>Allows admins to add addresses to any user in the system.
     * The address data is validated and duplicate detection is performed
     * to avoid creating redundant address entries.</p>
     *
     * <h4>Example request:</h4>
     * <pre>
     * POST /api/admin/users/user-123/addresses
     * {
     *   "streetNumber": "42",
     *   "streetName": "Rue de la Paix",
     *   "postalCode": "1000",
     *   "city": "Bruxelles",
     *   "countryCode": "BE",
     *   "addressType": "RESIDENTIAL",
     *   "label": "New Home",
     *   "isPrimary": true,
     *   "billingEligible": true,
     *   "shippingEligible": true
     * }
     * </pre>
     *
     * <h4>Business rules:</h4>
     * <ul>
     *   <li>Duplicate detection: reuses existing Address if found</li>
     *   <li>Setting isPrimary=true removes isPrimary from other addresses</li>
     *   <li>Setting isDefault=true removes isDefault from other addresses of same type</li>
     * </ul>
     *
     * @param userPublicId User's public ID
     * @param request      Address creation request
     * @return The created user address
     */
    @PostMapping
    public ResponseEntity<UserAddressDTO> createAddress(
            @PathVariable String userPublicId,
            @Valid @RequestBody CreateUserAddressRequest request) {

        log.debug("Admin request to create address for user: {}", userPublicId);

        UserAddress userAddress = adminUserAddressService.createAddress(userPublicId, request);

        log.info("Admin created address {} for user {}",
                userAddress.getPublicId(), userPublicId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserAddressDTO.fromEntity(userAddress));
    }

// endregion

    // ========================================
    // region PATCH - Update Metadata
    // ========================================

    /**
     * Updates the metadata of a user address link.
     *
     * <p>Allows admins to modify link-specific metadata without changing the
     * geographical address data. All fields in the request are optional;
     * only provided fields will be updated.</p>
     *
     * <h4>Updateable fields:</h4>
     * <ul>
     *   <li>label - User-friendly name</li>
     *   <li>notes - Additional information</li>
     *   <li>addressType - RESIDENTIAL, BILLING, SHIPPING, WORK</li>
     *   <li>isPrimary - User's primary address</li>
     *   <li>isDefault - Default for its type</li>
     *   <li>billingEligible - Can be used for billing</li>
     *   <li>shippingEligible - Can be used for shipping</li>
     *   <li>verifiedByOwner - Verification status</li>
     * </ul>
     *
     * <h4>Example request:</h4>
     * <pre>
     * PATCH /api/admin/users/user-123/addresses/addr-456/metadata
     * {
     *   "label": "New Home Address",
     *   "isPrimary": true,
     *   "billingEligible": true,
     *   "shippingEligible": true
     * }
     * </pre>
     *
     * <h4>Business rules:</h4>
     * <ul>
     *   <li>Setting isPrimary=true removes isPrimary from other addresses</li>
     *   <li>Setting isDefault=true removes isDefault from other addresses of same type</li>
     *   <li>Empty strings for label/notes will clear the field</li>
     * </ul>
     *
     * @param userPublicId User's public ID
     * @param linkPublicId UserAddress link public ID
     * @param request      Update request with optional fields
     * @return The updated user address
     */
    @PatchMapping("/{linkPublicId}/metadata")
    public ResponseEntity<UserAddressDTO> updateAddressMetadata(
            @PathVariable String userPublicId,
            @PathVariable String linkPublicId,
            @Valid @RequestBody UpdateUserAddressMetadataRequest request) {

        log.debug("Admin request to update address metadata - user: {}, link: {}",
                userPublicId, linkPublicId);

        UserAddress userAddress = adminUserAddressService.updateAddressMetadata(
                userPublicId,
                linkPublicId,
                request
        );

        log.info("Admin updated address metadata - user: {}, link: {}",
                userPublicId, linkPublicId);

        return ResponseEntity.ok(UserAddressDTO.fromEntity(userAddress));
    }

    // endregion
}