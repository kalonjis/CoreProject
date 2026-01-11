package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.address.AdminUserAddressService;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;
import be.steby.CoreProject.pl.domains.admin.models.requests.UpdateUserAddressMetadataRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.CreateUserAddressRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UserAddressSearchCriteria;
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
     * Retrieves addresses for a specific user with optional filtering.
     *
     * <p>Returns all user addresses (including inactive ones) by default.
     * Supports comprehensive filtering through query parameters.</p>
     *
     * <h4>Example requests:</h4>
     * <pre>
     * # Get all addresses (no filters)
     * GET /api/admin/users/abc-123/addresses
     *
     * # Get only inactive addresses
     * GET /api/admin/users/abc-123/addresses?active=false
     *
     * # Get billing addresses in Belgium
     * GET /api/admin/users/abc-123/addresses?type=BILLING&countryCode=BE
     *
     * # Get unverified addresses
     * GET /api/admin/users/abc-123/addresses?verified=false
     *
     * # Combine multiple filters
     * GET /api/admin/users/abc-123/addresses?active=true&billingEligible=true&isPrimary=true
     * </pre>
     *
     * <h4>Available filters:</h4>
     * <ul>
     *   <li>type - Address type (RESIDENTIAL, BILLING, SHIPPING, etc.)</li>
     *   <li>active - Active status (true/false)</li>
     *   <li>isDefault - Default status (true/false)</li>
     *   <li>isPrimary - Primary address flag (true/false)</li>
     *   <li>billingEligible - Billing eligibility (true/false)</li>
     *   <li>shippingEligible - Shipping eligibility (true/false)</li>
     *   <li>verified - Owner verification status (true/false)</li>
     *   <li>countryCode - Filter by country (ISO code, e.g., "BE")</li>
     *   <li>city - Filter by city name</li>
     *   <li>postalCode - Filter by postal code</li>
     *   <li>label - Filter by label (contains, case-insensitive)</li>
     *   <li>validOnly - Only currently valid addresses (true/false)</li>
     *   <li>hasCoordinates - Only addresses with geolocation (true/false)</li>
     * </ul>
     *
     * @param userPublicId      User's public ID
     * @param type              filter by address type
     * @param active            filter by active status (true/false/null)
     * @param isDefault         filter by default status (true/false/null)
     * @param isPrimary         filter by primary status (true/false/null)
     * @param billingEligible   filter by billing eligibility (true/false/null)
     * @param shippingEligible  filter by shipping eligibility (true/false/null)
     * @param verified          filter by verification status (true/false/null)
     * @param countryCode       filter by country (ISO code)
     * @param city              filter by city
     * @param postalCode        filter by postal code
     * @param label             filter by label (contains)
     * @param validOnly         only currently valid addresses
     * @param hasCoordinates    only addresses with geolocation
     * @return List of user addresses matching criteria
     */
    @GetMapping
    public ResponseEntity<List<UserAddressDTO>> getUserAddresses(
            @PathVariable String userPublicId,
            @RequestParam(required = false) AddressType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean isDefault,
            @RequestParam(required = false) Boolean isPrimary,
            @RequestParam(required = false) Boolean billingEligible,
            @RequestParam(required = false) Boolean shippingEligible,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) Boolean validOnly,
            @RequestParam(required = false) Boolean hasCoordinates) {

        log.debug("Admin request to retrieve addresses for user: {} with filters", userPublicId);

        // Build search criteria from query parameters
        UserAddressSearchCriteria criteria = new UserAddressSearchCriteria(
                type, active, isDefault, isPrimary,
                billingEligible, shippingEligible, verified,
                countryCode, city, postalCode, label,
                validOnly, hasCoordinates
        );

        List<UserAddress> addresses = adminUserAddressService.getUserAddresses(userPublicId, criteria);

        List<UserAddressDTO> dtos = addresses.stream()
                .map(UserAddressDTO::fromEntity)
                .toList();

        log.debug("Returning {} address(es) for user {} (filtered: {})",
                dtos.size(), userPublicId, criteria.hasFilters());

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


    // ========================================
    // region DELETE - Remove
    // ========================================

    /**
     * Soft deletes a user address link (deactivation).
     *
     * <p>Sets the address link as inactive (active=false). The link remains in
     * the database for historical purposes but the user won't see it anymore.
     * The Address entity itself is not affected.</p>
     *
     * <h4>Example request:</h4>
     * <pre>DELETE /api/admin/users/user-123/addresses/addr-456</pre>
     *
     * <h4>Use cases:</h4>
     * <ul>
     *   <li>User moved, keep address history</li>
     *   <li>Temporary deactivation</li>
     *   <li>Address no longer relevant but keep for audit</li>
     * </ul>
     *
     * @param userPublicId User's public ID
     * @param linkPublicId UserAddress link public ID
     * @return The deactivated address link
     */
    @DeleteMapping("/{linkPublicId}")
    public ResponseEntity<UserAddressDTO> softDeleteAddress(
            @PathVariable String userPublicId,
            @PathVariable String linkPublicId) {

        log.debug("Admin request to soft delete address - user: {}, link: {}",
                userPublicId, linkPublicId);

        UserAddress userAddress = adminUserAddressService.softDeleteAddress(userPublicId, linkPublicId);

        log.info("Admin soft deleted address {} for user {}", linkPublicId, userPublicId);

        return ResponseEntity.ok(UserAddressDTO.fromEntity(userAddress));
    }

    /**
     * Permanently deletes a user address link.
     *
     * <p>Completely removes the UserAddress link from the database. This operation
     * is irreversible. If this was the last link to the Address, the Address
     * becomes orphaned and can be cleaned up using the address cleanup endpoint.</p>
     *
     * <h4>Example request:</h4>
     * <pre>DELETE /api/admin/users/user-123/addresses/addr-456/permanent</pre>
     *
     * <h4>Use cases:</h4>
     * <ul>
     *   <li>Correct data entry error</li>
     *   <li>Remove duplicate address</li>
     *   <li>Permanent cleanup after verification</li>
     * </ul>
     *
     * @param userPublicId User's public ID
     * @param linkPublicId UserAddress link public ID
     * @return 204 No Content
     */
    @DeleteMapping("/{linkPublicId}/permanent")
    public ResponseEntity<Void> hardDeleteAddress(
            @PathVariable String userPublicId,
            @PathVariable String linkPublicId) {

        log.debug("Admin request to hard delete address - user: {}, link: {}",
                userPublicId, linkPublicId);

        adminUserAddressService.hardDeleteAddress(userPublicId, linkPublicId);

        log.info("Admin permanently deleted address {} for user {}", linkPublicId, userPublicId);

        return ResponseEntity.noContent().build();
    }

    // endregion
}