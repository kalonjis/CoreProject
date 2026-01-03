package be.steby.CoreProject.pl.domains.profile.address.controller;

import be.steby.CoreProject.bll.domains.profile.services.address.UserAddressService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.CreateUserAddressRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UpdateUserAddressRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UserAddressSearchCriteria;
import be.steby.CoreProject.pl.domains.profile.address.models.responses.UserAddressDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing user addresses.
 *
 * <p>All endpoints require authentication and operate on the authenticated user's addresses.</p>
 */
@RestController
@RequestMapping("/api/profile/addresses")
@RequiredArgsConstructor
@Slf4j
public class UserAddressController {

    private final UserAddressService userAddressService;

    // ========================================
    // region GET - Search & Retrieve
    // ========================================

    /**
     * Searches addresses with optional filters.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>GET /addresses → all active addresses</li>
     *   <li>GET /addresses?type=BILLING → billing addresses</li>
     *   <li>GET /addresses?type=SHIPPING&active=true → active shipping addresses</li>
     *   <li>GET /addresses?billingEligible=true&countryCode=BE → billing-eligible in Belgium</li>
     * </ul>
     *
     * @param type             filter by address type
     * @param active           filter by active status
     * @param isDefault        filter by default status
     * @param isPrimary        filter by primary status
     * @param billingEligible  filter by billing eligibility
     * @param shippingEligible filter by shipping eligibility
     * @param verified         filter by owner verification
     * @param countryCode      filter by country (ISO code)
     * @param city             filter by city
     * @param postalCode       filter by postal code
     * @param label            filter by label (contains)
     * @param validOnly        only currently valid addresses
     * @param hasCoordinates   only addresses with geolocation
     * @param user             the authenticated user
     *
     *                         GET /api/profile/addresses                           → toutes (actives)
     * GET /api/profile/addresses?type=BILLING              → type BILLING
     * GET /api/profile/addresses?type=SHIPPING&active=true → shipping actives
     * GET /api/profile/addresses?billingEligible=true      → éligibles facturation
     * GET /api/profile/addresses?countryCode=BE            → en Belgique
     * GET /api/profile/addresses?city=Bruxelles&type=RESIDENTIAL
     * GET /api/profile/addresses?isPrimary=true            → adresse principale
     * GET /api/profile/addresses?label=Maison              → label contient "Maison"
     * @return list of matching addresses
     */
    @GetMapping
    public ResponseEntity<List<UserAddressDTO>> searchAddresses(
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
            @RequestParam(required = false) Boolean hasCoordinates,
            @AuthenticationPrincipal User user) {

        UserAddressSearchCriteria criteria = new UserAddressSearchCriteria(
                type, active, isDefault, isPrimary,
                billingEligible, shippingEligible, verified,
                countryCode, city, postalCode, label,
                validOnly, hasCoordinates
        );

        List<UserAddress> addresses = userAddressService.search(user, criteria);
        List<UserAddressDTO> dtos = addresses.stream()
                .map(UserAddressDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Gets a specific address by public ID.
     *
     * @param publicId the address public ID
     * @param user     the authenticated user
     * @return the user address
     */
    @GetMapping("/{publicId}")
    public ResponseEntity<UserAddressDTO> getAddress(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user) {

        UserAddress userAddress = userAddressService.getByPublicIdAndUser(publicId, user);
        return ResponseEntity.ok(UserAddressDTO.fromEntity(userAddress));
    }

    // endregion

    // ========================================
    // region POST - Create
    // ========================================

    /**
     * Creates a new address for the authenticated user.
     *
     * @param request the address creation request
     * @param user    the authenticated user
     * @return the created address
     */
    @PostMapping
    public ResponseEntity<UserAddressDTO> createAddress(
            @Valid @RequestBody CreateUserAddressRequest request,
            @AuthenticationPrincipal User user) {

        UserAddress userAddress = userAddressService.createAndLink(
                user,
                request.toAddressEntity(),
                request.addressType(),
                request.label(),
                request.isDefault(),
                request.isPrimary()
        );

        // Update eligibility if specified
        if (!request.isBillingEligible() || !request.isShippingEligible()) {
            userAddress = userAddressService.updateEligibility(
                    userAddress.getPublicId(),
                    user,
                    request.isBillingEligible(),
                    request.isShippingEligible()
            );
        }

        log.info("User {} created new address: {}", user.getUsername(), userAddress.getPublicId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserAddressDTO.fromEntity(userAddress));
    }

    // endregion

    // ========================================
    // region PUT/PATCH - Update
    // ========================================

    /**
     * Updates an existing address.
     *
     * @param publicId the address public ID
     * @param request  the update request
     * @param user     the authenticated user
     * @return the updated address
     */
    @PutMapping("/{publicId}")
    public ResponseEntity<UserAddressDTO> updateAddress(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateUserAddressRequest request,
            @AuthenticationPrincipal User user) {

        UserAddress userAddress = userAddressService.getByPublicIdAndUser(publicId, user);

        // Update address fields if provided
        if (request.hasAddressChanges()) {
            userAddress = userAddressService.updateAddress(
                    publicId,
                    user,
                    request.toAddressEntity(userAddress.getAddress())
            );
        }

        // Update metadata if provided
        if (request.label() != null || request.notes() != null) {
            userAddress = userAddressService.updateLinkMetadata(
                    publicId,
                    user,
                    request.label(),
                    request.notes()
            );
        }

        // Update eligibility if provided
        if (request.billingEligible() != null || request.shippingEligible() != null) {
            boolean billing = request.billingEligible() != null
                    ? request.billingEligible()
                    : userAddress.isBillingEligible();
            boolean shipping = request.shippingEligible() != null
                    ? request.shippingEligible()
                    : userAddress.isShippingEligible();

            userAddress = userAddressService.updateEligibility(publicId, user, billing, shipping);
        }

        log.info("User {} updated address: {}", user.getUsername(), publicId);
        return ResponseEntity.ok(UserAddressDTO.fromEntity(userAddress));
    }

    /**
     * Sets an address as the default for its type.
     *
     * @param publicId the address public ID
     * @param user     the authenticated user
     * @return the updated address
     */
    @PatchMapping("/{publicId}/default")
    public ResponseEntity<UserAddressDTO> setAsDefault(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user) {

        UserAddress userAddress = userAddressService.setAsDefault(publicId, user);
        log.info("User {} set address {} as default", user.getUsername(), publicId);
        return ResponseEntity.ok(UserAddressDTO.fromEntity(userAddress));
    }

    /**
     * Sets an address as the user's primary address.
     *
     * @param publicId the address public ID
     * @param user     the authenticated user
     * @return the updated address
     */
    @PatchMapping("/{publicId}/primary")
    public ResponseEntity<UserAddressDTO> setAsPrimary(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user) {

        UserAddress userAddress = userAddressService.setAsPrimary(publicId, user);
        log.info("User {} set address {} as primary", user.getUsername(), publicId);
        return ResponseEntity.ok(UserAddressDTO.fromEntity(userAddress));
    }

    /**
     * Changes the type of an address.
     *
     * @param publicId     the address public ID
     * @param newType      the new address type
     * @param setAsDefault whether to set as default for the new type
     * @param user         the authenticated user
     * @return the updated address
     */
    @PatchMapping("/{publicId}/type")
    public ResponseEntity<UserAddressDTO> changeType(
            @PathVariable String publicId,
            @RequestParam AddressType newType,
            @RequestParam(defaultValue = "false") boolean setAsDefault,
            @AuthenticationPrincipal User user) {

        UserAddress userAddress = userAddressService.changeType(publicId, user, newType, setAsDefault);
        log.info("User {} changed address {} type to {}", user.getUsername(), publicId, newType);
        return ResponseEntity.ok(UserAddressDTO.fromEntity(userAddress));
    }

    // endregion

    // ========================================
    // region DELETE
    // ========================================

    /**
     * Unlinks an address (soft delete).
     *
     * @param publicId the address public ID
     * @param user     the authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> unlinkAddress(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user) {

        userAddressService.unlinkAddress(publicId, user);
        log.info("User {} unlinked address: {}", user.getUsername(), publicId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently removes an address link.
     *
     * @param publicId the address public ID
     * @param user     the authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/{publicId}/permanent")
    public ResponseEntity<Void> removeAddress(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user) {

        userAddressService.removeAddressLink(publicId, user);
        log.info("User {} permanently removed address: {}", user.getUsername(), publicId);
        return ResponseEntity.noContent().build();
    }

    // endregion
}